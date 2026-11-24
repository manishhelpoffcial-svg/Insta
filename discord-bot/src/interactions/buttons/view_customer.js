import { getSupabase } from '../../database/supabaseClient.js';
import { createCustomerOverviewEmbed } from '../../embeds/adminEmbeds.js';
import { isAdministrator } from './approve_payment.js';

export async function handleViewCustomerButton(interaction) {
    if (!isAdministrator(interaction.member)) {
        return interaction.reply({ content: '❌ Administrator access required.', ephemeral: true });
    }

    const paymentId = interaction.customId.replace('view_cust_', '');
    const supabase = getSupabase();

    // Fetch payment to get user or email
    const { data: payment } = await supabase
        .from('payments')
        .select('*')
        .eq('id', paymentId)
        .single();

    if (!payment) {
        return interaction.reply({ content: '❌ Record not found.', ephemeral: true });
    }

    // Fetch profile
    let query = supabase.from('profiles').select('*');
    if (payment.user_id) query = query.eq('id', payment.user_id);
    else query = query.eq('email', payment.customer_email);

    const { data: profile } = await query.maybeSingle();

    if (!profile) {
        return interaction.reply({
            content: `ℹ️ No profile created yet for \`${payment.customer_email}\`. Account will be provisioned upon payment approval.`,
            ephemeral: true
        });
    }

    // Fetch user subscriptions and payments
    const { data: subscriptions } = await supabase
        .from('subscriptions')
        .select('*, products(*)')
        .eq('user_id', profile.id)
        .order('expiry_date', { ascending: false });

    const { data: payments } = await supabase
        .from('payments')
        .select('*')
        .eq('customer_email', profile.email)
        .order('created_at', { ascending: false });

    const embed = createCustomerOverviewEmbed({ profile, subscriptions, payments });

    await interaction.reply({ embeds: [embed], ephemeral: true });
}
