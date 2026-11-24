import { SlashCommandBuilder, EmbedBuilder } from 'discord.js';
import { getSupabase } from '../../database/supabaseClient.js';
import { CONFIG } from '../../../config/constants.js';
import { isAdministrator } from '../../interactions/buttons/approve_payment.js';
import { getSystemStatistics } from '../../admin/adminActions.js';
import { formatCurrency, formatDate } from '../../utils/validators.js';
import { EMOJIS } from '../../utils/emojis.js';

export const data = new SlashCommandBuilder()
    .setName('system')
    .setDescription('Platform system monitoring, server health, diagnostics and live metrics')
    .addSubcommand(sub => sub.setName('status').setDescription('Complete platform health and operational status'))
    .addSubcommand(sub => sub.setName('ping').setDescription('Detailed network and WebSocket latency'))
    .addSubcommand(sub => sub.setName('uptime').setDescription('Process and host server uptime'))
    .addSubcommand(sub => sub.setName('database').setDescription('Database connection, latency, and table row metrics'))
    .addSubcommand(sub => sub.setName('api').setDescription('Supabase and Discord API endpoint status'))
    .addSubcommand(sub => sub.setName('api-usage').setDescription('API call metrics and Apify token rotation state'))
    .addSubcommand(sub => sub.setName('errors').setDescription('View recent system exceptions'))
    .addSubcommand(sub => sub.setName('logs').setDescription('View latest administrative audit logs'))
    .addSubcommand(sub => sub.setName('stats').setDescription('Real-time database records and financial totals'))
    .addSubcommand(sub => sub.setName('users').setDescription('Total registered user accounts'))
    .addSubcommand(sub => sub.setName('active-users').setDescription('Active unexpired user accounts'))
    .addSubcommand(sub => sub.setName('online-users').setDescription('Users with recent mobile device sessions'))
    .addSubcommand(sub => sub.setName('payments').setDescription('Total payments volume and pending requests'))
    .addSubcommand(sub => sub.setName('subscriptions').setDescription('Active vs expired subscriptions overview'));

export async function execute(interaction) {
    if (!isAdministrator(interaction.member)) {
        return interaction.reply({ content: `${EMOJIS.ERROR} You must be an administrator or owner to use this command.`, ephemeral: true });
    }

    const sub = interaction.options.getSubcommand();
    const supabase = getSupabase();
    await interaction.deferReply({ ephemeral: true });

    try {
        if (sub === 'status' || sub === 'stats') {
            const stats = await getSystemStatistics();
            const embed = new EmbedBuilder()
                .setColor(CONFIG.COLORS.PRIMARY)
                .setTitle(`${EMOJIS.SYSTEM} InSightX HQ — System Status & Metrics`)
                .addFields(
                    { name: `${EMOJIS.USERS} Accounts`, value: `Total: \`${stats.totalAccounts}\` (Active: \`${stats.activeAccounts}\`, Suspended: \`${stats.suspendedAccounts}\`)`, inline: false },
                    { name: `${EMOJIS.SUBSCRIPTION} Subscriptions`, value: `Active: \`${stats.activeSubs}\` | Expired: \`${stats.expiredSubs}\``, inline: true },
                    { name: `${EMOJIS.PAYMENT} Payments`, value: `Total: \`${stats.totalPayments}\` | Review: \`${stats.pendingPayments}\``, inline: true },
                    { name: `${EMOJIS.MONEY} Revenue`, value: `**${formatCurrency(stats.totalRevenue)}**`, inline: true },
                    { name: `${EMOJIS.API} Gateway Latency`, value: `\`${interaction.client.ws.ping}ms\``, inline: true },
                    { name: `${EMOJIS.SERVER} System Uptime`, value: `\`${Math.floor(process.uptime() / 60)} mins\``, inline: true }
                )
                .setFooter({ text: 'InSightX HQ Live Telemetry' })
                .setTimestamp();

            return await interaction.editReply({ embeds: [embed] });
        }

        if (sub === 'ping') {
            const embed = new EmbedBuilder()
                .setColor(CONFIG.COLORS.SUCCESS)
                .setTitle(`${EMOJIS.SYSTEM} Ping Diagnostics`)
                .addFields(
                    { name: `${EMOJIS.API} Discord Gateway`, value: `\`${interaction.client.ws.ping}ms\``, inline: true },
                    { name: `${EMOJIS.SERVER} Node Runtime`, value: `\`${process.version}\``, inline: true },
                    { name: '🧠 Memory RSS', value: `\`${Math.round(process.memoryUsage().rss / 1024 / 1024)} MB\``, inline: true }
                );

            return await interaction.editReply({ embeds: [embed] });
        }

        if (sub === 'uptime') {
            const s = Math.floor(process.uptime());
            const days = Math.floor(s / 86400);
            const hrs = Math.floor((s % 86400) / 3600);
            const mins = Math.floor((s % 3600) / 60);
            const secs = s % 60;

            return await interaction.editReply(`${EMOJIS.SERVER} InSightX HQ Uptime: **${days}d ${hrs}h ${mins}m ${secs}s**`);
        }

        if (sub === 'database') {
            const start = Date.now();
            const { count: users } = await supabase.from('profiles').select('*', { count: 'exact', head: true });
            const { count: subs } = await supabase.from('subscriptions').select('*', { count: 'exact', head: true });
            const { count: payments } = await supabase.from('payments').select('*', { count: 'exact', head: true });
            const { count: plans } = await supabase.from('plans').select('*', { count: 'exact', head: true });
            const latency = Date.now() - start;

            const embed = new EmbedBuilder()
                .setColor(CONFIG.COLORS.INFO)
                .setTitle(`${EMOJIS.DATABASE} Database Health & Tables`)
                .addFields(
                    { name: `${EMOJIS.SERVER} Latency`, value: `\`${latency}ms\``, inline: true },
                    { name: `${EMOJIS.USERS} Profiles`, value: `\`${users || 0}\``, inline: true },
                    { name: `${EMOJIS.SUBSCRIPTION} Subscriptions`, value: `\`${subs || 0}\``, inline: true },
                    { name: `${EMOJIS.PAYMENT} Payments`, value: `\`${payments || 0}\``, inline: true },
                    { name: `${EMOJIS.PLAN} Plans`, value: `\`${plans || 0}\``, inline: true }
                );

            return await interaction.editReply({ embeds: [embed] });
        }

        if (sub === 'api' || sub === 'api-usage') {
            const embed = new EmbedBuilder()
                .setColor(CONFIG.COLORS.INFO)
                .setTitle(`${EMOJIS.API} External APIs & Scraper Pool`)
                .addFields(
                    { name: '📡 Discord Gateway', value: '`CONNECTED`', inline: true },
                    { name: `${EMOJIS.DATABASE} Supabase REST / Auth`, value: '`ONLINE`', inline: true },
                    { name: `${EMOJIS.SECURITY} Apify Rotation Pool`, value: '`Primary + 7 Fallback Tokens Active`', inline: true },
                    { name: '🛡️ Fallback Triggers', value: '`HTTP 401, 402 (Credits Expired), 403, 429`', inline: false }
                );

            return await interaction.editReply({ embeds: [embed] });
        }

        if (sub === 'errors') {
            const { data: errors } = await supabase.from('system_errors').select('*').order('created_at', { ascending: false }).limit(5);
            if (!errors || errors.length === 0) return await interaction.editReply(`${EMOJIS.SUCCESS} No logged system errors.`);

            const embed = new EmbedBuilder()
                .setColor(CONFIG.COLORS.DANGER)
                .setTitle(`${EMOJIS.WARNING} Recent System Exceptions`)
                .setDescription(errors.map(e => `• \`${e.error_type}\`: *${e.message}* (${formatDate(e.created_at)})`).join('\n'));

            return await interaction.editReply({ embeds: [embed] });
        }

        if (sub === 'users' || sub === 'active-users' || sub === 'online-users') {
            const { count: total } = await supabase.from('profiles').select('*', { count: 'exact', head: true });
            const { count: active } = await supabase.from('profiles').select('*', { count: 'exact', head: true }).eq('account_status', 'active');
            const { count: online } = await supabase.from('device_sessions').select('*', { count: 'exact', head: true }).eq('session_status', 'active');

            return await interaction.editReply(`${EMOJIS.USERS} Users: Total: \`${total || 0}\` | Active Accounts: \`${active || 0}\` | Active Sessions: \`${online || 0}\``);
        }

        if (sub === 'payments') {
            const { count: total } = await supabase.from('payments').select('*', { count: 'exact', head: true });
            const { count: pending } = await supabase.from('payments').select('*', { count: 'exact', head: true }).eq('payment_status', 'pending_review');
            const { data: approved } = await supabase.from('payments').select('amount').eq('payment_status', 'approved');
            const rev = approved?.reduce((s, p) => s + Number(p.amount), 0) || 0;

            return await interaction.editReply(`${EMOJIS.PAYMENT} Payments: Total: \`${total || 0}\` | Review Queue: \`${pending || 0}\` | Revenue: **${formatCurrency(rev)}**`);
        }

        if (sub === 'subscriptions') {
            const { count: active } = await supabase.from('subscriptions').select('*', { count: 'exact', head: true }).eq('subscription_status', 'active').gt('expiry_date', new Date().toISOString());
            const { count: expired } = await supabase.from('subscriptions').select('*', { count: 'exact', head: true }).or(`subscription_status.eq.expired,expiry_date.lte.${new Date().toISOString()}`);

            return await interaction.editReply(`${EMOJIS.SUBSCRIPTION} Subscriptions: Active: \`${active || 0}\` | Expired: \`${expired || 0}\``);
        }

        if (sub === 'logs') {
            const { data: logs } = await supabase.from('admin_logs').select('*').order('created_at', { ascending: false }).limit(10);
            if (!logs || logs.length === 0) return await interaction.editReply(`${EMOJIS.WARNING} No recent logs.`);

            const embed = new EmbedBuilder()
                .setColor(CONFIG.COLORS.INFO)
                .setTitle(`${EMOJIS.LOGS} System Logs`)
                .setDescription(logs.map(l => `• \`${l.action}\`: ${l.description}`).join('\n'));

            return await interaction.editReply({ embeds: [embed] });
        }
    } catch (err) {
        return await interaction.editReply(`${EMOJIS.ERROR} Error: ${err.message}`);
    }
}
