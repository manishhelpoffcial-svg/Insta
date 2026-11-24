import { SlashCommandBuilder, EmbedBuilder } from 'discord.js';
import { getSupabase } from '../database/supabaseClient.js';
import { CONFIG } from '../../config/constants.js';
import { EMOJIS } from '../utils/emojis.js';

export const data = new SlashCommandBuilder()
    .setName('ping')
    .setDescription('Check InSightX HQ latency and service health');

export async function execute(interaction) {
    const sent = await interaction.deferReply({ fetchReply: true });
    const latency = sent.createdTimestamp - interaction.createdTimestamp;
    const wsPing = interaction.client.ws.ping;

    // Check Supabase DB response
    let dbStatus = `${EMOJIS.SUCCESS} Connected`;
    let dbLatency = 0;
    try {
        const start = Date.now();
        const supabase = getSupabase();
        await supabase.from('products').select('id', { count: 'exact', head: true });
        dbLatency = Date.now() - start;
    } catch {
        dbStatus = `${EMOJIS.ERROR} Unreachable`;
    }

    const embed = new EmbedBuilder()
        .setColor(CONFIG.COLORS.SUCCESS)
        .setTitle(`${EMOJIS.SYSTEM} InSightX HQ — System Diagnostics`)
        .addFields(
            { name: `${EMOJIS.SERVER} Bot Latency`, value: `\`${latency}ms\``, inline: true },
            { name: `${EMOJIS.API} WebSocket Ping`, value: `\`${wsPing}ms\``, inline: true },
            { name: `${EMOJIS.DATABASE} Supabase Database`, value: `${dbStatus} (\`${dbLatency}ms\`)`, inline: true },
            { name: `${EMOJIS.EXPIRY} Bot Uptime`, value: `\`${Math.floor(process.uptime() / 60)}m ${Math.floor(process.uptime() % 60)}s\``, inline: true },
            { name: `${EMOJIS.SETTINGS} Command Mode`, value: 'Dual Slash & Prefix (`.`)', inline: true }
        )
        .setFooter({ text: 'InSightX HQ Core v2.0 • Ultra Low Latency' })
        .setTimestamp();

    await interaction.editReply({ embeds: [embed] });
}
