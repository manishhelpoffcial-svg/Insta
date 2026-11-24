// In-memory tracker for users waiting to upload screenshots
export const awaitingScreenshots = new Map();

export async function handleUploadScreenshotButton(interaction) {
    const parts = interaction.customId.split('_');
    const paymentId = parts[2];
    const reference = parts[3];

    // Store in waiting map with a 15-minute timeout
    awaitingScreenshots.set(interaction.user.id, {
        paymentId,
        reference,
        channelId: interaction.channelId,
        timestamp: Date.now()
    });

    await interaction.reply({
        content: `📸 **Ready!** Please send your payment screenshot right here as an image attachment for reference **\`${reference}\`**.\n*(Supported formats: PNG, JPG, JPEG, WEBP)*`,
        ephemeral: true
    });
}
