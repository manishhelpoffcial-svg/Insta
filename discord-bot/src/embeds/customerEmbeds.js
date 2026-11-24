import { EmbedBuilder } from 'discord.js';
import { CONFIG } from '../../config/constants.js';
import { formatCurrency, formatDate } from '../utils/validators.js';
import { EMOJIS } from '../utils/emojis.js';

export function createProductSelectEmbed() {
    return new EmbedBuilder()
        .setColor(CONFIG.COLORS.PRIMARY)
        .setTitle(`${EMOJIS.INSTAGRAM} InSightX HQ — Select a Product`)
        .setDescription(
            `Welcome to the official **InSightX HQ** creator portal!\n\n` +
            `Please select the product you wish to purchase from the dropdown menu below.\n` +
            `Every subscription includes instant account provisioning and live tools access.`
        )
        .setFooter({ text: 'InSightX HQ • Official Secure Payment Portal' })
        .setTimestamp();
}

export function createPlanSelectEmbed(productName, description) {
    return new EmbedBuilder()
        .setColor(CONFIG.COLORS.PRIMARY)
        .setTitle(`${EMOJIS.PLAN} Select Subscription Plan — ${productName}`)
        .setDescription(
            `${description || 'Full mobile app & live creator analytics access.'}\n\n` +
            `Choose your desired duration from the options below:`
        )
        .setFooter({ text: 'InSightX HQ • Pricing loaded dynamically from Supabase' })
        .setTimestamp();
}

export function createPaymentEmbed({ productName, planName, durationDays, amount, reference, upiLink }) {
    return new EmbedBuilder()
        .setColor(CONFIG.COLORS.INFO)
        .setTitle(`${EMOJIS.PAYMENT} Complete Your UPI Payment`)
        .setDescription(
            `Thank you for initiating your **${productName}** order!\n\n` +
            `**3 Quick Steps to Activate Your Access:**\n` +
            `1. Tap **Open UPI App** or pay via the UPI ID below.\n` +
            `2. Pay the **exact amount** and enter the **Payment Reference** in your UPI transaction notes.\n` +
            `3. After paying, click **Upload Payment Screenshot** below to submit proof.`
        )
        .addFields(
            { name: `${EMOJIS.PLAN} Product`, value: `\`${productName}\``, inline: true },
            { name: `${EMOJIS.EXPIRY} Duration`, value: `\`${durationDays} Days\` (${planName})`, inline: true },
            { name: `${EMOJIS.RUPEE} Total Amount`, value: `**${formatCurrency(amount)}**`, inline: true },
            { name: `${EMOJIS.TRANSACTION} Payment Reference`, value: `\`${reference}\``, inline: false },
            { name: `${EMOJIS.UPI} Merchant UPI ID`, value: `\`${CONFIG.UPI_ID}\``, inline: true },
            { name: `${EMOJIS.OWNER} Payee Name`, value: `\`${CONFIG.MERCHANT_NAME}\``, inline: true }
        )
        .setFooter({ text: 'Important: Do not modify the reference code in your payment note' })
        .setTimestamp();
}

export function createScreenshotSubmittedEmbed(reference) {
    return new EmbedBuilder()
        .setColor(CONFIG.COLORS.SUCCESS)
        .setTitle(`${EMOJIS.SUCCESS} Payment Verification Request Submitted`)
        .setDescription(
            `Your payment verification request for reference **\`${reference}\`** has been received successfully!\n\n` +
            `${EMOJIS.PENDING} **Review in progress**: Our administration team will verify your transaction shortly.\n\n` +
            `${EMOJIS.SECURITY} Once approved, your instant login credentials will be delivered **directly to your Discord DMs**.`
        )
        .setFooter({ text: 'InSightX HQ • Automated Direct Provisioning' })
        .setTimestamp();
}

export function createPaymentApprovedEmbed({ productName, planName, expiryDate, username, password }) {
    const embed = new EmbedBuilder()
        .setColor(CONFIG.COLORS.SUCCESS)
        .setTitle(`${EMOJIS.VERIFIED} Payment Approved — Subscription Activated!`)
        .setDescription(
            `Your payment for **${productName}** (${planName}) has been verified and approved!\n\n` +
            `**Your App Login Credentials:**\n` +
            `• ${EMOJIS.USER} **Username**: \`${username}\`\n` +
            (password ? `• ${EMOJIS.SECURITY} **Password**: \`${password}\`\n` : '') +
            `• ${EMOJIS.EXPIRY} **Valid Until**: **${formatDate(expiryDate)}**\n\n` +
            `**How to Login:**\n` +
            `1. Open the **Instagram Clone** Android app.\n` +
            `2. Enter your **Username** and **Password**.\n` +
            `3. Enjoy your full creator access immediately!`
        )
        .setFooter({ text: 'InSightX HQ • Keep your credentials confidential' })
        .setTimestamp();

    return embed;
}

export function createPaymentRejectedEmbed({ reference, reason }) {
    return new EmbedBuilder()
        .setColor(CONFIG.COLORS.DANGER)
        .setTitle(`${EMOJIS.REJECT} Payment Verification Declined`)
        .setDescription(
            `Your payment request for reference **\`${reference}\`** could not be approved.\n\n` +
            `**Reason Provided:**\n> ${reason || 'Transaction could not be verified in merchant records.'}\n\n` +
            `If you believe this was in error, please open a ticket with \`/support create\` or contact an administrator.`
        )
        .setFooter({ text: 'InSightX HQ • Payment Audit' })
        .setTimestamp();
}

export function createExpirationNoticeEmbed({ productName, daysRemaining, expiryDate }) {
    const isToday = daysRemaining <= 0;
    return new EmbedBuilder()
        .setColor(isToday ? CONFIG.COLORS.DANGER : CONFIG.COLORS.WARNING)
        .setTitle(isToday ? `${EMOJIS.WARNING} Subscription Expired` : `${EMOJIS.EXPIRY} Subscription Expiring in ${daysRemaining} Day${daysRemaining > 1 ? 's' : ''}`)
        .setDescription(
            isToday
                ? `Your access to **${productName}** expired on **${formatDate(expiryDate)}**.\n\n` +
                  `To restore access to live Insights and creator tools, renew with \`/purchase\` or \`.purchase\`.`
                : `Your subscription to **${productName}** will expire on **${formatDate(expiryDate)}**.\n\n` +
                  `Renew early to prevent any interruption. Additional days will be added onto your existing expiry date.`
        )
        .setFooter({ text: 'Use /purchase to renew anytime' })
        .setTimestamp();
}
