import { SlashCommandBuilder, EmbedBuilder } from 'discord.js';
import { getSupabase } from '../../database/supabaseClient.js';
import { CONFIG } from '../../../config/constants.js';
import { formatDate, formatCurrency } from '../../utils/validators.js';

export const data = new SlashCommandBuilder()
    .setName('payment-history')
    .setDescription('View your submitted UPI payments and approval status');

export async function buildPaymentHistoryEmbed(discordUserId) {
    const supabase = getSupabase();

    const { data: payments } = await supabase
        .from('payments')
        .select('*, products(*), plans(*)')
        .eq('discord_user_id', discordUserId)
        .order('created_at', { ascending: false })
        .limit(10);

    if (!payments || payments.length === 0) {
        return new EmbedBuilder()
            .setColor(CONFIG.COLORS.INFO)
            .setTitle('💳 Payment History')
            .setDescription('You have not submitted any UPI payments yet.\nUse `/purchase` or `.purchase` to start an order.');
    }

    const embed = new EmbedBuilder()
        .setColor(CONFIG.COLORS.INFO)
        .setTitle('💳 Your Payment History')
        .setDescription(`Showing your last ${payments.length} payment records:`)
        .setFooter({ text: 'InSightX HQ Payment Ledger' })
        .setTimestamp();

    payments.forEach((p, idx) => {
        const statusEmoji = {
            approved: '✅',
            pending_review: '🕒',
            pending: '⏳',
            rejected: '❌',
            refunded: '↩️'
        }[p.payment_status] || '❓';

        embed.addFields({
            name: `${idx + 1}. Ref: \`${p.payment_reference}\` (${statusEmoji} ${p.payment_status.toUpperCase()})`,
            value:
                `• **Amount:** ${formatCurrency(p.amount)}\n` +
                `• **Plan:** ${p.plans?.plan_name || 'Standard'} (${p.products?.product_name || 'Insight'})\n` +
                `• **Date:** ${formatDate(p.created_at)}` +
                (p.rejection_reason ? `\n• **Note:** *${p.rejection_reason}*` : ''),
            inline: false
        });
    });

    return embed;
}

export async function execute(interaction) {
    await interaction.deferReply({ ephemeral: true });
    const embed = await buildPaymentHistoryEmbed(interaction.user.id);
    await interaction.editReply({ embeds: [embed] });
}
