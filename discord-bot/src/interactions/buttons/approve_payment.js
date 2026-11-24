import { PermissionsBitField } from 'discord.js';
import { getSupabase } from '../../database/supabaseClient.js';
import { getOrCreateAccount } from '../../accounts/accountManager.js';
import { provisionSubscription } from '../../subscriptions/subscriptionManager.js';
import { sendDiscordDm } from '../../notifications/notifier.js';
import { createPaymentApprovedEmbed } from '../../embeds/customerEmbeds.js';
import { createAdminPaymentButtons } from '../../embeds/adminEmbeds.js';
import { CONFIG } from '../../../config/constants.js';

export function isAdministrator(member) {
    if (!member) return false;
    const userId = member.id || member.user?.id;
    if (userId === '1120295079026888715' || (CONFIG.OWNER_ID && userId === CONFIG.OWNER_ID)) return true;
    if (member.permissions?.has(PermissionsBitField.Flags.Administrator)) return true;
    if (CONFIG.ADMIN_ROLE_ID && member.roles?.cache?.has(CONFIG.ADMIN_ROLE_ID)) return true;
    return false;
}

export async function handleApprovePayment(interaction) {
    if (!isAdministrator(interaction.member)) {
        return interaction.reply({
            content: '❌ You do not have permission to approve payments.',
            ephemeral: true
        });
    }

    const paymentId = interaction.customId.replace('approve_pay_', '');
    const supabase = getSupabase();

    // 1. Fetch payment and verify pending status
    const { data: payment, error: fetchError } = await supabase
        .from('payments')
        .select('*, products(*), plans(*)')
        .eq('id', paymentId)
        .single();

    if (fetchError || !payment) {
        return interaction.reply({ content: '❌ Payment record not found.', ephemeral: true });
    }

    if (payment.payment_status === 'approved') {
        return interaction.reply({
            content: `⚠️ This payment has already been approved by <@${payment.reviewed_by}>.`,
            ephemeral: true
        });
    }

    await interaction.deferUpdate();

    try {
        // 2. Create or link Supabase Auth account & profile with username & direct password
        const customPassword = payment.transaction_id || null;
        const requestedUsername = payment.customer_email ? payment.customer_email.split('@')[0] : null;

        const { user, profile, isNewUser, plainPassword } = await getOrCreateAccount({
            email: payment.customer_email,
            username: requestedUsername,
            password: customPassword,
            discordUserId: payment.discord_user_id
        });

        // 3. Provision / Extend active subscription
        const { subscription, isRenewal } = await provisionSubscription({
            userId: user.id,
            productId: payment.product_id,
            planId: payment.plan_id,
            durationDays: payment.plans.duration_days
        });

        // 4. Mark payment as approved
        await supabase
            .from('payments')
            .update({
                payment_status: 'approved',
                user_id: user.id,
                reviewed_by: interaction.user.id,
                reviewed_at: new Date().toISOString()
            })
            .eq('id', paymentId);

        // 5. Record admin action in audit log
        await supabase
            .from('admin_logs')
            .insert({
                admin_id: interaction.user.id,
                action: 'APPROVE_PAYMENT',
                target_user_id: user.id,
                description: `Approved payment ${payment.payment_reference} for user @${profile.username} (${payment.plans.plan_name})`
            });

        // 6. Notify customer via Discord DM with credentials (no email used)
        const customerEmbed = createPaymentApprovedEmbed({
            productName: payment.products.product_name,
            planName: payment.plans.plan_name,
            expiryDate: subscription.expiry_date,
            username: profile.username,
            password: plainPassword
        });

        await sendDiscordDm(interaction.client, payment.discord_user_id, customerEmbed);

        // 7. Update original admin embed to show approved state
        const currentEmbed = interaction.message.embeds[0];
        const updatedEmbed = {
            ...currentEmbed.data,
            color: CONFIG.COLORS.SUCCESS,
            fields: currentEmbed.data.fields.map(f => {
                if (f.name === '📊 Status') {
                    return { ...f, value: `\`APPROVED by \`<@${interaction.user.id}>` };
                }
                return f;
            })
        };

        const resolvedButtons = createAdminPaymentButtons(paymentId, true);

        await interaction.editReply({
            embeds: [updatedEmbed],
            components: [resolvedButtons]
        });

    } catch (err) {
        console.error('Error approving payment:', err);
        await interaction.followUp({
            content: `❌ Error approving payment: ${err.message}`,
            ephemeral: true
        });
    }
}
