import { REST, Routes } from 'discord.js';
import { CONFIG } from '../../config/constants.js';
import { data as purchaseCommand } from '../commands/purchase.js';
import { data as adminCommand } from '../commands/admin.js';
import { data as helpCommand } from '../commands/help.js';
import { data as pingCommand } from '../commands/ping.js';
import { data as accountCommand } from '../commands/account.js';
import { data as profileCommand } from '../commands/customer/profile.js';
import { data as subCustomerCommand } from '../commands/customer/subscription.js';
import { data as usageCustomerCommand } from '../commands/customer/usage.js';
import { data as paymentHistoryCommand } from '../commands/customer/payments.js';
import { data as changePasswordCommand } from '../commands/customer/accountAuth.js';
import { data as supportCommand } from '../commands/customer/support.js';
import { data as userAdminCommand } from '../commands/admin/userCmds.js';
import { data as subAdminCommand } from '../commands/admin/subCmds.js';
import { data as paymentAdminCommand } from '../commands/admin/paymentCmds.js';
import { data as usageAdminCommand } from '../commands/admin/usageCmds.js';
import { data as ownerCommand } from '../commands/owner/ownerCmds.js';
import { data as systemCommand } from '../commands/system/systemCmds.js';
import { data as uploadEmojisCommand } from '../commands/admin/uploadEmojis.js';

export async function onReady(client) {
    console.log(`[InSightX HQ] Logged in successfully as ${client.user.tag}`);

    const commands = [
        purchaseCommand.toJSON(),
        adminCommand.toJSON(),
        helpCommand.toJSON(),
        pingCommand.toJSON(),
        accountCommand.toJSON(),
        profileCommand.toJSON(),
        subCustomerCommand.toJSON(),
        usageCustomerCommand.toJSON(),
        paymentHistoryCommand.toJSON(),
        changePasswordCommand.toJSON(),
        supportCommand.toJSON(),
        userAdminCommand.toJSON(),
        subAdminCommand.toJSON(),
        paymentAdminCommand.toJSON(),
        usageAdminCommand.toJSON(),
        ownerCommand.toJSON(),
        systemCommand.toJSON(),
        uploadEmojisCommand.toJSON(),
    ];
    const rest = new REST({ version: '10' }).setToken(CONFIG.DISCORD_BOT_TOKEN);

    try {
        console.log('[Discord Bot] Refreshing application (/) commands...');

        if (CONFIG.DISCORD_GUILD_ID) {
            // Guild commands (instant propagation for development/server)
            await rest.put(
                Routes.applicationGuildCommands(CONFIG.DISCORD_CLIENT_ID, CONFIG.DISCORD_GUILD_ID),
                { body: commands }
            );
            console.log(`[Discord Bot] Successfully registered commands in guild: ${CONFIG.DISCORD_GUILD_ID}`);
        } else {
            // Global commands
            await rest.put(
                Routes.applicationCommands(CONFIG.DISCORD_CLIENT_ID),
                { body: commands }
            );
            console.log('[Discord Bot] Successfully registered global commands.');
        }
    } catch (error) {
        console.error('[Discord Bot] Error registering slash commands:', error);
    }
}
