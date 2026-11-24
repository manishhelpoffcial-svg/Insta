import { awaitingScreenshots } from '../interactions/buttons/upload_screenshot.js';
import { uploadScreenshotToStorage, getSignedScreenshotUrl } from '../payments/screenshotHandler.js';
import { getSupabase } from '../database/supabaseClient.js';
import { createScreenshotSubmittedEmbed } from '../embeds/customerEmbeds.js';
import { createAdminPaymentLogEmbed, createAdminPaymentButtons } from '../embeds/adminEmbeds.js';
import { handlePrefixCommand } from '../commands/prefixHandler.js';
import { CONFIG } from '../../config/constants.js';

export async function onMessageCreate(message) {
    if (message.author.bot) return;

    // 1. Process prefix commands using "." (e.g. .purchase, .help, .ping, .account, .admin)
    try {
        const wasCommand = await handlePrefixCommand(message);
        if (wasCommand) return;
    } catch (err) {
        console.error('[Prefix Command Error]', err);
        await message.reply(`❌ Error executing command: ${err.message}`).catch(() => {});
        return;
    }

    // 2. Process pending screenshot uploads
    const pending = awaitingScreenshots.get(message.author.id);
    if (!pending) return;

    // Check if attachment exists
    const attachment = message.attachments.first();
    if (!attachment) return;

    const contentType = attachment.contentType || '';
    const isImage = contentType.startsWith('image/') || /\.(png|jpe?g|webp)$/i.test(attachment.name || '');

    if (!isImage) {
        return message.reply('❌ Please upload a valid image file (PNG, JPG, or WEBP).');
    }

    try {
        const loadingMsg = await message.reply('⏳ Uploading and submitting your payment screenshot for review...');

        // 1. Upload to Supabase Storage private bucket
        const storagePath = await uploadScreenshotToStorage({
            paymentReference: pending.reference,
            attachmentUrl: attachment.url,
            contentType: attachment.contentType
        });

        const supabase = getSupabase();

        // 2. Update payment status in Supabase
        const { data: updatedPayment, error: updateError } = await supabase
            .from('payments')
            .update({
                screenshot_path: storagePath,
                payment_status: 'pending_review'
            })
            .eq('id', pending.paymentId)
            .select('*, products(*), plans(*)')
            .single();

        if (updateError || !updatedPayment) {
            throw new Error(`Failed to update payment record: ${updateError?.message}`);
        }

        // 3. Clear waiting state
        awaitingScreenshots.delete(message.author.id);

        // 4. Send customer confirmation
        const customerEmbed = createScreenshotSubmittedEmbed(pending.reference);
        await loadingMsg.edit({ content: null, embeds: [customerEmbed] });

        // 5. Notify Admins in the dedicated payment logs channel
        if (CONFIG.PAYMENT_LOG_CHANNEL_ID) {
            try {
                const adminChannel = await message.client.channels.fetch(CONFIG.PAYMENT_LOG_CHANNEL_ID);
                if (adminChannel && adminChannel.isTextBased()) {
                    const signedUrl = await getSignedScreenshotUrl(storagePath);

                    const adminEmbed = createAdminPaymentLogEmbed({
                        paymentId: updatedPayment.id,
                        reference: updatedPayment.payment_reference,
                        discordUserId: updatedPayment.discord_user_id,
                        customerEmail: updatedPayment.customer_email,
                        productName: updatedPayment.products?.product_name || 'Instagram Clone',
                        planName: updatedPayment.plans?.plan_name || 'Access Plan',
                        durationDays: updatedPayment.plans?.duration_days || 30,
                        amount: updatedPayment.amount,
                        screenshotSignedUrl: signedUrl,
                        submissionDate: updatedPayment.created_at,
                        status: 'pending_review'
                    });

                    const adminButtons = createAdminPaymentButtons(updatedPayment.id);

                    await adminChannel.send({
                        embeds: [adminEmbed],
                        components: [adminButtons]
                    });
                }
            } catch (err) {
                console.error('Error posting to payment logs channel:', err);
            }
        }
    } catch (err) {
        console.error('Screenshot processing error:', err);
        message.reply(`❌ Error processing screenshot: ${err.message}. Please try again.`);
    }
}
