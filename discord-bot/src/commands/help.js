import { SlashCommandBuilder, EmbedBuilder, ActionRowBuilder, StringSelectMenuBuilder } from 'discord.js';
import { CONFIG } from '../../config/constants.js';
import { EMOJIS } from '../utils/emojis.js';

export const data = new SlashCommandBuilder()
    .setName('help')
    .setDescription('Display InSightX HQ user command center');

const getEmojiId = (emojiTag) => emojiTag?.match(/:(\d+)>$/)?.[1];

const HELP_CATEGORIES = [
    {
        value: 'overview',
        label: 'Overview & Quick Start',
        description: 'Welcome guide and prefix command summary',
        emoji: getEmojiId(EMOJIS.HOME)
    },
    {
        value: 'account',
        label: 'Account & Profile',
        description: 'Dashboard, password & account linking commands',
        emoji: getEmojiId(EMOJIS.PROFILE)
    },
    {
        value: 'payments',
        label: 'Plans & UPI Payments',
        description: 'Purchase plans, subscriptions & payment status',
        emoji: getEmojiId(EMOJIS.PAYMENT)
    },
    {
        value: 'usage',
        label: 'Usage & Diagnostics',
        description: 'Action quotas, monthly stats & bot ping',
        emoji: getEmojiId(EMOJIS.USAGE)
    },
    {
        value: 'support',
        label: 'Support & Tickets',
        description: 'Create, reply, close & manage support tickets',
        emoji: getEmojiId(EMOJIS.SUPPORT)
    }
];

const ADMIN_HELP_CATEGORIES = [
    {
        value: 'overview',
        label: 'Admin Overview',
        description: 'Staff command center summary & guide',
        emoji: getEmojiId(EMOJIS.HOME)
    },
    {
        value: 'accounts',
        label: 'User Accounts & Device Lock',
        description: '.account-list, .user create, ban, reset, unlock',
        emoji: getEmojiId(EMOJIS.USERS)
    },
    {
        value: 'subscriptions',
        label: 'Subscription Management',
        description: '.subscription extend, expire, list',
        emoji: getEmojiId(EMOJIS.SUBSCRIPTION)
    },
    {
        value: 'payments',
        label: 'Payment Ledger & Queue',
        description: '.payment pending, approve, reject, refund',
        emoji: getEmojiId(EMOJIS.PAYMENT)
    },
    {
        value: 'usage',
        label: 'Quotas & Rate Limits',
        description: '.usage leaderboard, reset, set-limit',
        emoji: getEmojiId(EMOJIS.USAGE)
    },
    {
        value: 'system',
        label: 'System & Owner Controls',
        description: '.system status, uptime, .uploademojis, .owner',
        emoji: getEmojiId(EMOJIS.SYSTEM)
    }
];

export function createHelpEmbed(category = 'overview') {
    const embed = new EmbedBuilder()
        .setColor(CONFIG.COLORS.PRIMARY)
        .setFooter({ text: 'InSightX HQ • Prefix: . • Select a category below' })
        .setTimestamp();

    if (category === 'account') {
        return embed
            .setTitle(`${EMOJIS.PROFILE} InSightX HQ — Account & Profile`)
            .setDescription(`Manage your linked **InSightX** mobile app account and credentials:`)
            .addFields(
                {
                    name: `${EMOJIS.DASHBOARD} Dashboard`,
                    value:
                        `${EMOJIS.USER} \`.profile\` — View your account dashboard, active plan & daily quota\n` +
                        `*(Aliases: \`.myaccount\`, \`.dashboard\`, \`.account\`, \`.me\`)*`
                },
                {
                    name: `${EMOJIS.SECURITY} Security & Linking`,
                    value:
                        `${EMOJIS.VERIFIED} \`.change-password <new_password>\` — Update your app password *(min 6 chars)*\n` +
                        `${EMOJIS.USER_ADD} \`.link-account <username>\` — Link an existing app account to Discord\n` +
                        `${EMOJIS.USER_REMOVE} \`.unlink-account\` — Unlink your app account from Discord\n` +
                        `${EMOJIS.WARNING} \`.forgot-password\` — View password recovery instructions`
                }
            );
    }

    if (category === 'payments') {
        return embed
            .setTitle(`${EMOJIS.PAYMENT} InSightX HQ — Plans & UPI Payments`)
            .setDescription(`Subscribe, renew, and track your **InSightX** creator access:`)
            .addFields(
                {
                    name: `${EMOJIS.UPI} Checkout & Subscriptions`,
                    value:
                        `${EMOJIS.PREMIUM} \`.plans\` — View all subscription plans & pricing *(alias: \`.pricing\`)*\n` +
                        `${EMOJIS.PLAN} \`.purchase\` — Browse products & plans and pay via UPI *(alias: \`.buy\`)*\n` +
                        `${EMOJIS.SUBSCRIPTION} \`.subscription\` — View your active plan & days remaining *(alias: \`.expiry\`)*\n` +
                        `${EMOJIS.TRANSACTION} \`.payment-history\` — Check submitted UPI payments & status *(alias: \`.payment-status\`)*`
                }
            );
    }

    if (category === 'usage') {
        return embed
            .setTitle(`${EMOJIS.USAGE} InSightX HQ — Usage & Diagnostics`)
            .setDescription(`Monitor your action quotas and live bot connection health:`)
            .addFields(
                {
                    name: `${EMOJIS.ANALYTICS} Quotas & Downloads`,
                    value:
                        `${EMOJIS.PACKAGE || '📦'} \`.apk\` — Download latest Instagram APK build directly *(alias: \`.app\`)*\n` +
                        `${EMOJIS.LIMIT} \`.myusage\` — Check today's action quota & monthly total *(alias: \`.mylimit\`)*\n` +
                        `${EMOJIS.SERVER} \`.ping\` — Check message latency & WebSocket gateway speed\n` +
                        `${EMOJIS.HOME} \`.help\` — Open this interactive command center *(alias: \`.commands\`)*`
                }
            );
    }

    if (category === 'support') {
        return embed
            .setTitle(`${EMOJIS.SUPPORT} InSightX HQ — Support & Tickets`)
            .setDescription(`Need help with your account or payment? Manage tickets right here:`)
            .addFields(
                {
                    name: `${EMOJIS.TICKET} Ticket Commands`,
                    value:
                        `${EMOJIS.SUCCESS} \`.support create <subject>\` — Open a new support ticket\n` +
                        `${EMOJIS.LOGS} \`.support list\` — View your support tickets\n` +
                        `${EMOJIS.REFRESH} \`.support reply <ticket_id> <message>\` — Reply to a ticket\n` +
                        `${EMOJIS.ERROR} \`.support close <ticket_id>\` — Close a support ticket\n` +
                        `${EMOJIS.PENDING} \`.support reopen <ticket_id>\` — Reopen a closed ticket`
                }
            );
    }

    // Default: overview
    return embed
        .setTitle(`${EMOJIS.DASHBOARD} InSightX HQ — Command Center`)
        .setDescription(
            `Welcome to **InSightX HQ** ${EMOJIS.INSTAGRAM}!\n` +
            `All user commands use the **\`.\` prefix**. Pick a category from the dropdown menu below to view details.\n\n` +
            `${EMOJIS.PROFILE} **Account** — \`.profile\`, \`.change-password\`, \`.link-account\`, \`.unlink-account\`\n` +
            `${EMOJIS.PAYMENT} **Payments** — \`.plans\`, \`.purchase\`, \`.subscription\`, \`.payment-history\`\n` +
            `${EMOJIS.USAGE} **Usage & Info** — \`.myusage\`, \`.ping\`, \`.help\`\n` +
            `${EMOJIS.SUPPORT} **Support** — \`.support create\`, \`.support list\`, \`.support reply\``
        );
}

export function createHelpSelectMenu(selectedCategory = 'overview') {
    const select = new StringSelectMenuBuilder()
        .setCustomId('help_category')
        .setPlaceholder('Choose a command category...')
        .addOptions(
            HELP_CATEGORIES.map(cat => ({
                label: cat.label,
                description: cat.description,
                value: cat.value,
                emoji: cat.emoji,
                default: cat.value === selectedCategory
            }))
        );

    return new ActionRowBuilder().addComponents(select);
}

export function buildHelpPayload(category = 'overview') {
    return {
        embeds: [createHelpEmbed(category)],
        components: [createHelpSelectMenu(category)]
    };
}

export function createAdminHelpEmbed(category = 'overview') {
    const embed = new EmbedBuilder()
        .setColor(CONFIG.COLORS.INFO)
        .setFooter({ text: 'InSightX HQ • Staff Control Center • Select a category below' })
        .setTimestamp();

    if (category === 'accounts') {
        return embed
            .setTitle(`${EMOJIS.USERS} InSightX HQ — User Accounts & Device Controls`)
            .setDescription(`Full customer profile, credentials, and hardware device lock management:`)
            .addFields(
                {
                    name: `${EMOJIS.PROFILE} Account Inspection`,
                    value:
                        `• \`.account-list [page]\` — Show all database accounts, Discord links & device locks *(aliases: \`.accounts\`, \`.accountlist\`)*\n` +
                        `• \`.user info <target>\` — View detailed profile, UUID, status & discord ID`
                },
                {
                    name: `${EMOJIS.SECURITY} Credentials & Device Locks`,
                    value:
                        `• \`.user create <username> [password] [duration] [@user]\` — Provision user credentials & validity (e.g. 7d, 30d, 365d, lifetime) & auto-DM user\n` +
                        `• \`.user reset-password <target> [new_pass]\` — Set a new app login password\n` +
                        `• \`.user unlock-device <target>\` — Clear hardware device lock so customer can log in on a new phone *(alias: \`.user reset-device\`)*`
                },
                {
                    name: `${EMOJIS.WARNING} Moderation Actions`,
                    value:
                        `• \`.user suspend <target> [reason]\` — Temporarily block app login\n` +
                        `• \`.user unsuspend <target>\` — Lift suspension & restore access\n` +
                        `• \`.user ban <target> [reason]\` — Permanently ban user account\n` +
                        `• \`.user unban <target>\` — Lift permanent ban\n` +
                        `• \`.user delete <target>\` — Purge user profile & auth records`
                }
            );
    }

    if (category === 'subscriptions') {
        return embed
            .setTitle(`${EMOJIS.SUBSCRIPTION} InSightX HQ — Subscription Management`)
            .setDescription(`Manage active subscriptions, validity durations, and expirations:`)
            .addFields(
                {
                    name: `${EMOJIS.PLAN} Validity Controls`,
                    value:
                        `• \`.subscription extend <target> <days>\` — Add extra days to user's subscription\n` +
                        `• \`.subscription expire <target>\` — Immediately expire subscription access\n` +
                        `• \`.subscription add <target> <days>\` — Provision new subscription for user`
                },
                {
                    name: `${EMOJIS.EXPIRY} Lists & Durations`,
                    value:
                        `• \`.subscription list\` — View all active subscribers in the platform\n` +
                        `• \`.subscription expired\` — View recently expired accounts\n` +
                        `• \`.plans\` — Display the 7 active pricing plans (₹99 to ₹1,999)`
                }
            );
    }

    if (category === 'payments') {
        return embed
            .setTitle(`${EMOJIS.PAYMENT} InSightX HQ — Payment Verification Queue & Ledger`)
            .setDescription(`Review UPI transaction proofs, approve accounts, or issue refunds:`)
            .addFields(
                {
                    name: `${EMOJIS.PENDING} Verification Queue`,
                    value:
                        `• \`.payment pending\` — List pending payment screenshots waiting in queue\n` +
                        `• \`.payment approve <reference>\` — Verify transaction, create account & DM spoiler credentials\n` +
                        `• \`.payment reject <reference> [reason]\` — Reject verification & DM user the reason`
                },
                {
                    name: `${EMOJIS.TRANSACTION} Ledger & Refunds`,
                    value:
                        `• \`.payment refund <reference>\` — Mark refunded & revoke subscription access\n` +
                        `• \`.payment history\` — View last 10 transaction records\n` +
                        `• \`.payment accepted\` — View verified approved payments\n` +
                        `• \`.payment rejected\` — View declined payment attempts`
                }
            );
    }

    if (category === 'usage') {
        return embed
            .setTitle(`${EMOJIS.USAGE} InSightX HQ — Quota & Rate Limit Controls`)
            .setDescription(`Monitor and adjust customer daily action limits and metrics:`)
            .addFields(
                {
                    name: `${EMOJIS.LIMIT} Quota Actions`,
                    value:
                        `• \`.usage leaderboard\` — Top monthly active users by action volume\n` +
                        `• \`.usage reset <target>\` — Reset today's daily action counter for a user\n` +
                        `• \`.usage set-limit <target> <limit>\` — Set custom daily quota limit *(default: 100)*`
                }
            );
    }

    if (category === 'system') {
        return embed
            .setTitle(`${EMOJIS.SYSTEM} InSightX HQ — System & Infrastructure Controls`)
            .setDescription(`Platform telemetry, server customization, and owner commands:`)
            .addFields(
                {
                    name: `${EMOJIS.SERVER} Health & Telemetry`,
                    value:
                        `• \`.system status\` / \`.admin stats\` — Real-time revenue, accounts & database telemetry\n` +
                        `• \`.system uptime\` — Bot process uptime duration\n` +
                        `• \`.uploademojis [prefix]\` — Upload custom server emojis from an attached .zip`
                },
                {
                    name: `${EMOJIS.OWNER} Owner Infrastructure Controls`,
                    value:
                        `• \`.owner broadcast <message>\` — Post official embed broadcast to channel\n` +
                        `• \`.owner maintenance <on|off>\` — Toggle platform maintenance mode\n` +
                        `• \`.owner set-prefix <prefix>\` — Change global bot prefix\n` +
                        `• \`.owner setup\` — Run diagnostic check on database tables, plans & storage`
                }
            );
    }

    // Default: overview
    return embed
        .setTitle(`${EMOJIS.ADMIN} InSightX HQ — Administrator Command Guide`)
        .setDescription(
            `Welcome to the **InSightX HQ Staff Control Center** ${EMOJIS.SECURITY}!\n` +
            `All administrative commands use the **\`.\` prefix**. Pick a category below to explore available tools:\n\n` +
            `${EMOJIS.USERS} **User Accounts** — \`.account-list\`, \`.user create\`, \`.user info\`, \`.user reset-password\`, \`.user unlock-device\`, \`.user ban\`\n` +
            `${EMOJIS.SUBSCRIPTION} **Subscriptions** — \`.subscription extend\`, \`.subscription expire\`, \`.subscription list\`\n` +
            `${EMOJIS.PAYMENT} **Payments** — \`.payment pending\`, \`.payment approve\`, \`.payment reject\`, \`.payment refund\`\n` +
            `${EMOJIS.USAGE} **Usage & Limits** — \`.usage leaderboard\`, \`.usage reset\`, \`.usage set-limit\`\n` +
            `${EMOJIS.SYSTEM} **System & Owner** — \`.system status\`, \`.system uptime\`, \`.uploademojis\`, \`.owner\``
        );
}

export function createAdminHelpSelectMenu(selectedCategory = 'overview') {
    const select = new StringSelectMenuBuilder()
        .setCustomId('adhelp_category')
        .setPlaceholder('Choose an administrative category...')
        .addOptions(
            ADMIN_HELP_CATEGORIES.map(cat => ({
                label: cat.label,
                description: cat.description,
                value: cat.value,
                emoji: cat.emoji,
                default: cat.value === selectedCategory
            }))
        );

    return new ActionRowBuilder().addComponents(select);
}

export function buildAdminHelpPayload(category = 'overview') {
    return {
        embeds: [createAdminHelpEmbed(category)],
        components: [createAdminHelpSelectMenu(category)]
    };
}

export async function execute(interaction) {
    await interaction.reply({ ...buildHelpPayload('overview'), ephemeral: true });
}
