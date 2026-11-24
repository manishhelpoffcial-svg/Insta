import { execute as executePurchase } from '../commands/purchase.js';
import { execute as executeAdmin } from '../commands/admin.js';
import { execute as executeHelp } from '../commands/help.js';
import { execute as executePing } from '../commands/ping.js';
import { execute as executeAccount } from '../commands/account.js';
import { execute as executeProfile } from '../commands/customer/profile.js';
import { execute as executeSubCustomer } from '../commands/customer/subscription.js';
import { execute as executeUsageCustomer } from '../commands/customer/usage.js';
import { execute as executePaymentHistory } from '../commands/customer/payments.js';
import { execute as executeChangePassword } from '../commands/customer/accountAuth.js';
import { execute as executeSupport } from '../commands/customer/support.js';
import { execute as executeUserAdmin } from '../commands/admin/userCmds.js';
import { execute as executeSubAdmin } from '../commands/admin/subCmds.js';
import { execute as executePaymentAdmin } from '../commands/admin/paymentCmds.js';
import { execute as executeUsageAdmin } from '../commands/admin/usageCmds.js';
import { execute as executeOwner } from '../commands/owner/ownerCmds.js';
import { execute as executeSystem } from '../commands/system/systemCmds.js';
import { execute as executeUploadEmojis } from '../commands/admin/uploadEmojis.js';
import { handleSelectProduct } from '../interactions/selects/select_product.js';
import { handleSelectPlan } from '../interactions/selects/select_plan.js';
import { handleEmailModal } from '../interactions/modals/email_modal.js';
import { handleUploadScreenshotButton } from '../interactions/buttons/upload_screenshot.js';
import { handleApprovePayment } from '../interactions/buttons/approve_payment.js';
import { handleRejectPaymentButton } from '../interactions/buttons/reject_payment.js';
import { handleRejectionModal } from '../interactions/modals/rejection_modal.js';
import { handleViewCustomerButton } from '../interactions/buttons/view_customer.js';

export async function onInteractionCreate(interaction) {
    try {
        // 1. Slash Commands
        if (interaction.isChatInputCommand()) {
            if (interaction.commandName === 'purchase') {
                return await executePurchase(interaction);
            }
            if (interaction.commandName === 'admin') {
                return await executeAdmin(interaction);
            }
            if (interaction.commandName === 'help') {
                return await executeHelp(interaction);
            }
            if (interaction.commandName === 'ping') {
                return await executePing(interaction);
            }
            if (interaction.commandName === 'account') {
                return await executeAccount(interaction);
            }
            if (interaction.commandName === 'profile') {
                return await executeProfile(interaction);
            }
            if (interaction.commandName === 'subscription') {
                return await executeSubCustomer(interaction);
            }
            if (interaction.commandName === 'myusage') {
                return await executeUsageCustomer(interaction);
            }
            if (interaction.commandName === 'payment-history') {
                return await executePaymentHistory(interaction);
            }
            if (interaction.commandName === 'change-password') {
                return await executeChangePassword(interaction);
            }
            if (interaction.commandName === 'support') {
                return await executeSupport(interaction);
            }
            if (interaction.commandName === 'user') {
                return await executeUserAdmin(interaction);
            }
            if (interaction.commandName === 'subscription-admin') {
                return await executeSubAdmin(interaction);
            }
            if (interaction.commandName === 'payment') {
                return await executePaymentAdmin(interaction);
            }
            if (interaction.commandName === 'usage') {
                return await executeUsageAdmin(interaction);
            }
            if (interaction.commandName === 'owner') {
                return await executeOwner(interaction);
            }
            if (interaction.commandName === 'system') {
                return await executeSystem(interaction);
            }
            if (interaction.commandName === 'uploademojis') {
                return await executeUploadEmojis(interaction);
            }
        }

        // 2. Select Menus
        if (interaction.isStringSelectMenu()) {
            if (interaction.customId === 'select_product') {
                return await handleSelectProduct(interaction);
            }
            if (interaction.customId === 'select_plan') {
                return await handleSelectPlan(interaction);
            }
        }

        // 3. Modals
        if (interaction.isModalSubmit()) {
            if (interaction.customId.startsWith('email_modal_')) {
                return await handleEmailModal(interaction);
            }
            if (interaction.customId.startsWith('rejection_modal_')) {
                return await handleRejectionModal(interaction);
            }
        }

        // 4. Buttons
        if (interaction.isButton()) {
            if (interaction.customId.startsWith('upload_shot_')) {
                return await handleUploadScreenshotButton(interaction);
            }
            if (interaction.customId.startsWith('approve_pay_')) {
                return await handleApprovePayment(interaction);
            }
            if (interaction.customId.startsWith('reject_pay_')) {
                return await handleRejectPaymentButton(interaction);
            }
            if (interaction.customId.startsWith('view_cust_')) {
                return await handleViewCustomerButton(interaction);
            }
        }
    } catch (err) {
        console.error('Interaction execution error:', err);
        const errorMsg = '❌ An error occurred while processing this interaction.';
        if (interaction.replied || interaction.deferred) {
            await interaction.followUp({ content: errorMsg, ephemeral: true }).catch(() => {});
        } else {
            await interaction.reply({ content: errorMsg, ephemeral: true }).catch(() => {});
        }
    }
}
