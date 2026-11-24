import { getSupabase } from '../../database/supabaseClient.js';
import { sendDiscordDm, sendEmail } from '../../notifications/notifier.js';
import { createPaymentRejectedEmbed } from '../../embeds/customerEmbeds.js';
import { createAdminPaymentButtons } from '../../embeds/adminEmbeds.js';
import { CONFIG } from '../../../config/constants.js';

export async function handleRejectionModal(interaction) {
    const paymentId = interaction.customId.replace('rejection_modal_', '');
    const reason = interaction.fields.getTextInputValue('rejection_reason_input').trim();

    const supabase = getSupabase();

    // 1. Fetch payment record
    const { data: payment, error } = await supabase
        .from('payments')
        .select('*')
        .eq('id', paymentId)
        .single();

    if (error || !payment) {
        return interaction.reply({ content: '❌ Payment record not found.', ephemeral: true });
    }

    await interaction.deferUpdate();

    // 2. Update payment status to rejected
    await supabase
        .from('payments')
        .update({
            payment_status: 'rejected',
            rejection_reason: reason,
            reviewed_by: interaction.user.id,
            reviewed_at: new Date().toISOString()
        })
        .eq('id', paymentId);

    // 3. Record in admin logs
    await supabase
        .from('admin_logs')
        .insert({
            admin_id: interaction.user.id,
            action: 'REJECT_PAYMENT',
            target_user_id: payment.user_id || null,
            description: `Rejected payment ${payment.payment_reference}. Reason: ${reason}`
        });

    // 4. Send customer notification via Discord DM
    const customerEmbed = createPaymentRejectedEmbed({
        reference: payment.payment_reference,
        reason
    });

    await sendDiscordDm(interaction.client, payment.discord_user_id, customerEmbed);

    // 5. Send customer notification via Email
    const emailBody = `
        <h2>Payment Verification Notice</h2>
        <p>Your payment request for reference <b>${payment.payment_reference}</b> was not approved.</p>
        <p><b>Reason:</b> ${reason}</p>
        <p>If you have any questions or paid with an alternate account, please reply or contact a server administrator.</p>
    `;

    await sendEmail({
        to: payment.customer_email,
        subject: `Payment Verification Declined: ${payment.payment_reference}`,
        htmlText: emailBody
    });

    // 6. Update admin message
    const currentEmbed = interaction.message.embeds[0];
    const updatedEmbed = {
        ...currentEmbed.data,
        color: CONFIG.COLORS.DANGER,
        fields: currentEmbed.data.fields.map(f => {
            if (f.name === '📊 Status') {
                return { ...f, value: `\`REJECTED by \`<@${interaction.user.id}>\n> ${reason.slice(0, 50)}...` };
            }
            return f;
        })
    };

    const resolvedButtons = createAdminPaymentButtons(paymentId, true);

    await interaction.editReply({
        embeds: [updatedEmbed],
        components: [resolvedButtons]
    });
}
