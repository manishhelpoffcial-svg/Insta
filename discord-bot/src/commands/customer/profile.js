import { SlashCommandBuilder, EmbedBuilder } from 'discord.js';
import { getSupabase } from '../../database/supabaseClient.js';
import { CONFIG } from '../../../config/constants.js';
import { formatDate } from '../../utils/validators.js';
import { EMOJIS } from '../../utils/emojis.js';

export const data = new SlashCommandBuilder()
    .setName('profile')
    .setDescription('View your InSightX user profile, credentials and subscription overview');

export async function buildCustomerProfileEmbed(discordUserId) {
    const supabase = getSupabase();

    const { data: profile } = await supabase
        .from('profiles')
        .select('*')
        .eq('discord_user_id', discordUserId)
        .maybeSingle();

    if (!profile) {
        return new EmbedBuilder()
            .setColor(CONFIG.COLORS.WARNING)
            .setTitle(`${EMOJIS.WARNING} No InSightX Account Linked`)
            .setDescription(
                'No mobile app account is linked to your Discord profile.\n\n' +
                `• Run \`/purchase\` or \`.purchase\` to choose a plan & subscribe\n` +
                `• Run \`/link-account\` or \`.link-account <username>\` to link an existing account`
            )
            .setFooter({ text: 'InSightX HQ • Customer Portal' });
    }

    const { data: subs } = await supabase
        .from('subscriptions')
        .select('*, products(*), plans(*)')
        .eq('user_id', profile.id)
        .order('expiry_date', { ascending: false });

    const activeSub = subs?.find(s => s.subscription_status === 'active' && new Date(s.expiry_date) > new Date());
    const latestSub = subs?.[0];

    const { data: usage } = await supabase
        .from('user_usage')
        .select('*')
        .eq('user_id', profile.id)
        .maybeSingle();

    const embed = new EmbedBuilder()
        .setColor(activeSub ? CONFIG.COLORS.SUCCESS : CONFIG.COLORS.WARNING)
        .setTitle(`${EMOJIS.DASHBOARD} InSightX Dashboard — @${profile.username}`)
        .addFields(
            { name: `${EMOJIS.USER} Username`, value: `\`@${profile.username}\``, inline: true },
            { name: `${EMOJIS.SECURITY} Status`, value: `\`${profile.account_status.toUpperCase()}\``, inline: true },
            { name: '📅 Member Since', value: formatDate(profile.created_at), inline: true },
            {
                name: `${EMOJIS.INSTAGRAM} Product`,
                value: activeSub ? `\`${activeSub.products?.product_name || 'Insight Editor'}\`` : '`None`',
                inline: true
            },
            {
                name: `${EMOJIS.PLAN} Subscription Plan`,
                value: activeSub ? `\`${activeSub.plans?.plan_name}\`` : '`No active plan`',
                inline: true
            },
            {
                name: `${EMOJIS.EXPIRY} Expiry Date`,
                value: activeSub ? `**${formatDate(activeSub.expiry_date)}**` : (latestSub ? `*Expired on ${formatDate(latestSub.expiry_date)}*` : '`N/A`'),
                inline: true
            },
            {
                name: `${EMOJIS.USAGE} Today's Actions`,
                value: `\`${usage?.daily_count || 0} / ${usage?.custom_limit || 100}\``,
                inline: true
            },
            {
                name: `${EMOJIS.ANALYTICS} Monthly Total`,
                value: `\`${usage?.monthly_count || 0}\``,
                inline: true
            },
            {
                name: `${EMOJIS.VERIFIED} Auth Mode`,
                value: 'Direct In-App Password',
                inline: true
            }
        )
        .setFooter({ text: 'InSightX HQ • Account Security' })
        .setTimestamp();

    return embed;
}

export async function execute(interaction) {
    await interaction.deferReply({ ephemeral: true });
    const embed = await buildCustomerProfileEmbed(interaction.user.id);
    await interaction.editReply({ embeds: [embed] });
}
