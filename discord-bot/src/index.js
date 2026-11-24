import { Client, GatewayIntentBits, Partials } from 'discord.js';
import { CONFIG, validateConfig } from '../config/constants.js';
import { onReady } from './events/ready.js';
import { onInteractionCreate } from './events/interactionCreate.js';
import { onMessageCreate } from './events/messageCreate.js';
import { startExpirationScheduler } from './scheduler/expirationCron.js';

validateConfig();

const client = new Client({
    intents: [
        GatewayIntentBits.Guilds,
        GatewayIntentBits.GuildMessages,
        GatewayIntentBits.MessageContent,
        GatewayIntentBits.DirectMessages
    ],
    partials: [
        Partials.Channel,
        Partials.Message,
        Partials.User
    ]
});

client.once('ready', () => {
    onReady(client);
    startExpirationScheduler(client);
});

client.on('interactionCreate', onInteractionCreate);
client.on('messageCreate', onMessageCreate);

if (CONFIG.DISCORD_BOT_TOKEN) {
    client.login(CONFIG.DISCORD_BOT_TOKEN).catch(err => {
        console.error('[Discord Bot Login Error]', err.message);
    });
} else {
    console.warn('[Discord Bot] DISCORD_BOT_TOKEN not configured yet. Bot will start once configured in .env');
}
