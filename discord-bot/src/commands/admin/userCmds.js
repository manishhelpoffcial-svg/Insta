import { SlashCommandBuilder, EmbedBuilder } from 'discord.js';
import { getSupabase } from '../../database/supabaseClient.js';
import { CONFIG } from '../../../config/constants.js';
import { isAdministrator } from '../../interactions/buttons/approve_payment.js';
import { findUser, updateUserAccountStatus } from '../../admin/adminActions.js';
import { getOrCreateAccount } from '../../accounts/accountManager.js';
import { formatDate } from '../../utils/validators.js';
import { EMOJIS } from '../../utils/emojis.js';

export const data = new SlashCommandBuilder()
    .setName('user')
    .setDescription('Manage user accounts, credentials, and access permissions')
    // 1. create
    .addSubcommand(sub =>
        sub.setName('create')
            .setDescription('Create a new user account with instant credentials')
            .addStringOption(opt => opt.setName('username').setDescription('Desired unique username').setRequired(true))
            .addStringOption(opt => opt.setName('password').setDescription('Optional password').setRequired(false))
            .addUserOption(opt => opt.setName('discord_user').setDescription('Optional linked Discord user').setRequired(false))
    )
    // 2. info
    .addSubcommand(sub =>
        sub.setName('info')
            .setDescription('View detailed account records for a user')
            .addStringOption(opt => opt.setName('target').setDescription('Username, Discord ID, or UUID').setRequired(true))
    )
    // 3. search
    .addSubcommand(sub =>
        sub.setName('search')
            .setDescription('Search user accounts by keyword')
            .addStringOption(opt => opt.setName('query').setDescription('Username or ID keyword').setRequired(true))
    )
    // 4. edit
    .addSubcommand(sub =>
        sub.setName('edit')
            .setDescription('Edit user account details')
            .addStringOption(opt => opt.setName('target').setDescription('Current username or Discord ID').setRequired(true))
            .addStringOption(opt => opt.setName('new_username').setDescription('New username').setRequired(false))
            .addStringOption(opt => opt.setName('status').setDescription('Account status').addChoices(
                { name: 'Active', value: 'active' },
                { name: 'Suspended', value: 'suspended' },
                { name: 'Banned', value: 'banned' },
                { name: 'Inactive', value: 'inactive' }
            ).setRequired(false))
    )
    // 5. ban
    .addSubcommand(sub =>
        sub.setName('ban')
            .setDescription('Permanently ban an account')
            .addStringOption(opt => opt.setName('target').setDescription('Username or Discord ID').setRequired(true))
            .addStringOption(opt => opt.setName('reason').setDescription('Ban reason').setRequired(false))
    )
    // 6. unban
    .addSubcommand(sub =>
        sub.setName('unban')
            .setDescription('Unban and restore an account to active status')
            .addStringOption(opt => opt.setName('target').setDescription('Username or Discord ID').setRequired(true))
    )
    // 7. suspend
    .addSubcommand(sub =>
        sub.setName('suspend')
            .setDescription('Temporarily suspend an account')
            .addStringOption(opt => opt.setName('target').setDescription('Username or Discord ID').setRequired(true))
            .addStringOption(opt => opt.setName('reason').setDescription('Suspension reason').setRequired(false))
    )
    // 8. unsuspend
    .addSubcommand(sub =>
        sub.setName('unsuspend')
            .setDescription('Unsuspend and restore an account to active status')
            .addStringOption(opt => opt.setName('target').setDescription('Username or Discord ID').setRequired(true))
    )
    // 9. delete
    .addSubcommand(sub =>
        sub.setName('delete')
            .setDescription('Completely delete user profile and auth records')
            .addStringOption(opt => opt.setName('target').setDescription('Username or UUID').setRequired(true))
    )
    // 10. reset-password
    .addSubcommand(sub =>
        sub.setName('reset-password')
            .setDescription('Reset password for a user account')
            .addStringOption(opt => opt.setName('target').setDescription('Username or Discord ID').setRequired(true))
            .addStringOption(opt => opt.setName('new_password').setDescription('New password (or leave blank for random)').setRequired(false))
    )
    // 11. sessions
    .addSubcommand(sub =>
        sub.setName('sessions')
            .setDescription('View active device sessions for a user')
            .addStringOption(opt => opt.setName('target').setDescription('Username or Discord ID').setRequired(true))
    );

export async function execute(interaction) {
    if (!isAdministrator(interaction.member)) {
        return interaction.reply({ content: `${EMOJIS.ERROR} You must be an administrator or owner to use this command.`, ephemeral: true });
    }

    const sub = interaction.options.getSubcommand();
    const supabase = getSupabase();
    await interaction.deferReply({ ephemeral: true });

    try {
        if (sub === 'create') {
            const username = interaction.options.getString('username');
            const password = interaction.options.getString('password');
            const discordUser = interaction.options.getUser('discord_user');

            const { profile, plainPassword, isNewUser } = await getOrCreateAccount({
                username,
                password,
                discordUserId: discordUser?.id || interaction.user.id
            });

            const embed = new EmbedBuilder()
                .setColor(CONFIG.COLORS.SUCCESS)
                .setTitle(`${EMOJIS.USER_ADD} Account Successfully Created`)
                .addFields(
                    { name: `${EMOJIS.PROFILE} Username`, value: `\`@${profile.username}\``, inline: true },
                    { name: `${EMOJIS.SECURITY} Password`, value: `\`${plainPassword}\``, inline: true },
                    { name: `${EMOJIS.USER} Discord User`, value: discordUser ? `<@${discordUser.id}>` : '`Unlinked`', inline: true },
                    { name: `${EMOJIS.VERIFIED} Status`, value: '`ACTIVE`', inline: true }
                )
                .setFooter({ text: 'InSightX HQ Identity Management' })
                .setTimestamp();

            return await interaction.editReply({ embeds: [embed] });
        }

        if (sub === 'info') {
            const target = interaction.options.getString('target');
            const profile = await findUser(target);
            if (!profile) return await interaction.editReply(`${EMOJIS.ERROR} User not found: \`${target}\``);

            const { data: subs } = await supabase.from('subscriptions').select('*, products(*), plans(*)').eq('user_id', profile.id);
            const { data: payments } = await supabase.from('payments').select('*').eq('user_id', profile.id);

            const embed = new EmbedBuilder()
                .setColor(CONFIG.COLORS.INFO)
                .setTitle(`${EMOJIS.PROFILE} Account Dossier: @${profile.username}`)
                .addFields(
                    { name: 'UUID', value: `\`${profile.id}\``, inline: false },
                    { name: `${EMOJIS.USER} Username`, value: `\`@${profile.username}\``, inline: true },
                    { name: `${EMOJIS.VERIFIED} Status`, value: `\`${profile.account_status.toUpperCase()}\``, inline: true },
                    { name: `${EMOJIS.SECURITY} Discord Profile`, value: profile.discord_user_id ? `<@${profile.discord_user_id}>` : '`None`', inline: true },
                    { name: '📅 Member Since', value: formatDate(profile.created_at), inline: true },
                    { name: `${EMOJIS.SUBSCRIPTION} Subscriptions`, value: `\`${subs?.length || 0}\``, inline: true },
                    { name: `${EMOJIS.PAYMENT} Total Payments`, value: `\`${payments?.length || 0}\``, inline: true }
                )
                .setFooter({ text: 'InSightX HQ Directory Registry' });

            return await interaction.editReply({ embeds: [embed] });
        }

        if (sub === 'search') {
            const query = interaction.options.getString('query');
            const { data: profiles } = await supabase
                .from('profiles')
                .select('*')
                .or(`username.ilike.%${query}%,email.ilike.%${query}%,discord_user_id.eq.${query}`)
                .limit(10);

            if (!profiles || profiles.length === 0) {
                return await interaction.editReply(`${EMOJIS.WARNING} No accounts found matching \`${query}\`.`);
            }

            const embed = new EmbedBuilder()
                .setColor(CONFIG.COLORS.INFO)
                .setTitle(`${EMOJIS.ANALYTICS} Search Directory: "${query}"`)
                .setDescription(
                    profiles.map((p, i) =>
                        `**${i + 1}.** ${EMOJIS.USER} \`@${p.username}\` — Status: \`${p.account_status}\` — ID: \`${p.id.slice(0, 8)}...\``
                    ).join('\n')
                );

            return await interaction.editReply({ embeds: [embed] });
        }

        if (sub === 'ban' || sub === 'unban' || sub === 'suspend' || sub === 'unsuspend') {
            const target = interaction.options.getString('target');
            const reason = interaction.options.getString('reason') || '';
            const profile = await findUser(target);
            if (!profile) return await interaction.editReply(`${EMOJIS.ERROR} User not found: \`${target}\``);

            const newStatus = (sub === 'unban' || sub === 'unsuspend') ? 'active' : (sub === 'ban' ? 'banned' : 'suspended');
            await updateUserAccountStatus(interaction.user.id, profile.id, newStatus, reason);

            return await interaction.editReply(`${newStatus === 'active' ? EMOJIS.SUCCESS : EMOJIS.WARNING} Account for \`@${profile.username}\` set to **${newStatus.toUpperCase()}**.`);
        }

        if (sub === 'reset-password') {
            const target = interaction.options.getString('target');
            const profile = await findUser(target);
            if (!profile) return await interaction.editReply(`${EMOJIS.ERROR} User not found: \`${target}\``);

            const newPass = interaction.options.getString('new_password') || `Pass_${Math.random().toString(36).slice(-6)}!`;
            const { error: resetErr } = await supabase.auth.admin.updateUserById(profile.id, { password: newPass });
            if (resetErr) throw resetErr;

            return await interaction.editReply(`${EMOJIS.SECURITY} Password for \`@${profile.username}\` reset to: \`${newPass}\``);
        }

        if (sub === 'delete') {
            const target = interaction.options.getString('target');
            const profile = await findUser(target);
            if (!profile) return await interaction.editReply(`${EMOJIS.ERROR} User not found: \`${target}\``);

            await supabase.from('profiles').delete().eq('id', profile.id);
            await supabase.auth.admin.deleteUser(profile.id).catch(() => {});

            return await interaction.editReply(`${EMOJIS.USER_REMOVE} Account for \`@${profile.username}\` has been permanently deleted.`);
        }

        if (sub === 'sessions') {
            const target = interaction.options.getString('target');
            const profile = await findUser(target);
            if (!profile) return await interaction.editReply(`${EMOJIS.ERROR} User not found: \`${target}\``);

            const { data: sessions } = await supabase.from('device_sessions').select('*').eq('user_id', profile.id).limit(5);

            if (!sessions || sessions.length === 0) {
                return await interaction.editReply(`${EMOJIS.WARNING} No device sessions recorded for \`@${profile.username}\`.`);
            }

            const embed = new EmbedBuilder()
                .setColor(CONFIG.COLORS.INFO)
                .setTitle(`${EMOJIS.SECURITY} Mobile Device Sessions: @${profile.username}`)
                .setDescription(sessions.map(s => `• ${EMOJIS.SERVER} **${s.device_name || 'Android Device'}** — Status: \`${s.session_status}\` — Last active: ${formatDate(s.last_active)}`).join('\n'));

            return await interaction.editReply({ embeds: [embed] });
        }
    } catch (err) {
        return await interaction.editReply(`${EMOJIS.ERROR} Error: ${err.message}`);
    }
}
