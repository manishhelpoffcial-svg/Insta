import { ActionRowBuilder, ButtonBuilder, ButtonStyle } from 'discord.js';
import { getSupabase } from '../../database/supabaseClient.js';
import { generatePaymentReference } from '../../utils/validators.js';
import { generateUpiIntentLink } from '../../payments/upiGenerator.js';
import { createPaymentEmbed } from '../../embeds/customerEmbeds.js';

export async function handleEmailModal(interaction) {
    const customId = interaction.customId; // email_modal_{productId}_{planId}
    const parts = customId.replace('email_modal_', '').split('_');
    const productId = parts[0];
    const planId = parts[1];

    const usernameInput = interaction.fields.getTextInputValue('preferred_username_input')?.trim() || '';
    const passwordInput = interaction.fields.getTextInputValue('account_password_input')?.trim() || '';

    const cleanUsername = usernameInput.replace(/[^a-zA-Z0-9_.]/g, '').toLowerCase();

    if (cleanUsername.length < 3) {
        return interaction.reply({
            content: '❌ Desired username must be at least 3 characters (letters, numbers, dot, underscore).',
            ephemeral: true
        });
    }

    const internalEmail = `${cleanUsername}@insta.app`;
    const supabase = getSupabase();

    // 1. Fetch product & plan
    const { data: product } = await supabase.from('products').select('*').eq('id', productId).single();
    const { data: plan } = await supabase.from('plans').select('*').eq('id', planId).single();

    if (!product || !plan) {
        return interaction.reply({
            content: '❌ Selected product or plan is no longer available.',
            ephemeral: true
        });
    }

    // 2. Generate unique payment reference
    const reference = generatePaymentReference();

    // 3. Create pending payment record in Supabase
    const { data: paymentRecord, error: insertError } = await supabase
        .from('payments')
        .insert({
            customer_email: internalEmail,
            discord_user_id: interaction.user.id,
            product_id: productId,
            plan_id: planId,
            payment_reference: reference,
            amount: plan.price,
            transaction_id: passwordInput || null, // Store requested password temporarily if provided
            payment_status: 'pending'
        })
        .select()
        .single();

    if (insertError) {
        console.error('Error creating pending payment:', insertError);
        return interaction.reply({
            content: '❌ Could not create payment session. Please try again later.',
            ephemeral: true
        });
    }

    // 4. Generate UPI intent link
    const upiLink = generateUpiIntentLink({
        amount: plan.price,
        reference
    });

    const embed = createPaymentEmbed({
        productName: product.product_name,
        planName: plan.plan_name,
        durationDays: plan.duration_days,
        amount: plan.price,
        reference,
        upiLink
    });

    const paymentButtons = new ActionRowBuilder().addComponents(
        new ButtonBuilder()
            .setLabel('Open UPI App')
            .setStyle(ButtonStyle.Link)
            .setURL(upiLink)
            .setEmoji('📲'),
        new ButtonBuilder()
            .setCustomId(`upload_shot_${paymentRecord.id}_${reference}`)
            .setLabel('Upload Payment Screenshot')
            .setStyle(ButtonStyle.Primary)
            .setEmoji('📤')
    );

    await interaction.reply({
        embeds: [embed],
        components: [paymentButtons],
        ephemeral: true
    });
}
