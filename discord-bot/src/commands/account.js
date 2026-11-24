import { SlashCommandBuilder, EmbedBuilder } from 'discord.js';
import { getSupabase } from '../database/supabaseClient.js';
import { CONFIG } from '../../config/constants.js';
import { formatDate } from '../utils/validators.js';

export const data = new SlashCommandBuilder()
    .setName('account')
    .setDescription('View your InSightX subscription status and account details');

export async function getUserAccountEmbed(discordUserId) {
    const supabase = getSupabase();

    // 1. Find profile by discord_user_id
    const { data: profile } = await supabase
        .from('profiles')
        .select('*')
        .eq('discord_user_id', discordUserId)
        .maybeSingle();

    if (!profile) {
        return new EmbedBuilder()
            .setColor(CONFIG.COLORS.WARNING)
            .setTitle('🔍 No Linked Account Found')
            .setDescription(
                'You do not have an active InSightX account linked to your Discord profile yet.\n\n' +
                'Use `/purchase` or `.purchase` to choose a plan and subscribe.'
            )
            .setFooter({ text: 'InSightX HQ Customer Portal' });
    }

    // 2. Fetch active subscriptions
    const { data: subs } = await supabase
        .from('subscriptions')
        .select('*, products(*), plans(*)')
        .eq('user_id', profile.id)
        .order('expiry_date', { ascending: false });

    const activeSub = subs?.find(s => s.subscription_status === 'active' && new Date(s.expiry_date) > new Date());
    const isBanned = profile.account_status === 'banned';
    const isSuspended = profile.account_status === 'suspended';

    const embed = new EmbedBuilder()
        .setColor(
            isBanned || isSuspended
                ? CONFIG.COLORS.DANGER
                : activeSub
                    ? CONFIG.COLORS.SUCCESS
                    : CONFIG.COLORS.WARNING
        )
        .setTitle('👤 InSightX HQ — My Account')
        .addFields(
            { name: '🆔 Username', value: `\`@${profile.username}\``, inline: true },
            { name: '📊 Account Status', value: `\`${profile.account_status.toUpperCase()}\``, inline: true },
            {
                name: '📦 Product',
                value: activeSub ? `\`${activeSub.products?.product_name || 'Instagram Clone'}\`` : '`None`',
                inline: true
            },
            {
                name: '⏱️ Subscription Status',
                value: activeSub ? `\`ACTIVE\` (${activeSub.plans?.plan_name || 'Standard'})` : '`INACTIVE / EXPIRED`',
                inline: true
            },
            {
                name: '📅 Valid Until',
                value: activeSub ? `**${formatDate(activeSub.expiry_date)}**` : '`N/A`',
                inline: true
            }
        )
        .setFooter({ text: 'InSightX HQ • Keep your credentials confidential' })
        .setTimestamp();

    if (!activeSub && !isBanned && !isSuspended) {
        embed.setDescription('💡 *Your subscription is inactive. Use `/purchase` or `.purchase` to renew!*');
    }

    return embed;
}

export async function execute(interaction) {
    await interaction.deferReply({ ephemeral: true });
    const embed = await getUserAccountEmbed(interaction.user.id);
    await interaction.editReply({ embeds: [embed] });
}
