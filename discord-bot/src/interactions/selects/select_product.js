import { ActionRowBuilder, StringSelectMenuBuilder } from 'discord.js';
import { getSupabase } from '../../database/supabaseClient.js';
import { createPlanSelectEmbed } from '../../embeds/customerEmbeds.js';
import { formatCurrency } from '../../utils/validators.js';

export async function handleSelectProduct(interaction) {
    const productId = interaction.values[0];
    const supabase = getSupabase();

    // 1. Fetch product details
    const { data: product } = await supabase
        .from('products')
        .select('*')
        .eq('id', productId)
        .single();

    // 2. Fetch plans for this product dynamically
    const { data: plans, error } = await supabase
        .from('plans')
        .select('*')
        .eq('product_id', productId)
        .eq('is_active', true)
        .order('duration_days', { ascending: true });

    if (error || !plans || plans.length === 0) {
        return interaction.update({
            content: '❌ No active subscription plans found for this product.',
            embeds: [],
            components: []
        });
    }

    const planOptions = plans.map(plan => ({
        label: `${plan.plan_name} (${plan.duration_days} Days)`,
        description: `Price: ${formatCurrency(plan.price)}`,
        value: `${productId}:${plan.id}`,
        emoji: '⏱️'
    }));

    const planSelectRow = new ActionRowBuilder().addComponents(
        new StringSelectMenuBuilder()
            .setCustomId('select_plan')
            .setPlaceholder('Choose a duration plan...')
            .addOptions(planOptions)
    );

    const embed = createPlanSelectEmbed(product.product_name, product.description);

    await interaction.update({
        embeds: [embed],
        components: [planSelectRow]
    });
}
