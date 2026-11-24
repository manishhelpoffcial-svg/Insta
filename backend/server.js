import http from 'node:http';
import { ApifyClient } from 'apify-client';

const PORT = Number(process.env.PORT || 3000);
const ACTOR_ID = 'apify/instagram-profile-scraper';

function sendJson(res, statusCode, payload) {
    res.writeHead(statusCode, {
        'Content-Type': 'application/json; charset=utf-8',
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
        'Access-Control-Allow-Headers': 'Content-Type',
    });
    res.end(JSON.stringify(payload));
}

function normalizeUsername(raw) {
    const trimmed = String(raw || '').trim();
    if (!trimmed) return '';
    const match = trimmed.match(/instagram\.com\/([^/?#]+)/i);
    const extracted = match ? match[1] : trimmed;
    return extracted.replace(/^@+/, '').trim();
}

function getApifyTokens() {
    const list = [];
    const primary = (process.env.APIFY_API_TOKEN || process.env.APIFY_TOKEN || '').trim();
    if (primary && primary !== 'MY_APIFY_API_TOKEN') list.push(primary);

    for (let i = 1; i <= 7; i++) {
        const backup = (process.env[`APIFY_BACKUP_TOKEN_${i}`] || '').trim();
        if (backup && !backup.startsWith('MY_') && !list.includes(backup)) {
            list.push(backup);
        }
    }
    return list;
}

const server = http.createServer(async (req, res) => {
    if (req.method === 'OPTIONS') {
        sendJson(res, 204, {});
        return;
    }

    if (req.method === 'GET' && req.url === '/health') {
        sendJson(res, 200, { ok: true, actor: ACTOR_ID });
        return;
    }

    if (req.method === 'POST' && req.url === '/api/fetch-account') {
        const tokens = getApifyTokens();
        if (tokens.length === 0) {
            sendJson(res, 500, {
                error: 'No valid Apify API tokens (primary or backup 1..7) configured in .env',
            });
            return;
        }

        let body = '';
        req.on('data', (chunk) => {
            body += chunk;
            if (body.length > 1024 * 64) req.destroy();
        });

        req.on('end', async () => {
            const parsed = body ? JSON.parse(body) : {};
            const cleanUsername = normalizeUsername(parsed.username);
            if (!cleanUsername) {
                sendJson(res, 400, { error: 'Please enter a valid Instagram username.' });
                return;
            }

            let lastError = null;
            for (let i = 0; i < tokens.length; i++) {
                const token = tokens[i];
                const tokenLabel = i === 0 ? 'Primary Token' : `Backup Token #${i}`;
                try {
                    const client = new ApifyClient({ token });
                    const input = {
                        usernames: [cleanUsername],
                    };

                    const run = await client.actor(ACTOR_ID).call(input);
                    const { items } = await client.dataset(run.defaultDatasetId).listItems();

                    sendJson(res, 200, {
                        username: cleanUsername,
                        runId: run.id,
                        datasetUrl: `https://console.apify.com/storage/datasets/${run.defaultDatasetId}`,
                        items: Array.isArray(items) ? items : [],
                    });
                    return;
                } catch (err) {
                    lastError = err;
                    const msg = (err?.message || '').toLowerCase();
                    const isLimitOrAuth = msg.includes('401') || msg.includes('402') || msg.includes('403') ||
                        msg.includes('429') || msg.includes('limit') || msg.includes('usage') || msg.includes('credit');

                    if (isLimitOrAuth && i < tokens.length - 1) {
                        console.warn(`[Apify] ${tokenLabel} failed (${err.message}). Trying next backup token...`);
                        continue;
                    }
                    break;
                }
            }

            sendJson(res, 502, {
                error: lastError?.message || 'Unable to load Instagram profile right now (All tokens exhausted).',
            });
        });
        return;
    }

    sendJson(res, 404, { error: 'Not found' });
});

server.listen(PORT, () => {
    console.log(`Instagram Profile Scraper backend listening on port ${PORT}`);
});
