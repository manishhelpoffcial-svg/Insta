import { getSupabase } from '../database/supabaseClient.js';

/**
 * Calculates start and expiry date for a subscription.
 * If user has an existing active subscription, calculates expiry from the existing expiry date.
 * If expired or new, calculates expiry from now.
 */
export async function calculateSubscriptionDates(userId, productId, durationDays) {
    const supabase = getSupabase();
    const now = new Date();

    const { data: existingSub } = await supabase
        .from('subscriptions')
        .select('id, expiry_date, subscription_status')
        .eq('user_id', userId)
        .eq('product_id', productId)
        .eq('subscription_status', 'active')
        .gt('expiry_date', now.toISOString())
        .order('expiry_date', { ascending: false })
        .limit(1)
        .maybeSingle();

    let startDate = now;
    let baseExpiryDate = now;

    if (existingSub && existingSub.expiry_date) {
        const currentExpiry = new Date(existingSub.expiry_date);
        if (currentExpiry > now) {
            baseExpiryDate = currentExpiry;
        }
    }

    const expiryDate = new Date(baseExpiryDate.getTime() + durationDays * 24 * 60 * 60 * 1000);

    return {
        existingSubscriptionId: existingSub?.id || null,
        startDate: startDate.toISOString(),
        expiryDate: expiryDate.toISOString(),
    };
}

export async function provisionSubscription({ userId, productId, planId, durationDays }) {
    const supabase = getSupabase();
    const { existingSubscriptionId, startDate, expiryDate } = await calculateSubscriptionDates(userId, productId, durationDays);

    if (existingSubscriptionId) {
        // Extend existing subscription
        const { data, error } = await supabase
            .from('subscriptions')
            .update({
                plan_id: planId,
                expiry_date: expiryDate,
                subscription_status: 'active',
                updated_at: new Date().toISOString()
            })
            .eq('id', existingSubscriptionId)
            .select()
            .single();

        if (error) throw error;
        return { subscription: data, isRenewal: true };
    } else {
        // Create new subscription record
        const { data, error } = await supabase
            .from('subscriptions')
            .insert({
                user_id: userId,
                product_id: productId,
                plan_id: planId,
                start_date: startDate,
                expiry_date: expiryDate,
                subscription_status: 'active'
            })
            .select()
            .single();

        if (error) throw error;
        return { subscription: data, isRenewal: false };
    }
}
