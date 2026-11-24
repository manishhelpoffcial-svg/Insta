import { SlashCommandBuilder, EmbedBuilder } from 'discord.js';
import { getSupabase } from '../../database/supabaseClient.js';
import { CONFIG } from '../../../config/constants.js';
import { isAdministrator } from '../../interactions/buttons/approve_payment.js';
import { formatDate, formatCurrency } from '../../utils/validators.js';
import { getOrCreateAccount } from '../../accounts/accountManager.js';
import { provisionSubscription } from '../../subscriptions/subscriptionManager.js';
import { sendDiscordDm } from '../../notifications/notifier.js';
import { createPaymentApprovedEmbed } from '../../embeds/customerEmbeds.js';
import { EMOJIS } from '../../utils/emojis.js';

export const data = new SlashCommandBuilder()
    .setName('payment')
    .setDescription('Administrative payment verification, approvals, rejections and ledger')
    // 1. pending
    .addSubcommand(sub =>
        sub.setName('pending')
            .setDescription('List all payments pending review')
    )
    // 2. approve
    .addSubcommand(sub =>
        sub.setName('approve')
            .setDescription('Approve a pending payment reference')
            .addStringOption(opt => opt.setName('reference').setDescription('Unique payment reference (e.g. PAY-...)').setRequired(true))
    )
    // 3. reject
    .addSubcommand(sub =>
        sub.setName('reject')
            .setDescription('Reject a pending payment reference')
            .addStringOption(opt => opt.setName('reference').setDescription('Unique payment reference').setRequired(true))
            .addStringOption(opt => opt.setName('reason').setDescription('Reason for rejection').setRequired(false))
    )
    // 4. info
    .addSubcommand(sub =>
        sub.setName('info')
            .setDescription('View full details for a payment reference')
            .addStringOption(opt => opt.setName('reference').setDescription('Payment reference').setRequired(true))
    )
    // 5. search
    .addSubcommand(sub =>
        sub.setName('search')
            .setDescription('Search payments by customer username, email, or reference')
            .addStringOption(opt => opt.setName('query').setDescription('Keyword to search').setRequired(true))
    )
    // 6. history
    .addSubcommand(sub =>
        sub.setName('history')
            .setDescription('View the last 15 payment transactions')
    )
    // 7. accepted
    .addSubcommand(sub =>
        sub.setName('accepted')
            .setDescription('View approved payments')
    )
    // 8. rejected
    .addSubcommand(sub =>
        sub.setName('rejected')
            .setDescription('View rejected payments')
    )
    // 9. refund
    .addSubcommand(sub =>
        sub.setName('refund')
            .setDescription('Mark a payment as refunded and revoke subscription')
            .addStringOption(opt => opt.setName('reference').setDescription('Payment reference').setRequired(true))
    );

export async function execute(interaction) {
    if (!isAdministrator(interaction.member)) {
        return interaction.reply({ content: `${EMOJIS.ERROR} You must be an administrator or owner to use this command.`, ephemeral: true });
    }

    const sub = interaction.options.getSubcommand();
    const supabase = getSupabase();
    await interaction.deferReply({ ephemeral: true });

    try {
        if (sub === 'pending') {
            const { data: payments } = await supabase
                .from('payments')
                .select('*, plans(*)')
                .in('payment_status', ['pending_review', 'pending'])
                .order('created_at', { ascending: false })
                .limit(10);

            if (!payments || payments.length === 0) return await interaction.editReply(`${EMOJIS.SUCCESS} No payments currently pending review.`);

            const embed = new EmbedBuilder()
                .setColor(CONFIG.COLORS.WARNING)
                .setTitle(`${EMOJIS.PENDING} Payments Pending Review`)
                .setDescription(payments.map(p =>
                    `• Ref: \`${p.payment_reference}\` — **${formatCurrency(p.amount)}** — User: <@${p.discord_user_id}> (${p.plans?.plan_name}) — Status: \`${p.payment_status}\``
                ).join('\n'));

            return await interaction.editReply({ embeds: [embed] });
        }

        if (sub === 'approve') {
            const ref = interaction.options.getString('reference').trim().toUpperCase();
            const { data: payment } = await supabase
                .from('payments')
                .select('*, products(*), plans(*)')
                .eq('payment_reference', ref)
                .single();

            if (!payment) return await interaction.editReply(`${EMOJIS.ERROR} Payment ref \`${ref}\` not found.`);
            if (payment.payment_status === 'approved') return await interaction.editReply(`${EMOJIS.WARNING} Payment \`${ref}\` was already approved.`);

            const customPass = payment.transaction_id || null;
            const requestedUsername = payment.customer_email ? payment.customer_email.split('@')[0] : null;

            const { user, profile, plainPassword } = await getOrCreateAccount({
                email: payment.customer_email,
                username: requestedUsername,
                password: customPass,
                discordUserId: payment.discord_user_id
            });

            const { subscription } = await provisionSubscription({
                userId: user.id,
                productId: payment.product_id,
                planId: payment.plan_id,
                durationDays: payment.plans.duration_days
            });

            await supabase
                .from('payments')
                .update({ payment_status: 'approved', user_id: user.id, reviewed_by: interaction.user.id, reviewed_at: new Date().toISOString() })
                .eq('id', payment.id);

            const customerEmbed = createPaymentApprovedEmbed({
                productName: payment.products.product_name,
                planName: payment.plans.plan_name,
                expiryDate: subscription.expiry_date,
                username: profile.username,
                password: plainPassword
            });
            await sendDiscordDm(interaction.client, payment.discord_user_id, customerEmbed);

            return await interaction.editReply(`${EMOJIS.APPROVE} Payment \`${ref}\` successfully **APPROVED** and credentials DM'd to customer.`);
        }

        if (sub === 'reject') {
            const ref = interaction.options.getString('reference').trim().toUpperCase();
            const reason = interaction.options.getString('reason') || 'Transaction unverified in merchant statement';

            const { data: payment } = await supabase.from('payments').select('*').eq('payment_reference', ref).single();
            if (!payment) return await interaction.editReply(`${EMOJIS.ERROR} Payment ref \`${ref}\` not found.`);

            await supabase
                .from('payments')
                .update({ payment_status: 'rejected', rejection_reason: reason, reviewed_by: interaction.user.id, reviewed_at: new Date().toISOString() })
                .eq('id', payment.id);

            return await interaction.editReply(`${EMOJIS.REJECT} Payment \`${ref}\` marked as **REJECTED**.\nReason: *${reason}*`);
        }

        if (sub === 'info') {
            const ref = interaction.options.getString('reference').trim().toUpperCase();
            const { data: p } = await supabase
                .from('payments')
                .select('*, products(*), plans(*)')
                .eq('payment_reference', ref)
                .single();

            if (!p) return await interaction.editReply(`${EMOJIS.ERROR} Payment ref \`${ref}\` not found.`);

            const embed = new EmbedBuilder()
                .setColor(CONFIG.COLORS.INFO)
                .setTitle(`${EMOJIS.PAYMENT} Payment Record: ${p.payment_reference}`)
                .addFields(
                    { name: `${EMOJIS.MONEY} Amount`, value: formatCurrency(p.amount), inline: true },
                    { name: `${EMOJIS.VERIFIED} Status`, value: `\`${p.payment_status.toUpperCase()}\``, inline: true },
                    { name: `${EMOJIS.USER} Discord User`, value: `<@${p.discord_user_id}>`, inline: true },
                    { name: '📦 Product', value: p.products?.product_name || 'InSightX Pro', inline: true },
                    { name: `${EMOJIS.PLAN} Plan`, value: p.plans?.plan_name || 'Standard', inline: true },
                    { name: '📅 Date', value: formatDate(p.created_at), inline: true }
                );

            return await interaction.editReply({ embeds: [embed] });
        }

        if (sub === 'history' || sub === 'accepted' || sub === 'rejected') {
            let q = supabase.from('payments').select('*, plans(*)').order('created_at', { ascending: false }).limit(15);
            if (sub === 'accepted') q = q.eq('payment_status', 'approved');
            if (sub === 'rejected') q = q.eq('payment_status', 'rejected');

            const { data: payments } = await q;
            if (!payments || payments.length === 0) return await interaction.editReply(`${EMOJIS.WARNING} No matching payment records.`);

            const embed = new EmbedBuilder()
                .setColor(CONFIG.COLORS.INFO)
                .setTitle(`${EMOJIS.PAYMENT} Payment Ledger (${sub.toUpperCase()})`)
                .setDescription(payments.map(p =>
                    `• ${EMOJIS.TRANSACTION} \`${p.payment_reference}\` — ${formatCurrency(p.amount)} — \`${p.payment_status}\` — ${formatDate(p.created_at)}`
                ).join('\n'));

            return await interaction.editReply({ embeds: [embed] });
        }

        if (sub === 'refund') {
            const ref = interaction.options.getString('reference').trim().toUpperCase();
            const { data: payment } = await supabase.from('payments').select('*').eq('payment_reference', ref).single();
            if (!payment) return await interaction.editReply(`${EMOJIS.ERROR} Payment ref \`${ref}\` not found.`);

            await supabase.from('payments').update({ payment_status: 'refunded' }).eq('id', payment.id);
            if (payment.user_id) {
                await supabase.from('subscriptions').update({ subscription_status: 'cancelled', expiry_date: new Date().toISOString() }).eq('user_id', payment.user_id);
            }

            return await interaction.editReply(`${EMOJIS.REFUND} Payment \`${ref}\` marked as **REFUNDED** and user access revoked.`);
        }
    } catch (err) {
        return await interaction.editReply(`${EMOJIS.ERROR} Error: ${err.message}`);
    }
}
