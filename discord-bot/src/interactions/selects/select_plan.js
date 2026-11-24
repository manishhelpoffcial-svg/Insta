import { ModalBuilder, TextInputBuilder, TextInputStyle, ActionRowBuilder } from 'discord.js';

export async function handleSelectPlan(interaction) {
    const [productId, planId] = interaction.values[0].split(':');

    const modal = new ModalBuilder()
        .setCustomId(`email_modal_${productId}_${planId}`)
        .setTitle('Set App Credentials');

    const usernameInput = new TextInputBuilder()
        .setCustomId('preferred_username_input')
        .setLabel('Desired App Username (letters, numbers, dot)')
        .setPlaceholder('e.g. cinema_enthusiast')
        .setStyle(TextInputStyle.Short)
        .setRequired(true)
        .setMinLength(3)
        .setMaxLength(30);

    const passwordInput = new TextInputBuilder()
        .setCustomId('account_password_input')
        .setLabel('App Password (or leave blank to auto-generate)')
        .setPlaceholder('Leave blank for auto-generated password')
        .setStyle(TextInputStyle.Short)
        .setRequired(false)
        .setMaxLength(50);

    modal.addComponents(
        new ActionRowBuilder().addComponents(usernameInput),
        new ActionRowBuilder().addComponents(passwordInput)
    );

    await interaction.showModal(modal);
}
