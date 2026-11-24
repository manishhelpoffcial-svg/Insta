import dotenv from 'dotenv';
dotenv.config();

export const CONFIG = {
    SUPABASE_URL: process.env.SUPABASE_URL || '',
    SUPABASE_PUBLISHABLE_KEY: process.env.SUPABASE_PUBLISHABLE_KEY || '',
    SUPABASE_SERVICE_ROLE_KEY: process.env.SUPABASE_SERVICE_ROLE_KEY || '',

    DISCORD_BOT_TOKEN: process.env.DISCORD_BOT_TOKEN || '',
    DISCORD_CLIENT_ID: process.env.DISCORD_CLIENT_ID || '',
    DISCORD_GUILD_ID: process.env.DISCORD_GUILD_ID || '',

    ADMIN_ROLE_ID: process.env.ADMIN_ROLE_ID || '',
    PAYMENT_LOG_CHANNEL_ID: process.env.PAYMENT_LOG_CHANNEL_ID || '',
    OWNER_ID: process.env.OWNER_ID || '1120295079026888715',

    UPI_ID: process.env.UPI_ID || 'merchant@upi',
    MERCHANT_NAME: process.env.MERCHANT_NAME || 'Instagram Pro Tools',

    SMTP_HOST: process.env.SMTP_HOST || '',
    SMTP_PORT: Number(process.env.SMTP_PORT || 587),
    SMTP_USER: process.env.SMTP_USER || '',
    SMTP_PASS: process.env.SMTP_PASS || '',
    FROM_EMAIL: process.env.FROM_EMAIL || 'support@instagramclone.app',

    COLORS: {
        PRIMARY: 0xE1306C,     // Instagram gradient pink
        SUCCESS: 0x00BA88,     // Green
        WARNING: 0xF77737,     // Orange
        DANGER: 0xED4956,      // Red
        DARK: 0x121212,        // Dark background
        INFO: 0x3897F0,        // Instagram Blue
    }
};

export function validateConfig() {
    const required = [
        'SUPABASE_URL',
        'SUPABASE_SERVICE_ROLE_KEY',
        'DISCORD_BOT_TOKEN',
        'DISCORD_CLIENT_ID',
        'ADMIN_ROLE_ID',
        'PAYMENT_LOG_CHANNEL_ID'
    ];

    const missing = required.filter(key => !CONFIG[key]);
    if (missing.length > 0) {
        console.warn(`[CONFIG WARNING] Missing required environment variables: ${missing.join(', ')}`);
        console.warn('Please configure these in discord-bot/.env');
    }
}
