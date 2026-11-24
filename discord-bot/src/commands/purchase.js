import { SlashCommandBuilder, ActionRowBuilder, StringSelectMenuBuilder, ComponentType } from 'discord.js';
import { getSupabase } from '../database/supabaseClient.js';
import { createProductSelectEmbed } from '../embeds/customerEmbeds.js';

export const data = new SlashCommandBuilder()
    .setName('purchase')
    .setDescription('Purchase or renew a subscription for Instagram Clone products');

export async function execute(interaction) {
    const supabase = getSupabase();

    // Fetch active products dynamically from Supabase
    const { data: products, error } = await supabase
        .from('products')
        .select('*')
        .eq('is_active', true)
        .order('created_at', { ascending: true });

    if (error || !products || products.length === 0) {
        return interaction.reply({
            content: '❌ No subscription products are currently available. Please try again later or contact an administrator.',
            ephemeral: true
        });
    }

    const selectOptions = products.map(product => ({
        label: product.product_name,
        description: (product.description || 'Full access plan').slice(0, 100),
        value: product.id,
        emoji: '📱'
    }));

    const selectRow = new ActionRowBuilder().addComponents(
        new StringSelectMenuBuilder()
            .setCustomId('select_product')
            .setPlaceholder('Choose a product to purchase access for...')
            .addOptions(selectOptions)
    );

    const embed = createProductSelectEmbed();

    await interaction.reply({
        embeds: [embed],
        components: [selectRow],
        ephemeral: true
    });
}
