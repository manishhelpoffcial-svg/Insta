import { SlashCommandBuilder, EmbedBuilder } from 'discord.js';
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import { execFile } from 'node:child_process';
import { promisify } from 'node:util';
import { isAdministrator } from '../../interactions/buttons/approve_payment.js';
import { CONFIG } from '../../../config/constants.js';

const execFileAsync = promisify(execFile);

export const data = new SlashCommandBuilder()
    .setName('uploademojis')
    .setDescription('Unpack a zip file and upload all images as server custom emojis')
    .addAttachmentOption(opt =>
        opt.setName('file')
            .setDescription('ZIP file containing PNG, JPG, WEBP, or GIF emoji images')
            .setRequired(true)
    )
    .addStringOption(opt =>
        opt.setName('prefix')
            .setDescription('Optional prefix to prepend to emoji names (e.g. ig_)')
            .setRequired(false)
    );

export async function processEmojiZip({ guild, attachmentUrl, attachmentName, prefix = '', progressCallback }) {
    if (!guild) throw new Error('Emojis can only be uploaded inside a Discord server (guild).');

    // Check bot permissions
    const botMember = guild.members.me;
    if (!botMember?.permissions?.has('ManageGuildExpressions') && !botMember?.permissions?.has('ManageEmojisAndStickers') && !botMember?.permissions?.has('Administrator')) {
        throw new Error('Bot is missing the "Manage Emojis and Stickers" (ManageGuildExpressions) permission in this server.');
    }

    const baseTemp = process.env.TMPDIR || os.tmpdir() || '/data/user/0/ai.antigem/files/usr/tmp';
    const tempDir = path.join(baseTemp, `emojis_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`);
    const zipPath = path.join(tempDir, 'emojis.zip');
    const extractDir = path.join(tempDir, 'extracted');

    fs.mkdirSync(extractDir, { recursive: true });

    try {
        if (progressCallback) await progressCallback('📥 Downloading ZIP archive...');

        // 1. Download ZIP file
        const response = await fetch(attachmentUrl);
        if (!response.ok) throw new Error(`Failed to download attachment (${response.statusText})`);

        const arrayBuffer = await response.arrayBuffer();
        fs.writeFileSync(zipPath, Buffer.from(arrayBuffer));

        if (progressCallback) await progressCallback('📦 Unzipping image files...');

        // 2. Unzip using system unzip binary
        await execFileAsync('unzip', ['-o', zipPath, '-d', extractDir]);

        // 3. Find all supported image files recursively
        const supportedExts = new Set(['.png', '.jpg', '.jpeg', '.webp', '.gif']);
        const imageFiles = [];

        function scanDir(dir) {
            const entries = fs.readdirSync(dir, { withFileTypes: true });
            for (const entry of entries) {
                const fullPath = path.join(dir, entry.name);
                if (entry.isDirectory()) {
                    if (entry.name !== '__MACOSX') scanDir(fullPath);
                } else if (entry.isFile()) {
                    const ext = path.extname(entry.name).toLowerCase();
                    if (supportedExts.has(ext) && !entry.name.startsWith('.')) {
                        imageFiles.push(fullPath);
                    }
                }
            }
        }

        scanDir(extractDir);

        if (imageFiles.length === 0) {
            throw new Error('No valid image files (PNG, JPG, WEBP, GIF) were found inside the ZIP file.');
        }

        if (progressCallback) await progressCallback(`🔍 Found **${imageFiles.length}** images. Beginning server upload...`);

        const uploadedEmojis = [];
        const failedEmojis = [];

        // 4. Iterate and upload emojis with rate limit pacing
        for (let i = 0; i < imageFiles.length; i++) {
            const filePath = imageFiles[i];
            const baseFileName = path.basename(filePath, path.extname(filePath));

            // Clean & sanitize emoji name (alphanumeric & underscore only, 2-32 chars)
            let sanitized = baseFileName.replace(/[^a-zA-Z0-9_]/g, '_').replace(/^_+|_+$/g, '');
            if (prefix) {
                const cleanPrefix = prefix.replace(/[^a-zA-Z0-9_]/g, '_');
                sanitized = `${cleanPrefix}_${sanitized}`;
            }

            if (sanitized.length < 2) sanitized = `emoji_${sanitized}`;
            if (sanitized.length > 32) sanitized = sanitized.slice(0, 32);

            try {
                const fileStats = fs.statSync(filePath);
                if (fileStats.size > 256 * 1024) {
                    failedEmojis.push({ name: sanitized, reason: `File exceeds Discord 256KB limit (${Math.round(fileStats.size / 1024)}KB)` });
                    continue;
                }

                const fileBuffer = fs.readFileSync(filePath);
                const createdEmoji = await guild.emojis.create({
                    attachment: fileBuffer,
                    name: sanitized,
                    reason: `Uploaded by InSightX HQ ZIP Importer`
                });

                uploadedEmojis.push(createdEmoji);

                // Small delay to respect Discord emoji upload rate limits
                await new Promise(r => setTimeout(r, 400));
            } catch (err) {
                failedEmojis.push({ name: sanitized, reason: err.message || 'Discord API error' });
            }
        }

        return {
            totalFound: imageFiles.length,
            uploaded: uploadedEmojis,
            failed: failedEmojis
        };

    } finally {
        // Clean up temp directory
        try {
            fs.rmSync(tempDir, { recursive: true, force: true });
        } catch {}
    }
}

export function buildEmojiUploadEmbed({ totalFound, uploaded, failed, guildName }) {
    const isSuccess = uploaded.length > 0;
    const embed = new EmbedBuilder()
        .setColor(isSuccess ? CONFIG.COLORS.SUCCESS : CONFIG.COLORS.DANGER)
        .setTitle(`🎨 Emoji ZIP Upload Results — ${guildName}`)
        .addFields(
            { name: '📦 Total Found in ZIP', value: `\`${totalFound}\``, inline: true },
            { name: '✅ Successfully Added', value: `\`${uploaded.length}\``, inline: true },
            { name: '⚠️ Skipped / Failed', value: `\`${failed.length}\``, inline: true }
        )
        .setFooter({ text: 'InSightX HQ Emoji Importer' })
        .setTimestamp();

    if (uploaded.length > 0) {
        // Render preview tags (first 35 emojis)
        const sample = uploaded.slice(0, 35).map(e => `${e}`).join(' ');
        embed.setDescription(`**Uploaded Emojis:**\n${sample}${uploaded.length > 35 ? `\n*...and ${uploaded.length - 35} more*` : ''}`);
    }

    if (failed.length > 0) {
        const errorSummary = failed.slice(0, 5).map(f => `• \`${f.name}\`: *${f.reason}*`).join('\n');
        embed.addFields({
            name: '❌ Failures Sample',
            value: errorSummary + (failed.length > 5 ? `\n*...and ${failed.length - 5} more errors*` : '')
        });
    }

    return embed;
}

export async function execute(interaction) {
    if (!isAdministrator(interaction.member)) {
        return interaction.reply({ content: '❌ You must be an administrator or owner to use this command.', ephemeral: true });
    }

    if (!interaction.guild) {
        return interaction.reply({ content: '❌ This command must be executed within a Discord server.', ephemeral: true });
    }

    const attachment = interaction.options.getAttachment('file');
    const prefix = interaction.options.getString('prefix') || '';

    if (!attachment.name.endsWith('.zip')) {
        return interaction.reply({ content: '❌ The uploaded file must be a `.zip` archive.', ephemeral: true });
    }

    await interaction.deferReply();

    try {
        const results = await processEmojiZip({
            guild: interaction.guild,
            attachmentUrl: attachment.url,
            attachmentName: attachment.name,
            prefix,
            progressCallback: async (statusText) => {
                await interaction.editReply({ content: statusText }).catch(() => {});
            }
        });

        const embed = buildEmojiUploadEmbed({
            totalFound: results.totalFound,
            uploaded: results.uploaded,
            failed: results.failed,
            guildName: interaction.guild.name
        });

        await interaction.editReply({ content: null, embeds: [embed] });
    } catch (err) {
        await interaction.editReply({ content: `❌ Error processing emoji zip: ${err.message}` });
    }
}
