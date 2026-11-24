import { SlashCommandBuilder, EmbedBuilder } from 'discord.js';
import { CONFIG } from '../../config/constants.js';
import { EMOJIS } from '../utils/emojis.js';
import { isAdministrator } from '../interactions/buttons/approve_payment.js';

export const data = new SlashCommandBuilder()
    .setName('help')
    .setDescription('Display InSightX HQ command center and documentation');

export function createHelpEmbed(isAdmin = false) {
    const embed = new EmbedBuilder()
        .setColor(CONFIG.COLORS.PRIMARY)
        .setTitle(`${EMOJIS.DASHBOARD} InSightX HQ — Command Center`)
        .setDescription(
            `Welcome to **InSightX HQ**, the supreme creator management system for Instagram Clone.\n\n` +
            `Both **Slash Commands** (\`/\`) and **Prefix Commands** (\`.\`) are fully supported.`
        )
        .addFields(
            {
                name: `${EMOJIS.USER} Customer & Self-Service Commands`,
                value:
                    `• \`/profile\` or \`.profile\` — Account dashboard, active plan & daily quota\n` +
                    `• \`/purchase\` or \`.purchase\` — Dynamic UPI checkout for subscription plans\n` +
                    `• \`/subscription\` or \`.subscription\` — View active plan & days remaining\n` +
                    `• \`/myusage\` or \`.myusage\` — Real-time action quota & monthly metrics\n` +
                    `• \`/payment-history\` or \`.payment-history\` — Past transactions & review status\n` +
                    `• \`/change-password\` or \`.change-password\` — Update your app login password\n` +
                    `• \`/support create\` or \`.support create\` — Open an administrative support ticket\n` +
                    `• \`/ping\` or \`.ping\` — Live bot, Supabase & gateway diagnostics`
            }
        )
        .setFooter({ text: 'InSightX HQ • Dual Slash & Prefix Engine' })
        .setTimestamp();

    if (isAdmin) {
        embed.addFields(
            {
                name: `${EMOJIS.ADMIN} Administration Commands`,
                value:
                    `• \`/user <create|info|search|edit|ban|suspend|reset-password>\`\n` +
                    `• \`/subscription-admin <add|extend|expire|list|expiring>\`\n` +
                    `• \`/payment <pending|approve|reject|history|refund>\`\n` +
                    `• \`/usage <check|leaderboard|reset|set-limit|add-limit>\`\n` +
                    `• \`/support <reply|close|reopen|list|assign>\`\n` +
                    `• \`/uploademojis\` or \`.uploademojis\` — Auto-unpack & import ZIP emoji packs`
            },
            {
                name: `${EMOJIS.OWNER} Owner & Infrastructure Controls`,
                value:
                    `• \`/owner <setup|database|api-status|maintenance|broadcast|set-prefix>\`\n` +
                    `• \`/system <status|ping|uptime|database|users|payments>\``
            }
        );
    }

    return embed;
}

export async function execute(interaction) {
    const isAdmin = isAdministrator(interaction.member);
    const embed = createHelpEmbed(isAdmin);
    await interaction.reply({ embeds: [embed], ephemeral: true });
}
