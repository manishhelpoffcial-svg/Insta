import { SlashCommandBuilder, EmbedBuilder, PermissionFlagsBits } from 'discord.js';
import { isAdministrator } from '../interactions/buttons/approve_payment.js';
import { findUser, updateUserAccountStatus, extendUserSubscription, getSystemStatistics } from '../admin/adminActions.js';
import { getSupabase } from '../database/supabaseClient.js';
import { createCustomerOverviewEmbed } from '../embeds/adminEmbeds.js';
import { CONFIG } from '../../config/constants.js';
import { formatCurrency, formatDate } from '../utils/validators.js';
import { EMOJIS } from '../utils/emojis.js';

export const data = new SlashCommandBuilder()
    .setName('admin')
    .setDescription('Administrative management for Instagram Clone accounts and subscriptions')
    // 1. userinfo
    .addSubcommand(sub =>
        sub.setName('userinfo')
            .setDescription('View customer account, subscription and payment details')
            .addStringOption(opt => opt.setName('target').setDescription('Username, Email, Discord ID or UUID').setRequired(true))
    )
    // 2. search
    .addSubcommand(sub =>
        sub.setName('search')
            .setDescription('Search user profiles by username or email')
            .addStringOption(opt => opt.setName('query').setDescription('Username or email keyword').setRequired(true))
    )
    // 3. activate
    .addSubcommand(sub =>
        sub.setName('activate')
            .setDescription('Reactivate an account')
            .addStringOption(opt => opt.setName('target').setDescription('Username, Email, or Discord ID').setRequired(true))
    )
    // 4. suspend
    .addSubcommand(sub =>
        sub.setName('suspend')
            .setDescription('Suspend an account')
            .addStringOption(opt => opt.setName('target').setDescription('Username, Email, or Discord ID').setRequired(true))
            .addStringOption(opt => opt.setName('reason').setDescription('Reason for suspension').setRequired(false))
    )
    // 5. ban
    .addSubcommand(sub =>
        sub.setName('ban')
            .setDescription('Ban an account permanently')
            .addStringOption(opt => opt.setName('target').setDescription('Username, Email, or Discord ID').setRequired(true))
            .addStringOption(opt => opt.setName('reason').setDescription('Reason for ban').setRequired(false))
    )
    // 6. extend
    .addSubcommand(sub =>
        sub.setName('extend')
            .setDescription('Extend an active subscription duration')
            .addStringOption(opt => opt.setName('target').setDescription('Username, Email, or Discord ID').setRequired(true))
            .addIntegerOption(opt => opt.setName('days').setDescription('Additional days to grant').setRequired(true).setMinValue(1))
    )
    // 7. createplan
    .addSubcommand(sub =>
        sub.setName('createplan')
            .setDescription('Create a new subscription plan')
            .addStringOption(opt => opt.setName('product').setDescription('Product Name (e.g. Insight Editor)').setRequired(true))
            .addStringOption(opt => opt.setName('plan_name').setDescription('Plan Name (e.g. 60 Days Access)').setRequired(true))
            .addIntegerOption(opt => opt.setName('days').setDescription('Duration in days').setRequired(true).setMinValue(1))
            .addNumberOption(opt => opt.setName('price').setDescription('Price in INR').setRequired(true).setMinValue(0))
    )
    // 8. statistics
    .addSubcommand(sub =>
        sub.setName('statistics')
            .setDescription('View account and payment statistics')
    )
    // 9. logs
    .addSubcommand(sub =>
        sub.setName('logs')
            .setDescription('View recent administrative audit logs')
            .addIntegerOption(opt => opt.setName('limit').setDescription('Number of logs to fetch (max 25)').setMinValue(1).setMaxValue(25))
    )
    // 10. info
    .addSubcommand(sub =>
        sub.setName('info')
            .setDescription('View administrative panel information and bot configuration')
    )
    // 11. list
    .addSubcommand(sub =>
        sub.setName('list')
            .setDescription('List all designated server administrators')
    )
    // 12. permissions
    .addSubcommand(sub =>
        sub.setName('permissions')
            .setDescription('Check your current administrative privilege flags')
    )
    // 13. activity
    .addSubcommand(sub =>
        sub.setName('activity')
            .setDescription('View recent administrator audit actions')
    );

export async function execute(interaction) {
    if (!isAdministrator(interaction.member)) {
        return interaction.reply({ content: `${EMOJIS.ERROR} You must be an administrator to use this command.`, ephemeral: true });
    }

    const subcommand = interaction.options.getSubcommand();
    const supabase = getSupabase();

    if (subcommand === 'userinfo') {
        const target = interaction.options.getString('target');
        const profile = await findUser(target);

        if (!profile) {
            return interaction.reply({ content: `${EMOJIS.ERROR} No account found matching \`${target}\`.`, ephemeral: true });
        }

        const { data: subscriptions } = await supabase.from('subscriptions').select('*, products(*)').eq('user_id', profile.id).order('expiry_date', { ascending: false });
        const { data: payments } = await supabase.from('payments').select('*').eq('customer_email', profile.email).order('created_at', { ascending: false });

        const embed = createCustomerOverviewEmbed({ profile, subscriptions, payments });
        return interaction.reply({ embeds: [embed], ephemeral: true });
    }

    if (subcommand === 'search') {
        const query = interaction.options.getString('query').trim();
        const { data: results } = await supabase
            .from('profiles')
            .select('username, email, account_status, created_at')
            .or(`username.ilike.%${query}%,email.ilike.%${query}%`)
            .limit(10);

        if (!results || results.length === 0) {
            return interaction.reply({ content: `${EMOJIS.WARNING} No accounts found matching \`${query}\`.`, ephemeral: true });
        }

        const desc = results.map(r => `• ${EMOJIS.USER} **${r.username}** (\`${r.email}\`) — [${r.account_status}]`).join('\n');
        const embed = new EmbedBuilder()
            .setColor(CONFIG.COLORS.INFO)
            .setTitle(`${EMOJIS.ANALYTICS} Search Results for "${query}"`)
            .setDescription(desc);

        return interaction.reply({ embeds: [embed], ephemeral: true });
    }

    if (subcommand === 'activate' || subcommand === 'suspend' || subcommand === 'ban') {
        const target = interaction.options.getString('target');
        const reason = interaction.options.getString('reason') || '';
        const profile = await findUser(target);

        if (!profile) return interaction.reply({ content: `${EMOJIS.ERROR} User not found: \`${target}\``, ephemeral: true });

        const newStatus = subcommand === 'activate' ? 'active' : (subcommand === 'suspend' ? 'suspended' : 'banned');
        await updateUserAccountStatus(interaction.user.id, profile.id, newStatus, reason);

        return interaction.reply({
            content: `${newStatus === 'active' ? EMOJIS.SUCCESS : EMOJIS.WARNING} Successfully set account status of **${profile.username}** to **\`${newStatus.toUpperCase()}\`**.`,
            ephemeral: true
        });
    }

    if (subcommand === 'extend') {
        const target = interaction.options.getString('target');
        const days = interaction.options.getInteger('days');
        const profile = await findUser(target);

        if (!profile) return interaction.reply({ content: `${EMOJIS.ERROR} User not found: \`${target}\``, ephemeral: true });

        try {
            const updated = await extendUserSubscription(interaction.user.id, profile.id, days);
            return interaction.reply({
                content: `${EMOJIS.SUCCESS} Successfully extended **${profile.username}**'s subscription by **${days} days**.\nNew expiry: **${formatDate(updated.expiry_date)}**`,
                ephemeral: true
            });
        } catch (err) {
            return interaction.reply({ content: `${EMOJIS.ERROR} ${err.message}`, ephemeral: true });
        }
    }

    if (subcommand === 'createplan') {
        const prodName = interaction.options.getString('product').trim();
        const planName = interaction.options.getString('plan_name').trim();
        const days = interaction.options.getInteger('days');
        const price = interaction.options.getNumber('price');

        // Find or create product
        let { data: product } = await supabase.from('products').select('*').ilike('product_name', prodName).maybeSingle();
        if (!product) {
            const { data: newProd, error: pErr } = await supabase.from('products').insert({ product_name: prodName }).select().single();
            if (pErr) return interaction.reply({ content: `${EMOJIS.ERROR} Error creating product: ${pErr.message}`, ephemeral: true });
            product = newProd;
        }

        const { error: planErr } = await supabase.from('plans').insert({
            product_id: product.id,
            plan_name: planName,
            duration_days: days,
            price,
            is_active: true
        });

        if (planErr) return interaction.reply({ content: `${EMOJIS.ERROR} Error creating plan: ${planErr.message}`, ephemeral: true });

        return interaction.reply({
            content: `${EMOJIS.SUCCESS} New plan created successfully!\n• Product: **${product.product_name}**\n• Plan: **${planName}**\n• Duration: **${days} Days**\n• Price: **${formatCurrency(price)}**`,
            ephemeral: true
        });
    }

    if (subcommand === 'statistics') {
        const stats = await getSystemStatistics();
        const embed = new EmbedBuilder()
            .setColor(CONFIG.COLORS.PRIMARY)
            .setTitle(`${EMOJIS.ANALYTICS} System Statistics & Performance Overview`)
            .addFields(
                { name: `${EMOJIS.USERS} Total Accounts`, value: `\`${stats.totalAccounts}\``, inline: true },
                { name: `${EMOJIS.VERIFIED} Active Accounts`, value: `\`${stats.activeAccounts}\``, inline: true },
                { name: '🔴 Suspended Accounts', value: `\`${stats.suspendedAccounts}\``, inline: true },
                { name: `${EMOJIS.SUBSCRIPTION} Active Subscriptions`, value: `\`${stats.activeSubs}\``, inline: true },
                { name: `${EMOJIS.EXPIRY} Expired Subscriptions`, value: `\`${stats.expiredSubs}\``, inline: true },
                { name: `${EMOJIS.PAYMENT} Total Payments`, value: `\`${stats.totalPayments}\``, inline: true },
                { name: `${EMOJIS.PENDING} Pending Verifications`, value: `\`${stats.pendingPayments}\``, inline: true },
                { name: `${EMOJIS.MONEY} Verified Revenue`, value: `**${formatCurrency(stats.totalRevenue)}**`, inline: true }
            )
            .setFooter({ text: 'InSightX HQ Core Database Analytics' })
            .setTimestamp();

        return interaction.reply({ embeds: [embed], ephemeral: true });
    }

    if (subcommand === 'logs') {
        const limit = interaction.options.getInteger('limit') || 10;
        const { data: logs } = await supabase
            .from('admin_logs')
            .select('*')
            .order('created_at', { ascending: false })
            .limit(limit);

        if (!logs || logs.length === 0) {
            return interaction.reply({ content: `${EMOJIS.WARNING} No audit logs recorded yet.`, ephemeral: true });
        }

        const desc = logs.map(l => `• \`${formatDate(l.created_at)}\` | **${l.action}** by <@${l.admin_id}>\n  > ${l.description}`).join('\n\n');
        const embed = new EmbedBuilder()
            .setColor(CONFIG.COLORS.DARK)
            .setTitle(`${EMOJIS.LOGS} Recent Admin Audit Logs (Last ${logs.length})`)
            .setDescription(desc);

        return interaction.reply({ embeds: [embed], ephemeral: true });
    }

    if (subcommand === 'info') {
        const embed = new EmbedBuilder()
            .setColor(CONFIG.COLORS.INFO)
            .setTitle(`${EMOJIS.ADMIN} InSightX HQ Administrative Panel`)
            .addFields(
                { name: '🤖 Bot Name', value: '`InSightX HQ Core v2.0`', inline: true },
                { name: `${EMOJIS.OWNER} Admin Role ID`, value: `\`${CONFIG.ADMIN_ROLE_ID || 'Guild Administrator'}\``, inline: true },
                { name: `${EMOJIS.PAYMENT} Payment Log Channel`, value: `\`${CONFIG.PAYMENT_LOG_CHANNEL_ID || 'Not set'}\``, inline: true },
                { name: `${EMOJIS.MONEY} UPI Payee`, value: `\`${CONFIG.MERCHANT_NAME || 'Instagram Pro Tools'}\``, inline: true }
            );
        return interaction.reply({ embeds: [embed], ephemeral: true });
    }

    if (subcommand === 'list') {
        const role = CONFIG.ADMIN_ROLE_ID ? interaction.guild?.roles?.cache?.get(CONFIG.ADMIN_ROLE_ID) : null;
        const members = role?.members?.map(m => `• <@${m.id}> (\`${m.user.tag}\`)`).join('\n') || '• *Server Administrators*';

        const embed = new EmbedBuilder()
            .setColor(CONFIG.COLORS.INFO)
            .setTitle(`${EMOJIS.ADMIN} Authorized Administrators`)
            .setDescription(members);

        return interaction.reply({ embeds: [embed], ephemeral: true });
    }

    if (subcommand === 'permissions') {
        const hasAdmin = interaction.member?.permissions?.has(PermissionFlagsBits.Administrator);
        const hasRole = CONFIG.ADMIN_ROLE_ID && interaction.member?.roles?.cache?.has(CONFIG.ADMIN_ROLE_ID);

        const embed = new EmbedBuilder()
            .setColor(hasAdmin || hasRole ? CONFIG.COLORS.SUCCESS : CONFIG.COLORS.DANGER)
            .setTitle(`${EMOJIS.SECURITY} Admin Privilege Check`)
            .addFields(
                { name: `${EMOJIS.OWNER} Server Administrator Flag`, value: hasAdmin ? `\`GRANTED\` ${EMOJIS.SUCCESS}` : `\`NONE\` ${EMOJIS.ERROR}`, inline: true },
                { name: `${EMOJIS.ADMIN} Admin Role Assigned`, value: hasRole ? `\`GRANTED\` ${EMOJIS.SUCCESS}` : `\`NONE\` ${EMOJIS.ERROR}`, inline: true },
                { name: `${EMOJIS.SYSTEM} InSightX Command Access`, value: hasAdmin || hasRole ? '`FULL ACCESS`' : '`RESTRICTED`', inline: true }
            );

        return interaction.reply({ embeds: [embed], ephemeral: true });
    }

    if (subcommand === 'activity') {
        const { data: recent } = await supabase
            .from('admin_logs')
            .select('*')
            .order('created_at', { ascending: false })
            .limit(10);

        if (!recent || recent.length === 0) return interaction.reply({ content: `${EMOJIS.WARNING} No recent administrator activity.`, ephemeral: true });

        const embed = new EmbedBuilder()
            .setColor(CONFIG.COLORS.INFO)
            .setTitle(`${EMOJIS.LOGS} Recent Admin Activity`)
            .setDescription(recent.map(l => `• \`${l.action}\` by <@${l.admin_id}> — ${l.description}`).join('\n'));

        return interaction.reply({ embeds: [embed], ephemeral: true });
    }
}
