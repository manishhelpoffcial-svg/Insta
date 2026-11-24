import { SlashCommandBuilder, EmbedBuilder } from 'discord.js';
import { getSupabase } from '../../database/supabaseClient.js';
import { CONFIG } from '../../../config/constants.js';
import { formatDate } from '../../utils/validators.js';
import { EMOJIS } from '../../utils/emojis.js';

export const data = new SlashCommandBuilder()
    .setName('subscription')
    .setDescription('View your personal active subscription details');

export async function buildSubscriptionEmbed(discordUserId) {
    const supabase = getSupabase();

    const { data: profile } = await supabase
        .from('profiles')
        .select('*')
        .eq('discord_user_id', discordUserId)
        .maybeSingle();

    if (!profile) {
        return new EmbedBuilder()
            .setColor(CONFIG.COLORS.WARNING)
            .setTitle(`${EMOJIS.WARNING} No Account Found`)
            .setDescription('Please run `/purchase` or `.purchase` to create an account and subscribe.');
    }

    const { data: subs } = await supabase
        .from('subscriptions')
        .select('*, products(*), plans(*)')
        .eq('user_id', profile.id)
        .order('expiry_date', { ascending: false });

    const activeSub = subs?.find(s => s.subscription_status === 'active' && new Date(s.expiry_date) > new Date());

    if (!activeSub) {
        return new EmbedBuilder()
            .setColor(CONFIG.COLORS.DANGER)
            .setTitle(`${EMOJIS.WARNING} Subscription Inactive or Expired`)
            .setDescription(
                `Your account **@${profile.username}** currently does not have an active subscription.\n\n` +
                'To regain access to Insight Editor and live features, use `/purchase` or `.purchase`.'
            );
    }

    const msDiff = new Date(activeSub.expiry_date).getTime() - Date.now();
    const daysLeft = Math.max(0, Math.ceil(msDiff / (1000 * 60 * 60 * 24)));

    return new EmbedBuilder()
        .setColor(CONFIG.COLORS.SUCCESS)
        .setTitle(`${EMOJIS.PREMIUM} Active InSightX Subscription`)
        .addFields(
            { name: `${EMOJIS.INSTAGRAM} Product`, value: `\`${activeSub.products?.product_name || 'Insight Editor'}\``, inline: true },
            { name: `${EMOJIS.PLAN} Plan`, value: `\`${activeSub.plans?.plan_name}\``, inline: true },
            { name: `${EMOJIS.RUPEE} Price`, value: `\`₹${activeSub.plans?.price}\``, inline: true },
            { name: `${EMOJIS.EXPIRY} Remaining Days`, value: `**${daysLeft} Day${daysLeft === 1 ? '' : 's'}**`, inline: true },
            { name: '📅 Expiry Date', value: `**${formatDate(activeSub.expiry_date)}**`, inline: true },
            { name: `${EMOJIS.VERIFIED} Status`, value: '`ACTIVE & VERIFIED`', inline: true }
        )
        .setFooter({ text: 'Use /purchase anytime to extend your duration early' })
        .setTimestamp();
}

export async function execute(interaction) {
    await interaction.deferReply({ ephemeral: true });
    const embed = await buildSubscriptionEmbed(interaction.user.id);
    await interaction.editReply({ embeds: [embed] });
}
