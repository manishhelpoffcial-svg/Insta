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
        const apiToken = (process.env.APIFY_API_TOKEN || process.env.APIFY_TOKEN || '').trim();
        if (!apiToken || apiToken === 'MY_APIFY_API_TOKEN') {
            sendJson(res, 500, {
                error: 'APIFY_API_TOKEN is not configured on the backend server.',
            });
            return;
        }

        let body = '';
        req.on('data', (chunk) => {
            body += chunk;
            if (body.length > 1024 * 64) req.destroy();
        });

        req.on('end', async () => {
            try {
                const parsed = body ? JSON.parse(body) : {};
                const cleanUsername = normalizeUsername(parsed.username);
                if (!cleanUsername) {
                    sendJson(res, 400, { error: 'Please enter a valid Instagram username.' });
                    return;
                }

                const client = new ApifyClient({ token: apiToken });
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
            } catch (err) {
                sendJson(res, 502, {
                    error: err?.message || 'Unable to load Instagram profile right now.',
                });
            }
        });
        return;
    }

    sendJson(res, 404, { error: 'Not found' });
});

server.listen(PORT, () => {
    console.log(`Instagram Profile Scraper backend listening on port ${PORT}`);
});
