import { EmbedBuilder, ActionRowBuilder, ButtonBuilder, ButtonStyle } from 'discord.js';
import { CONFIG } from '../../config/constants.js';
import { formatCurrency, formatDate } from '../utils/validators.js';
import { EMOJIS } from '../utils/emojis.js';

export function createAdminPaymentLogEmbed({
    paymentId,
    reference,
    discordUserId,
    customerEmail,
    productName,
    planName,
    durationDays,
    amount,
    screenshotSignedUrl,
    submissionDate,
    status = 'pending_review'
}) {
    const embed = new EmbedBuilder()
        .setColor(status === 'approved' ? CONFIG.COLORS.SUCCESS : (status === 'rejected' ? CONFIG.COLORS.DANGER : CONFIG.COLORS.WARNING))
        .setTitle(`${EMOJIS.PAYMENT} Payment Verification Request — \`${reference}\``)
        .setDescription(
            `A customer has submitted a UPI transaction screenshot for administrator review.`
        )
        .addFields(
            { name: `${EMOJIS.TRANSACTION} Reference`, value: `\`${reference}\``, inline: true },
            { name: `${EMOJIS.USER} Customer`, value: `<@${discordUserId}> (\`${discordUserId}\`)`, inline: true },
            { name: `${EMOJIS.PLAN} Product`, value: `**${productName}**`, inline: true },
            { name: `${EMOJIS.EXPIRY} Plan`, value: `\`${planName}\` (${durationDays} Days)`, inline: true },
            { name: `${EMOJIS.RUPEE} Amount`, value: `**${formatCurrency(amount)}**`, inline: true },
            { name: `${EMOJIS.SECURITY} Status`, value: `\`${status.toUpperCase()}\``, inline: true },
            { name: '📅 Submitted At', value: formatDate(submissionDate), inline: false }
        )
        .setFooter({ text: `Payment ID: ${paymentId} • InSightX HQ Verification Queue` })
        .setTimestamp();

    if (screenshotSignedUrl) {
        embed.setImage(screenshotSignedUrl);
    }

    return embed;
}

export function createAdminPaymentButtons(paymentId, isResolved = false) {
    if (isResolved) {
        return new ActionRowBuilder().addComponents(
            new ButtonBuilder()
                .setCustomId(`view_cust_${paymentId}`)
                .setLabel('View Customer Profile')
                .setStyle(ButtonStyle.Secondary)
                .setEmoji('1574679629833306213')
        );
    }

    return new ActionRowBuilder().addComponents(
        new ButtonBuilder()
            .setCustomId(`approve_pay_${paymentId}`)
            .setLabel('Approve Payment')
            .setStyle(ButtonStyle.Success)
            .setEmoji('1574679419161677834'),
        new ButtonBuilder()
            .setCustomId(`reject_pay_${paymentId}`)
            .setLabel('Reject Payment')
            .setStyle(ButtonStyle.Danger)
            .setEmoji('1574679558060245103'),
        new ButtonBuilder()
            .setCustomId(`view_cust_${paymentId}`)
            .setLabel('View Customer')
            .setStyle(ButtonStyle.Secondary)
            .setEmoji('1574679629833306213')
    );
}

export function createCustomerOverviewEmbed({ profile, subscriptions, payments }) {
    const activeSub = subscriptions?.find(s => s.subscription_status === 'active');

    return new EmbedBuilder()
        .setColor(CONFIG.COLORS.INFO)
        .setTitle(`${EMOJIS.PROFILE} Customer Overview: @${profile.username}`)
        .addFields(
            { name: '🆔 User UUID', value: `\`${profile.id}\``, inline: false },
            { name: `${EMOJIS.USER} Username`, value: `\`@${profile.username}\``, inline: true },
            { name: `${EMOJIS.SECURITY} Account Status`, value: `\`${profile.account_status.toUpperCase()}\``, inline: true },
            { name: '👤 Discord ID', value: profile.discord_user_id ? `<@${profile.discord_user_id}>` : '`None`', inline: true },
            {
                name: `${EMOJIS.SUBSCRIPTION} Active Subscription`,
                value: activeSub
                    ? `**${activeSub.products?.product_name || 'Insight Editor'}**\nExpires: **${formatDate(activeSub.expiry_date)}**`
                    : '*No active subscription*',
                inline: false
            },
            {
                name: `${EMOJIS.PAYMENT} Recent Payments`,
                value: payments?.length > 0
                    ? payments.slice(0, 5).map(p => `• \`${p.payment_reference}\`: ${formatCurrency(p.amount)} [${p.payment_status}]`).join('\n')
                    : '*No payment records*',
                inline: false
            }
        )
        .setFooter({ text: 'InSightX HQ Customer Ledger' })
        .setTimestamp();
}
