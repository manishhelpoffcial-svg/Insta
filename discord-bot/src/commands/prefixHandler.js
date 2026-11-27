import { ActionRowBuilder, StringSelectMenuBuilder, EmbedBuilder, AttachmentBuilder } from 'discord.js';
import fs from 'node:fs';
import path from 'node:path';
import { getSupabase } from '../database/supabaseClient.js';
import { isAdministrator } from '../interactions/buttons/approve_payment.js';
import { createProductSelectEmbed, createPaymentRejectedEmbed, createUserCredentialsDmEmbed, createPasswordResetDmEmbed } from '../embeds/customerEmbeds.js';
import { createHelpEmbed, buildHelpPayload, createAdminHelpEmbed, buildAdminHelpPayload } from './help.js';
import { buildPlansEmbed } from './plans.js';
import { getUserAccountEmbed } from './account.js';
import { buildCustomerProfileEmbed } from './customer/profile.js';
import { buildSubscriptionEmbed } from './customer/subscription.js';
import { buildUsageEmbed } from './customer/usage.js';
import { buildPaymentHistoryEmbed } from './customer/payments.js';
import { changeUserPassword, linkAccount, unlinkAccount } from './customer/accountAuth.js';
import { createTicket, addTicketMessage } from './customer/support.js';
import { findUser, updateUserAccountStatus, extendUserSubscription, getSystemStatistics } from '../admin/adminActions.js';
import { getOrCreateAccount } from '../accounts/accountManager.js';
import { provisionSubscription } from '../subscriptions/subscriptionManager.js';
import { sendDiscordDm } from '../notifications/notifier.js';
import { sendPaymentRejectedLog } from '../payments/paymentLogger.js';
import { approveAndFulfillPayment } from '../payments/paymentFulfillment.js';
import { createCustomerOverviewEmbed } from '../embeds/adminEmbeds.js';
import { formatCurrency, formatDate } from '../utils/validators.js';
import { processEmojiZip, buildEmojiUploadEmbed } from './admin/uploadEmojis.js';
import { buildAccountListPayload } from './admin/accountList.js';
import { buildAdminDashboardPayload } from '../admin/adminDashboard.js';
import { logUserManagement, logSubscription, logSecurity } from '../utils/channelLogger.js';
import { EMOJIS } from '../utils/emojis.js';
import { CONFIG } from '../../config/constants.js';

function findLatestApk() {
    const candidatePaths = [
        '/sdcard/Download/Instagram.apk',
        '/storage/emulated/0/Download/Instagram.apk',
        '/sdcard/insta/Instagram.apk',
        '/storage/emulated/0/insta/Instagram.apk',
        '/data/user/0/ai.antigem/files/home/.gemini/antigravity-cli/scratch/apk_download/Instagram-Debug-APK/app-debug.apk',
        path.resolve(process.cwd(), 'Instagram.apk'),
        path.resolve(process.cwd(), '../Download/Instagram.apk'),
        path.resolve(process.cwd(), '../insta/Instagram.apk'),
    ];

    let latestFile = null;
    let maxMtime = -1;

    for (const p of candidatePaths) {
        try {
            if (fs.existsSync(p)) {
                const stat = fs.statSync(p);
                if (stat.isFile() && stat.size > 1024 * 1024 && stat.mtimeMs > maxMtime) {
                    maxMtime = stat.mtimeMs;
                    latestFile = p;
                }
            }
        } catch (_) {}
    }
    return latestFile;
}

export async function handlePrefixCommand(message) {
    const rawContent = message.content.trim();
    if (!rawContent.startsWith('.')) return false;

    const withoutPrefix = rawContent.slice(1).trim();
    if (!withoutPrefix) return false;

    const parts = withoutPrefix.split(/\s+/);
    const command = parts[0].toLowerCase();
    const args = parts.slice(1);

    const isAdmin = isAdministrator(message.member) || message.author.id === '1120295079026888715' || (CONFIG.OWNER_ID && message.author.id === CONFIG.OWNER_ID);

    // ==========================================
    // 1. CUSTOMER COMMANDS
    // ==========================================

    // .apk / .app / .getapk
    if (['apk', 'app', 'getapk', 'latestapk'].includes(command)) {
        const apkPath = findLatestApk();
        if (!apkPath) {
            await message.reply(`${EMOJIS.ERROR} No Instagram APK build found on the system. Please ensure the APK has been built.`);
            return true;
        }

        const stat = fs.statSync(apkPath);
        const sizeMb = (stat.size / (1024 * 1024)).toFixed(1);
        const lastUpdated = new Date(stat.mtimeMs).toLocaleString();

        const progressMsg = await message.reply(`${EMOJIS.REFRESH} Uploading latest **Instagram.apk** (${sizeMb} MB)... Please wait a moment.`);

        try {
            const attachment = new AttachmentBuilder(apkPath, { name: 'Instagram.apk' });
            await message.reply({
                content: `${EMOJIS.SUCCESS} **Here is the latest Instagram APK:**\n📂 **File:** \`Instagram.apk\`\n📦 **Size:** \`${sizeMb} MB\`\n⏱️ **Updated:** \`${lastUpdated}\``,
                files: [attachment]
            });
            await progressMsg.delete().catch(() => {});
        } catch (err) {
            console.error('[APK Command Error]', err);
            if (err.code === 40005 || err.message?.includes('Request entity too large') || err.message?.includes('too large')) {
                await progressMsg.edit({
                    content: `${EMOJIS.WARNING} **Discord File Size Limit Exceeded**\n\nThe APK size is **${sizeMb} MB**, which exceeds Discord's attachment limit for this channel/DM (standard servers allow up to 25 MB).\n\n💡 **Options to receive the direct file:**\n1. Boost this server to Level 3 (100 MB limit) to receive direct 60 MB file uploads.\n2. Or access the file directly on device storage at: \`/sdcard/Download/Instagram.apk\``
                }).catch(() => {});
            } else {
                await progressMsg.edit({
                    content: `${EMOJIS.ERROR} Failed to attach APK: ${err.message}`
                }).catch(() => {});
            }
        }
        return true;
    }

    // .help / .commands
    if (command === 'help' || command === 'commands') {
        await message.reply(buildHelpPayload('overview'));
        return true;
    }

    // .adhelp / .adminhelp / .ahelp
    if (command === 'adhelp' || command === 'adminhelp' || command === 'ahelp') {
        if (!isAdmin) {
            await message.reply(`${EMOJIS.ERROR} Administrator access required.`);
            return true;
        }
        await message.reply(buildAdminHelpPayload('overview'));
        return true;
    }

    // .ping
    if (command === 'ping') {
        const start = Date.now();
        const sent = await message.reply(`${EMOJIS.REFRESH} Calculating network latency...`);
        const latency = Date.now() - start;

        const embed = new EmbedBuilder()
            .setColor(CONFIG.COLORS.SUCCESS)
            .setTitle(`${EMOJIS.SYSTEM} InSightX HQ — Ping Telemetry`)
            .addFields(
                { name: `${EMOJIS.SERVER} Message Latency`, value: `\`${latency}ms\``, inline: true },
                { name: `${EMOJIS.API} WebSocket Gateway`, value: `\`${message.client.ws.ping}ms\``, inline: true }
            );

        await sent.edit({ content: null, embeds: [embed] });
        return true;
    }

    // .profile / .myaccount / .dashboard / .account / .me
    if (['profile', 'myaccount', 'dashboard', 'account', 'me'].includes(command)) {
        const embed = await buildCustomerProfileEmbed(message.author.id);
        await message.reply({ embeds: [embed] });
        return true;
    }

    // .subscription / .expiry
    if (command === 'subscription' || command === 'expiry') {
        if (args.length === 0 || !isAdmin) {
            const embed = await buildSubscriptionEmbed(message.author.id);
            await message.reply({ embeds: [embed] });
            return true;
        }
    }

    // .myusage / .mylimit
    if (command === 'myusage' || command === 'mylimit') {
        const embed = await buildUsageEmbed(message.author.id);
        await message.reply({ embeds: [embed] });
        return true;
    }

    // .payment-history / .payment-status
    if (command === 'payment-history' || command === 'payment-status') {
        const embed = await buildPaymentHistoryEmbed(message.author.id);
        await message.reply({ embeds: [embed] });
        return true;
    }

    // .plans / .pricing
    if (command === 'plans' || command === 'pricing') {
        const embed = await buildPlansEmbed();
        await message.reply({ embeds: [embed] });
        return true;
    }

    // .purchase / .buy
    if (command === 'purchase' || command === 'buy') {
        const supabase = getSupabase();
        const { data: products, error } = await supabase.from('products').select('*').eq('is_active', true);

        if (error || !products || products.length === 0) {
            await message.reply(`${EMOJIS.ERROR} No active products are currently available in the catalog.`);
            return true;
        }

        const embed = createProductSelectEmbed();
        const selectMenu = new StringSelectMenuBuilder()
            .setCustomId('select_product')
            .setPlaceholder('👉 Choose a product to purchase...')
            .addOptions(
                products.map(p => ({
                    label: p.product_name,
                    description: p.description ? p.description.slice(0, 100) : 'Full creator tools access',
                    value: p.id,
                    emoji: '1574679528972746842'
                }))
            );

        const row = new ActionRowBuilder().addComponents(selectMenu);
        await message.reply({ embeds: [embed], components: [row] });
        return true;
    }

    // .change-password <new_password>
    if (command === 'change-password') {
        const newPass = args[0];
        if (!newPass) return message.reply(`${EMOJIS.WARNING} **Usage:** \`.change-password <new_password_min_6_chars>\``);
        try {
            const profile = await changeUserPassword(message.author.id, newPass);
            await message.reply(`${EMOJIS.SUCCESS} Password for \`@${profile.username}\` updated successfully!`);
        } catch (err) {
            await message.reply(`${EMOJIS.ERROR} ${err.message}`);
        }
        return true;
    }

    // .link-account <username>
    if (command === 'link-account') {
        const username = args[0];
        if (!username) return message.reply(`${EMOJIS.WARNING} **Usage:** \`.link-account <username>\``);
        try {
            const profile = await linkAccount(message.author.id, username);
            await message.reply(`${EMOJIS.VERIFIED} Account \`@${profile.username}\` is now securely linked to your Discord profile!`);
        } catch (err) {
            await message.reply(`${EMOJIS.ERROR} ${err.message}`);
        }
        return true;
    }

    // .unlink-account
    if (command === 'unlink-account') {
        try {
            const profile = await unlinkAccount(message.author.id);
            await message.reply(`${EMOJIS.SUCCESS} Account \`@${profile.username}\` unlinked from Discord.`);
        } catch (err) {
            await message.reply(`${EMOJIS.ERROR} ${err.message}`);
        }
        return true;
    }

    // .forgot-password
    if (command === 'forgot-password') {
        await message.reply(`${EMOJIS.SECURITY} Credentials & resets are managed directly in Discord. Use \`.change-password <new_password>\` or reach out via \`.support create\`.`);
        return true;
    }

    // .support
    if (command === 'support') {
        const subAction = (args[0] || '').toLowerCase();
        const supabase = getSupabase();

        if (subAction === 'create') {
            const subject = args.slice(1).join(' ').trim();
            if (!subject) return message.reply(`${EMOJIS.WARNING} **Usage:** \`.support create <description of issue>\``);
            const ticket = await createTicket(message.author.id, subject);
            return message.reply(`${EMOJIS.TICKET} Support Ticket **#${ticket.ticket_number}** created! Our team will review it.`);
        }

        if (subAction === 'reply') {
            const num = parseInt(args[1], 10);
            const text = args.slice(2).join(' ').trim();
            if (isNaN(num) || !text) return message.reply(`${EMOJIS.WARNING} **Usage:** \`.support reply <ticket_number> <message>\``);
            await addTicketMessage(num, message.author.id, message.author.username, isAdmin, text);
            return message.reply(`${EMOJIS.SUCCESS} Reply added to Ticket **#${num}**.`);
        }

        if (subAction === 'close') {
            const num = parseInt(args[1], 10);
            if (isNaN(num)) return message.reply(`${EMOJIS.WARNING} **Usage:** \`.support close <ticket_number>\``);
            await supabase.from('support_tickets').update({ status: 'closed', updated_at: new Date().toISOString() }).eq('ticket_number', num);
            return message.reply(`${EMOJIS.SUCCESS} Ticket **#${num}** closed.`);
        }

        if (subAction === 'reopen') {
            const num = parseInt(args[1], 10);
            if (isNaN(num)) return message.reply(`${EMOJIS.WARNING} **Usage:** \`.support reopen <ticket_number>\``);
            await supabase.from('support_tickets').update({ status: 'reopened', updated_at: new Date().toISOString() }).eq('ticket_number', num);
            return message.reply(`${EMOJIS.REFRESH} Ticket **#${num}** reopened.`);
        }

        if (subAction === 'list') {
            let q = supabase.from('support_tickets').select('*').order('created_at', { ascending: false }).limit(10);
            if (!isAdmin) q = q.eq('discord_user_id', message.author.id);
            const { data: tickets } = await q;

            if (!tickets || tickets.length === 0) return message.reply(`${EMOJIS.SUPPORT} No support tickets found.`);
            const embed = new EmbedBuilder()
                .setColor(CONFIG.COLORS.INFO)
                .setTitle(`${EMOJIS.SUPPORT} Support Tickets Registry`)
                .setDescription(tickets.map(t => `${EMOJIS.TICKET} **#${t.ticket_number}** — \`${t.status}\` — ${t.subject}`).join('\n'));
            return message.reply({ embeds: [embed] });
        }

        return message.reply(`${EMOJIS.SUPPORT} **Support Commands:**\n• \`.support create <subject>\`\n• \`.support reply <ticket_id> <msg>\`\n• \`.support close <ticket_id>\`\n• \`.support list\``);
    }

    // ==========================================
    // 2. ADMINISTRATOR & OWNER COMMANDS
    // ==========================================

    // .dashboard / .admindashboard
    if (['dashboard', 'admindashboard'].includes(command)) {
        if (!isAdmin) return message.reply(`${EMOJIS.ERROR} Administrator access required.`);
        const payload = await buildAdminDashboardPayload();
        await message.reply(payload);
        return true;
    }

    // .account-list / .accounts / .accountlist [page]
    if (['account-list', 'accounts', 'accountlist'].includes(command)) {
        if (!isAdmin) return message.reply(`${EMOJIS.ERROR} Administrator access required.`);
        const page = parseInt(args[0], 10) || 1;
        const payload = await buildAccountListPayload(page);
        await message.reply(payload);
        return true;
    }

    // .createuser shortcut
    if (command === 'createuser') {
        if (!isAdmin) return message.reply(`${EMOJIS.ERROR} Administrator access required.`);
        args = ['create', ...args];
        command = 'user';
    }

    // .user <subcommand>
    if (command === 'user') {
        if (!isAdmin) return message.reply(`${EMOJIS.ERROR} Administrator access required.`);
        const sub = (args[0] || '').toLowerCase();
        const target = args[1];
        const supabase = getSupabase();

        if (sub === 'list' || sub === 'accounts') {
            const page = parseInt(args[1], 10) || 1;
            const payload = await buildAccountListPayload(page);
            await message.reply(payload);
            return true;
        }

        if (sub === 'unlock-device' || sub === 'reset-device') {
            if (!target) return message.reply(`${EMOJIS.WARNING} **Usage:** \`.user unlock-device <username | discord_id | uuid>\``);
            const profile = await findUser(target);
            if (!profile) return message.reply(`${EMOJIS.ERROR} User not found: \`${target}\``);

            await supabase.from('device_sessions').delete().eq('user_id', profile.id);
            await supabase.from('profiles').update({ is_device_locked: false, locked_device_id: null, locked_at: null }).eq('id', profile.id);

            await logSecurity(message.client, new EmbedBuilder()
                .setColor(CONFIG.COLORS.WARNING)
                .setTitle(`${EMOJIS.SECURITY} Hardware Device Lock Cleared`)
                .setDescription(`Admin <@${message.author.id}> cleared hardware device lock for **@${profile.username}**.`)
                .setTimestamp()
            );

            return message.reply(`${EMOJIS.SUCCESS} Device lock cleared for \`@${profile.username}\`. The user can now log in once on a new device.`);
        }

        if (sub === 'create') {
            const username = args[1];
            if (!username) return message.reply(`${EMOJIS.WARNING} **Usage:** \`.user create <username> [password] [duration] [@user/discord_id]\``);

            let pass = null;
            let durationDays = 30;
            let targetDiscordUser = message.mentions.users.first() || null;

            // Parse remaining arguments for password, duration, and target user
            const remainingArgs = args.slice(2);
            for (const arg of remainingArgs) {
                if (arg.startsWith('<@') || /^\d{17,20}$/.test(arg)) {
                    if (!targetDiscordUser && /^\d{17,20}$/.test(arg)) {
                        targetDiscordUser = await message.client.users.fetch(arg).catch(() => null);
                    }
                } else if (/^(\d+)[dD]?$/.test(arg) || arg.toLowerCase() === 'lifetime') {
                    durationDays = arg.toLowerCase() === 'lifetime' ? 36500 : parseInt(arg, 10);
                } else if (!pass) {
                    pass = arg;
                }
            }

            const { profile, plainPassword } = await getOrCreateAccount({
                username,
                password: pass,
                discordUserId: targetDiscordUser ? targetDiscordUser.id : null
            });

            // Automatically provision subscription
            const { subscription } = await provisionSubscription({
                userId: profile.id,
                durationDays
            });

            let dmStatus = '';
            if (targetDiscordUser) {
                const dmEmbed = createUserCredentialsDmEmbed({
                    username: profile.username,
                    password: plainPassword,
                    createdBy: message.author.tag,
                    planName: subscription?.plans?.plan_name || `${durationDays} Days Plan`,
                    expiryDate: subscription?.expiry_date
                });
                const sent = await sendDiscordDm(message.client, targetDiscordUser.id, dmEmbed);
                dmStatus = sent
                    ? `\n📬 **Auto DM:** Login credentials sent directly to <@${targetDiscordUser.id}>!`
                    : `\n⚠️ **Auto DM:** Could not DM <@${targetDiscordUser.id}> (their direct messages may be closed).`;
            }

            // Log User Management
            await logUserManagement(message.client, new EmbedBuilder()
                .setColor(CONFIG.COLORS.SUCCESS)
                .setTitle(`${EMOJIS.USER_ADD} Account Provisioned via Prefix`)
                .addFields(
                    { name: 'Username', value: `@${profile.username}`, inline: true },
                    { name: 'UUID', value: `\`${profile.id}\``, inline: true },
                    { name: 'Discord', value: targetDiscordUser ? `<@${targetDiscordUser.id}>` : '*Unlinked*', inline: true },
                    { name: 'Validity', value: `${durationDays} Days`, inline: true },
                    { name: 'Expiry Date', value: subscription ? formatDate(subscription.expiry_date) : '*N/A*', inline: true }
                )
                .setTimestamp()
            );

            // Log Subscription
            await logSubscription(message.client, new EmbedBuilder()
                .setColor(CONFIG.COLORS.PLAN)
                .setTitle(`${EMOJIS.PLAN} Subscription Provisioned`)
                .setDescription(`Admin <@${message.author.id}> provisioned **${durationDays} days** for user **@${profile.username}**.`)
                .setTimestamp()
            );

            return message.reply(
                `${EMOJIS.USER_ADD} **User Account Created!**\n` +
                `• **Username:** ||${profile.username}||\n` +
                `• **Password:** ||${plainPassword}||\n` +
                `• **Validity:** **${durationDays} Days** (${formatDate(subscription?.expiry_date)})\n` +
                `• **Linked Discord:** ${targetDiscordUser ? `<@${targetDiscordUser.id}>` : '`Unlinked`'}${dmStatus}`
            );
        }

        if (sub === 'info') {
            if (!target) return message.reply(`${EMOJIS.WARNING} **Usage:** \`.user info <username | discord_id>\``);
            const profile = await findUser(target);
            if (!profile) return message.reply(`${EMOJIS.ERROR} User not found: \`${target}\``);
            const embed = new EmbedBuilder()
                .setColor(CONFIG.COLORS.INFO)
                .setTitle(`${EMOJIS.PROFILE} User Profile: @${profile.username}`)
                .addFields(
                    { name: 'UUID', value: `\`${profile.id}\``, inline: false },
                    { name: 'Account Status', value: `\`${profile.account_status.toUpperCase()}\``, inline: true },
                    { name: 'Discord', value: profile.discord_user_id ? `<@${profile.discord_user_id}>` : '`None`', inline: true }
                );
            return message.reply({ embeds: [embed] });
        }

        if (sub === 'ban' || sub === 'unban' || sub === 'suspend' || sub === 'unsuspend') {
            if (!target) return message.reply(`${EMOJIS.WARNING} **Usage:** \`.user ${sub} <target>\``);
            const profile = await findUser(target);
            if (!profile) return message.reply(`${EMOJIS.ERROR} User not found: \`${target}\``);
            const newStatus = (sub === 'unban' || sub === 'unsuspend') ? 'active' : (sub === 'ban' ? 'banned' : 'suspended');
            await updateUserAccountStatus(message.author.id, profile.id, newStatus);
            return message.reply(`${newStatus === 'active' ? EMOJIS.SUCCESS : EMOJIS.WARNING} Account \`@${profile.username}\` set to **${newStatus.toUpperCase()}**.`);
        }

        if (sub === 'reset-password') {
            if (!target) return message.reply(`${EMOJIS.WARNING} **Usage:** \`.user reset-password <target> [new_pass]\``);
            const profile = await findUser(target);
            if (!profile) return message.reply(`${EMOJIS.ERROR} User not found: \`${target}\``);
            const newPass = args[2] || `Pass_${Math.random().toString(36).slice(-6)}!`;
            await supabase.auth.admin.updateUserById(profile.id, { password: newPass });

            let dmStatus = '';
            if (profile.discord_user_id) {
                const dmEmbed = createPasswordResetDmEmbed({
                    username: profile.username,
                    newPassword: newPass,
                    adminTag: message.author.tag
                });
                const sent = await sendDiscordDm(message.client, profile.discord_user_id, dmEmbed);
                dmStatus = sent
                    ? `\n📬 **Auto DM:** New password sent directly to <@${profile.discord_user_id}>!`
                    : `\n⚠️ **Auto DM:** Could not DM <@${profile.discord_user_id}> (user DMs may be closed).`;
            } else {
                dmStatus = '\nℹ️ **Auto DM:** No Discord account linked to this user.';
            }

            await logSecurity(message.client, new EmbedBuilder()
                .setColor(CONFIG.COLORS.WARNING)
                .setTitle(`${EMOJIS.SECURITY} Password Reset by Admin`)
                .setDescription(`Admin <@${message.author.id}> reset password for **@${profile.username}**.`)
                .addFields(
                    { name: 'User', value: `@${profile.username}`, inline: true },
                    { name: 'Discord', value: profile.discord_user_id ? `<@${profile.discord_user_id}>` : '*Not linked*', inline: true },
                    { name: 'Admin', value: `<@${message.author.id}>`, inline: true }
                )
                .setTimestamp()
            );

            return message.reply(
                `${EMOJIS.SECURITY} **Password Reset Successful!**\n` +
                `• **User:** \`@${profile.username}\`\n` +
                `• **New Password:** ||${newPass}||${dmStatus}`
            );
        }

        if (sub === 'delete') {
            if (!target) return message.reply(`${EMOJIS.WARNING} **Usage:** \`.user delete <target>\``);
            const profile = await findUser(target);
            if (!profile) return message.reply(`${EMOJIS.ERROR} User not found: \`${target}\``);
            await supabase.from('profiles').delete().eq('id', profile.id);
            await supabase.auth.admin.deleteUser(profile.id).catch(() => {});
            return message.reply(`${EMOJIS.USER_REMOVE} Account \`@${profile.username}\` deleted.`);
        }

        return message.reply(`${EMOJIS.USERS} **Usage:** \`.user create\` | \`.user info\` | \`.user ban\` | \`.user suspend\` | \`.user reset-password\` | \`.user delete\``);
    }

    // .subscription <subcommand>
    if (command === 'subscription' && isAdmin && args.length > 0) {
        const sub = (args[0] || '').toLowerCase();
        const target = args[1];
        const supabase = getSupabase();

        if (sub === 'extend' || sub === 'add') {
            const days = parseInt(args[2], 10);
            if (!target || isNaN(days)) return message.reply(`${EMOJIS.WARNING} **Usage:** \`.subscription extend <target> <days>\``);
            const profile = await findUser(target);
            if (!profile) return message.reply(`${EMOJIS.ERROR} User not found: \`${target}\``);
            const updated = await extendUserSubscription(message.author.id, profile.id, days);
            return message.reply(`${EMOJIS.SUCCESS} Subscription extended by **${days} days**. New expiry: **${formatDate(updated.expiry_date)}**`);
        }

        if (sub === 'expire') {
            if (!target) return message.reply(`${EMOJIS.WARNING} **Usage:** \`.subscription expire <target>\``);
            const profile = await findUser(target);
            if (!profile) return message.reply(`${EMOJIS.ERROR} User not found: \`${target}\``);
            await supabase.from('subscriptions').update({ subscription_status: 'expired', expiry_date: new Date().toISOString() }).eq('user_id', profile.id);
            return message.reply(`${EMOJIS.EXPIRY} Subscription for \`@${profile.username}\` marked as expired.`);
        }

        if (sub === 'list' || sub === 'expired' || sub === 'expiring') {
            let q = supabase.from('subscriptions').select('*, profiles(username), plans(plan_name)').limit(10);
            if (sub === 'list') q = q.eq('subscription_status', 'active');
            if (sub === 'expired') q = q.eq('subscription_status', 'expired');
            const { data: subs } = await q;

            if (!subs || subs.length === 0) return message.reply(`${EMOJIS.SUBSCRIPTION} No matching subscriptions.`);
            const embed = new EmbedBuilder()
                .setColor(CONFIG.COLORS.INFO)
                .setTitle(`${EMOJIS.SUBSCRIPTION} Subscriptions (${sub.toUpperCase()})`)
                .setDescription(subs.map(s => `${EMOJIS.PLAN} \`@${s.profiles?.username}\` — **${s.plans?.plan_name}** — ${formatDate(s.expiry_date)}`).join('\n'));
            return message.reply({ embeds: [embed] });
        }
    }

    // .payment <subcommand>
    if (command === 'payment') {
        if (!isAdmin) return message.reply(`${EMOJIS.ERROR} Administrator access required.`);
        const sub = (args[0] || '').toLowerCase();
        const ref = (args[1] || '').toUpperCase();
        const supabase = getSupabase();

        if (sub === 'pending') {
            const { data: list } = await supabase.from('payments').select('*, plans(*)').in('payment_status', ['pending_review', 'pending']).limit(10);
            if (!list || list.length === 0) return message.reply(`${EMOJIS.SUCCESS} No pending payments in review queue.`);
            const embed = new EmbedBuilder().setColor(CONFIG.COLORS.WARNING).setTitle(`${EMOJIS.PENDING} Payments Pending Review`)
                .setDescription(list.map(p => `${EMOJIS.TRANSACTION} \`${p.payment_reference}\` — ${formatCurrency(p.amount)} — <@${p.discord_user_id}>`).join('\n'));
            return message.reply({ embeds: [embed] });
        }

        if (sub === 'approve') {
            if (!ref) return message.reply(`${EMOJIS.WARNING} **Usage:** \`.payment approve <payment_reference>\``);
            const { data: payment } = await supabase.from('payments').select('*, products(*), plans(*)').eq('payment_reference', ref).single();
            if (!payment) return message.reply(`${EMOJIS.ERROR} Payment ref \`${ref}\` not found.`);

            await approveAndFulfillPayment({
                client: message.client,
                payment,
                adminId: message.author.id
            });

            return message.reply(`${EMOJIS.APPROVE} Payment \`${ref}\` approved! Account credentials sent in Discord DM.`);
        }

        if (sub === 'reject') {
            if (!ref) return message.reply(`${EMOJIS.WARNING} **Usage:** \`.payment reject <payment_reference> [reason]\``);
            const reason = args.slice(2).join(' ') || 'Verification unconfirmed';
            const { data: rejectedPayment } = await supabase
                .from('payments')
                .update({ payment_status: 'rejected', rejection_reason: reason, reviewed_by: message.author.id, reviewed_at: new Date().toISOString() })
                .eq('payment_reference', ref)
                .select()
                .maybeSingle();
            if (rejectedPayment) {
                await sendDiscordDm(
                    message.client,
                    rejectedPayment.discord_user_id,
                    createPaymentRejectedEmbed({ reference: rejectedPayment.payment_reference, reason })
                );
                await sendPaymentRejectedLog(message.client, {
                    payment: rejectedPayment,
                    reason,
                    adminId: message.author.id
                });
            }
            return message.reply(`${EMOJIS.REJECT} Payment \`${ref}\` rejected.`);
        }

        if (sub === 'refund') {
            if (!ref) return message.reply(`${EMOJIS.WARNING} **Usage:** \`.payment refund <payment_reference>\``);
            const { data: p } = await supabase.from('payments').select('*').eq('payment_reference', ref).single();
            if (!p) return message.reply(`${EMOJIS.ERROR} Payment ref \`${ref}\` not found.`);
            await supabase.from('payments').update({ payment_status: 'refunded' }).eq('id', p.id);
            if (p.user_id) await supabase.from('subscriptions').update({ subscription_status: 'cancelled', expiry_date: new Date().toISOString() }).eq('user_id', p.user_id);
            return message.reply(`${EMOJIS.REFUND} Payment \`${ref}\` refunded and access cancelled.`);
        }

        if (sub === 'history' || sub === 'accepted' || sub === 'rejected') {
            let q = supabase.from('payments').select('*, plans(*)').order('created_at', { ascending: false }).limit(10);
            if (sub === 'accepted') q = q.eq('payment_status', 'approved');
            if (sub === 'rejected') q = q.eq('payment_status', 'rejected');
            const { data: list } = await q;

            if (!list || list.length === 0) return message.reply(`${EMOJIS.PAYMENT} No payments recorded.`);
            const embed = new EmbedBuilder().setColor(CONFIG.COLORS.INFO).setTitle(`${EMOJIS.PAYMENT} Payments (${sub.toUpperCase()})`)
                .setDescription(list.map(p => `${EMOJIS.TRANSACTION} \`${p.payment_reference}\` — ${formatCurrency(p.amount)} — \`${p.payment_status}\``).join('\n'));
            return message.reply({ embeds: [embed] });
        }
    }

    // .usage <subcommand>
    if (command === 'usage') {
        if (!isAdmin) return message.reply(`${EMOJIS.ERROR} Administrator access required.`);
        const sub = (args[0] || '').toLowerCase();
        const target = args[1];
        const supabase = getSupabase();

        if (sub === 'leaderboard') {
            const { data: list } = await supabase.from('user_usage').select('*, profiles(username)').order('monthly_count', { ascending: false }).limit(10);
            if (!list || list.length === 0) return message.reply(`${EMOJIS.USAGE} No usage records found.`);
            const embed = new EmbedBuilder().setColor(CONFIG.COLORS.PRIMARY).setTitle(`${EMOJIS.ANALYTICS} Top Action Leaderboard`)
                .setDescription(list.map((u, i) => `**${i + 1}.** \`@${u.profiles?.username}\` — **${u.monthly_count}** actions`).join('\n'));
            return message.reply({ embeds: [embed] });
        }

        if (sub === 'reset') {
            if (!target) return message.reply(`${EMOJIS.WARNING} **Usage:** \`.usage reset <target>\``);
            const profile = await findUser(target);
            if (!profile) return message.reply(`${EMOJIS.ERROR} User not found: \`${target}\``);
            await supabase.from('user_usage').upsert({ user_id: profile.id, daily_count: 0 });
            return message.reply(`${EMOJIS.REFRESH} Usage counter reset for \`@${profile.username}\`.`);
        }

        if (sub === 'set-limit') {
            const limit = parseInt(args[2], 10);
            if (!target || isNaN(limit)) return message.reply(`${EMOJIS.WARNING} **Usage:** \`.usage set-limit <target> <limit>\``);
            const profile = await findUser(target);
            if (!profile) return message.reply(`${EMOJIS.ERROR} User not found: \`${target}\``);
            await supabase.from('user_usage').upsert({ user_id: profile.id, custom_limit: limit });
            return message.reply(`${EMOJIS.LIMIT} Limit for \`@${profile.username}\` set to **${limit} actions**.`);
        }
    }

    // .owner <subcommand>
    if (command === 'owner') {
        if (!isAdmin) return message.reply(`${EMOJIS.ERROR} Owner access required.`);
        const sub = (args[0] || '').toLowerCase();
        const supabase = getSupabase();

        if (sub === 'setup') {
            const embed = new EmbedBuilder().setColor(CONFIG.COLORS.SUCCESS).setTitle(`${EMOJIS.OWNER} InSightX HQ Setup Status`)
                .addFields(
                    { name: 'Database', value: '`ONLINE`', inline: true },
                    { name: 'Plans', value: '`6 Standard Plans (₹199 to ₹1,999)`', inline: true },
                    { name: 'Auth', value: '`Supabase Direct Credentials`', inline: true }
                );
            return message.reply({ embeds: [embed] });
        }

        if (sub === 'maintenance') {
            const mode = args[1] === 'true' || args[1] === 'on' || args[1] === '1';
            await supabase.from('bot_config').upsert({ key: 'maintenance_mode', value: mode });
            return message.reply(`${EMOJIS.SETTINGS} Maintenance mode: **${mode ? 'ACTIVE' : 'OFF'}**.`);
        }

        if (sub === 'broadcast') {
            const msg = args.slice(1).join(' ');
            if (!msg) return message.reply(`${EMOJIS.WARNING} **Usage:** \`.owner broadcast <message>\``);
            const embed = new EmbedBuilder().setColor(CONFIG.COLORS.PRIMARY).setTitle(`${EMOJIS.OWNER} InSightX Broadcast`).setDescription(msg);
            await message.channel.send({ embeds: [embed] });
            return message.reply(`${EMOJIS.SUCCESS} Broadcast posted.`);
        }

        if (sub === 'set-prefix') {
            const p = args[1];
            if (!p) return message.reply(`${EMOJIS.WARNING} **Usage:** \`.owner set-prefix <prefix>\``);
            await supabase.from('bot_config').upsert({ key: 'prefix', value: p });
            return message.reply(`${EMOJIS.SUCCESS} Global prefix updated to \`${p}\``);
        }
    }

    // .system <subcommand>
    if (command === 'system') {
        if (!isAdmin) return message.reply(`${EMOJIS.ERROR} Administrator access required.`);
        const sub = (args[0] || '').toLowerCase();
        const stats = await getSystemStatistics();

        if (sub === 'status' || sub === 'stats') {
            const embed = new EmbedBuilder().setColor(CONFIG.COLORS.PRIMARY).setTitle(`${EMOJIS.SYSTEM} InSightX HQ Telemetry`)
                .addFields(
                    { name: `${EMOJIS.USERS} Users`, value: `Total: \`${stats.totalAccounts}\` | Active: \`${stats.activeAccounts}\``, inline: true },
                    { name: `${EMOJIS.SUBSCRIPTION} Subs`, value: `Active: \`${stats.activeSubs}\` | Expired: \`${stats.expiredSubs}\``, inline: true },
                    { name: `${EMOJIS.MONEY} Revenue`, value: `**${formatCurrency(stats.totalRevenue)}**`, inline: true },
                    { name: `${EMOJIS.SERVER} Uptime`, value: `\`${Math.floor(process.uptime() / 60)}m\``, inline: true }
                );
            return message.reply({ embeds: [embed] });
        }

        if (sub === 'uptime') {
            return message.reply(`${EMOJIS.SERVER} Bot Uptime: **${Math.floor(process.uptime() / 60)} minutes**`);
        }
    }

    // .uploademojis / .emojizip [prefix]
    if (command === 'uploademojis' || command === 'emojizip') {
        if (!isAdmin) return message.reply(`${EMOJIS.ERROR} Administrator or Owner access required.`);
        if (!message.guild) return message.reply(`${EMOJIS.ERROR} This command must be executed within a Discord server.`);

        const attachment = message.attachments.find(a => a.name?.toLowerCase().endsWith('.zip'));
        if (!attachment) {
            return message.reply(`${EMOJIS.WARNING} Please attach a \`.zip\` archive containing image files (PNG, JPG, WEBP, GIF) with your command:\n\`.uploademojis [optional_prefix]\``);
        }

        const prefix = (args[0] || '').trim();
        const progressMsg = await message.reply(`${EMOJIS.REFRESH} Downloading and preparing emoji ZIP archive...`);

        try {
            const results = await processEmojiZip({
                guild: message.guild,
                attachmentUrl: attachment.url,
                attachmentName: attachment.name,
                prefix,
                progressCallback: async (text) => {
                    await progressMsg.edit(text).catch(() => {});
                }
            });

            const embed = buildEmojiUploadEmbed({
                totalFound: results.totalFound,
                uploaded: results.uploaded,
                failed: results.failed,
                guildName: message.guild.name
            });

            await progressMsg.edit({ content: null, embeds: [embed] });
        } catch (err) {
            await progressMsg.edit({ content: `${EMOJIS.ERROR} Error processing emoji zip: ${err.message}` });
        }
        return true;
    }

    // .admin legacy / alias
    if (command === 'admin') {
        const sub = (args[0] || '').toLowerCase();
        if (sub === 'stats') {
            const stats = await getSystemStatistics();
            const embed = new EmbedBuilder().setColor(CONFIG.COLORS.PRIMARY).setTitle(`${EMOJIS.ADMIN} InSightX HQ Platform Metrics`)
                .addFields(
                    { name: `${EMOJIS.USERS} Total Accounts`, value: `\`${stats.totalAccounts}\``, inline: true },
                    { name: `${EMOJIS.VERIFIED} Active Accounts`, value: `\`${stats.activeAccounts}\``, inline: true },
                    { name: `${EMOJIS.SUBSCRIPTION} Active Subscriptions`, value: `\`${stats.activeSubs}\``, inline: true },
                    { name: `${EMOJIS.MONEY} Total Revenue`, value: `**${formatCurrency(stats.totalRevenue)}**`, inline: true }
                );
            return message.reply({ embeds: [embed] });
        }
    }

    return false;
}
