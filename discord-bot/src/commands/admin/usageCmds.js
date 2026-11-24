import { SlashCommandBuilder, EmbedBuilder } from 'discord.js';
import { getSupabase } from '../../database/supabaseClient.js';
import { CONFIG } from '../../../config/constants.js';
import { isAdministrator } from '../../interactions/buttons/approve_payment.js';
import { findUser } from '../../admin/adminActions.js';
import { EMOJIS } from '../../utils/emojis.js';

export const data = new SlashCommandBuilder()
    .setName('usage')
    .setDescription('Manage user action quotas, usage limits, leaderboard and logs')
    // 1. check
    .addSubcommand(sub =>
        sub.setName('check')
            .setDescription('Check usage stats for a specific user')
            .addStringOption(opt => opt.setName('target').setDescription('Username or Discord ID').setRequired(true))
    )
    // 2. leaderboard
    .addSubcommand(sub =>
        sub.setName('leaderboard')
            .setDescription('View top active users by monthly action volume')
    )
    // 3. reset
    .addSubcommand(sub =>
        sub.setName('reset')
            .setDescription('Reset daily usage counter for a user')
            .addStringOption(opt => opt.setName('target').setDescription('Username or Discord ID').setRequired(true))
    )
    // 4. set-limit
    .addSubcommand(sub =>
        sub.setName('set-limit')
            .setDescription('Set daily action quota limit for a user')
            .addStringOption(opt => opt.setName('target').setDescription('Username or Discord ID').setRequired(true))
            .addIntegerOption(opt => opt.setName('limit').setDescription('Daily limit quota').setRequired(true).setMinValue(10))
    )
    // 5. add-limit
    .addSubcommand(sub =>
        sub.setName('add-limit')
            .setDescription('Add bonus action limit to existing daily quota')
            .addStringOption(opt => opt.setName('target').setDescription('Username or Discord ID').setRequired(true))
            .addIntegerOption(opt => opt.setName('amount').setDescription('Bonus actions to add').setRequired(true).setMinValue(1))
    )
    // 6. logs
    .addSubcommand(sub =>
        sub.setName('logs')
            .setDescription('View recent user action audit logs')
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
        if (sub === 'check') {
            const target = interaction.options.getString('target');
            const profile = await findUser(target);
            if (!profile) return await interaction.editReply(`${EMOJIS.ERROR} User not found: \`${target}\``);

            const { data: usage } = await supabase.from('user_usage').select('*').eq('user_id', profile.id).maybeSingle();

            const embed = new EmbedBuilder()
                .setColor(CONFIG.COLORS.INFO)
                .setTitle(`${EMOJIS.ANALYTICS} Usage Metrics: @${profile.username}`)
                .addFields(
                    { name: `${EMOJIS.USAGE} Today Quota`, value: `\`${usage?.daily_count || 0} / ${usage?.custom_limit || 100}\``, inline: true },
                    { name: '📅 Monthly Actions', value: `\`${usage?.monthly_count || 0}\``, inline: true },
                    { name: `${EMOJIS.LIMIT} Daily Limit`, value: `\`${usage?.custom_limit || 100}\``, inline: true }
                );

            return await interaction.editReply({ embeds: [embed] });
        }

        if (sub === 'leaderboard') {
            const { data: list } = await supabase
                .from('user_usage')
                .select('*, profiles(username)')
                .order('monthly_count', { ascending: false })
                .limit(10);

            if (!list || list.length === 0) return await interaction.editReply(`${EMOJIS.WARNING} No usage records recorded yet.`);

            const embed = new EmbedBuilder()
                .setColor(CONFIG.COLORS.PRIMARY)
                .setTitle(`${EMOJIS.USAGE} Top User Action Leaderboard (Monthly)`)
                .setDescription(list.map((u, i) =>
                    `**${i + 1}.** ${EMOJIS.USER} \`@${u.profiles?.username || 'user'}\` — **${u.monthly_count}** actions (Today: \`${u.daily_count}\`)`
                ).join('\n'));

            return await interaction.editReply({ embeds: [embed] });
        }

        if (sub === 'reset') {
            const target = interaction.options.getString('target');
            const profile = await findUser(target);
            if (!profile) return await interaction.editReply(`${EMOJIS.ERROR} User not found: \`${target}\``);

            await supabase
                .from('user_usage')
                .upsert({ user_id: profile.id, daily_count: 0, updated_at: new Date().toISOString() });

            return await interaction.editReply(`${EMOJIS.REFRESH} Reset daily usage count to 0 for \`@${profile.username}\`.`);
        }

        if (sub === 'set-limit') {
            const target = interaction.options.getString('target');
            const limit = interaction.options.getInteger('limit');
            const profile = await findUser(target);
            if (!profile) return await interaction.editReply(`${EMOJIS.ERROR} User not found: \`${target}\``);

            await supabase
                .from('user_usage')
                .upsert({ user_id: profile.id, custom_limit: limit, updated_at: new Date().toISOString() });

            return await interaction.editReply(`${EMOJIS.LIMIT} Daily quota limit for \`@${profile.username}\` set to **${limit} actions**.`);
        }

        if (sub === 'add-limit') {
            const target = interaction.options.getString('target');
            const amount = interaction.options.getInteger('amount');
            const profile = await findUser(target);
            if (!profile) return await interaction.editReply(`${EMOJIS.ERROR} User not found: \`${target}\``);

            const { data: usage } = await supabase.from('user_usage').select('custom_limit').eq('user_id', profile.id).maybeSingle();
            const current = usage?.custom_limit || 100;
            const newLimit = current + amount;

            await supabase
                .from('user_usage')
                .upsert({ user_id: profile.id, custom_limit: newLimit, updated_at: new Date().toISOString() });

            return await interaction.editReply(`${EMOJIS.SUCCESS} Added **+${amount} actions** to daily quota for \`@${profile.username}\`. New limit: **${newLimit}**.`);
        }

        if (sub === 'logs') {
            const target = interaction.options.getString('target');
            const profile = await findUser(target);
            if (!profile) return await interaction.editReply(`${EMOJIS.ERROR} User not found: \`${target}\``);

            const { data: logs } = await supabase.from('usage_logs').select('*').eq('user_id', profile.id).order('created_at', { ascending: false }).limit(10);

            if (!logs || logs.length === 0) return await interaction.editReply(`${EMOJIS.WARNING} No audit action logs found for \`@${profile.username}\`.`);

            const embed = new EmbedBuilder()
                .setColor(CONFIG.COLORS.INFO)
                .setTitle(`${EMOJIS.LOGS} Action Logs: @${profile.username}`)
                .setDescription(logs.map(l => `• \`${l.action}\` — *${l.details || 'OK'}*`).join('\n'));

            return await interaction.editReply({ embeds: [embed] });
        }
    } catch (err) {
        return await interaction.editReply(`${EMOJIS.ERROR} Error: ${err.message}`);
    }
}
