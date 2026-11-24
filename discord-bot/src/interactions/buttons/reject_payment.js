import { ModalBuilder, TextInputBuilder, TextInputStyle, ActionRowBuilder } from 'discord.js';
import { isAdministrator } from './approve_payment.js';

export async function handleRejectPaymentButton(interaction) {
    if (!isAdministrator(interaction.member)) {
        return interaction.reply({
            content: '❌ You do not have permission to reject payments.',
            ephemeral: true
        });
    }

    const paymentId = interaction.customId.replace('reject_pay_', '');

    const modal = new ModalBuilder()
        .setCustomId(`rejection_modal_${paymentId}`)
        .setTitle('Reject Payment Verification');

    const reasonInput = new TextInputBuilder()
        .setCustomId('rejection_reason_input')
        .setLabel('Rejection Reason (sent to customer)')
        .setPlaceholder('e.g. UTR number not found in merchant bank statement')
        .setStyle(TextInputStyle.Paragraph)
        .setRequired(true)
        .setMaxLength(500);

    modal.addComponents(new ActionRowBuilder().addComponents(reasonInput));

    await interaction.showModal(modal);
}
