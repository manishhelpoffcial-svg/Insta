import { SlashCommandBuilder, EmbedBuilder } from 'discord.js';
import { getSupabase } from '../../database/supabaseClient.js';
import { CONFIG } from '../../../config/constants.js';
import { EMOJIS } from '../../utils/emojis.js';

export const data = new SlashCommandBuilder()
    .setName('myusage')
    .setDescription('Check your daily action count, monthly quota and limits');

export async function buildUsageEmbed(discordUserId) {
    const supabase = getSupabase();

    const { data: profile } = await supabase
        .from('profiles')
        .select('id, username')
        .eq('discord_user_id', discordUserId)
        .maybeSingle();

    if (!profile) {
        return new EmbedBuilder()
            .setColor(CONFIG.COLORS.WARNING)
            .setTitle(`${EMOJIS.WARNING} No Account Found`)
            .setDescription('Link or purchase an InSightX account to track usage.');
    }

    const { data: usage } = await supabase
        .from('user_usage')
        .select('*')
        .eq('user_id', profile.id)
        .maybeSingle();

    const daily = usage?.daily_count || 0;
    const limit = usage?.custom_limit || 100;
    const monthly = usage?.monthly_count || 0;
    const remaining = Math.max(0, limit - daily);
    const percent = Math.min(100, Math.round((daily / limit) * 100));

    return new EmbedBuilder()
        .setColor(percent >= 90 ? CONFIG.COLORS.DANGER : (percent >= 70 ? CONFIG.COLORS.WARNING : CONFIG.COLORS.SUCCESS))
        .setTitle(`${EMOJIS.USAGE} Usage Metrics — @${profile.username}`)
        .addFields(
            { name: `${EMOJIS.LIMIT} Today's Quota Used`, value: `\`${daily} / ${limit}\` (${percent}%)`, inline: true },
            { name: `${EMOJIS.SUCCESS} Remaining Today`, value: `\`${remaining}\``, inline: true },
            { name: `${EMOJIS.ANALYTICS} Monthly Volume`, value: `\`${monthly}\``, inline: true },
            { name: `${EMOJIS.REFRESH} Quota Reset`, value: 'Daily at 00:00 UTC', inline: true }
        )
        .setFooter({ text: 'InSightX HQ Usage Monitor' })
        .setTimestamp();
}

export async function execute(interaction) {
    await interaction.deferReply({ ephemeral: true });
    const embed = await buildUsageEmbed(interaction.user.id);
    await interaction.editReply({ embeds: [embed] });
}
