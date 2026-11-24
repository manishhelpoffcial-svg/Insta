import { getSupabase } from '../database/supabaseClient.js';
import { formatCurrency, formatDate } from '../utils/validators.js';

export async function findUser(query) {
    const supabase = getSupabase();
    const clean = query.trim().toLowerCase();

    // Check by Discord ID first
    let { data: profile } = await supabase
        .from('profiles')
        .select('*')
        .eq('discord_user_id', query.trim())
        .maybeSingle();

    if (!profile) {
        // Check by username
        ({ data: profile } = await supabase
            .from('profiles')
            .select('*')
            .ilike('username', clean)
            .maybeSingle());
    }

    if (!profile) {
        // Check by email
        ({ data: profile } = await supabase
            .from('profiles')
            .select('*')
            .eq('email', clean)
            .maybeSingle());
    }

    if (!profile) {
        // Check by UUID
        if (/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(query.trim())) {
            ({ data: profile } = await supabase
                .from('profiles')
                .select('*')
                .eq('id', query.trim())
                .maybeSingle());
        }
    }

    return profile;
}

export async function updateUserAccountStatus(adminId, userId, newStatus, reason = '') {
    const supabase = getSupabase();

    const { data: updated, error } = await supabase
        .from('profiles')
        .update({
            account_status: newStatus,
            updated_at: new Date().toISOString()
        })
        .eq('id', userId)
        .select()
        .single();

    if (error) throw error;

    await supabase.from('admin_logs').insert({
        admin_id: adminId,
        action: `SET_STATUS_${newStatus.toUpperCase()}`,
        target_user_id: userId,
        description: `Set status to ${newStatus}. Reason: ${reason || 'N/A'}`
    });

    return updated;
}

export async function extendUserSubscription(adminId, userId, additionalDays) {
    const supabase = getSupabase();
    const now = new Date();

    // Find latest active subscription
    const { data: currentSub } = await supabase
        .from('subscriptions')
        .select('*')
        .eq('user_id', userId)
        .order('expiry_date', { ascending: false })
        .limit(1)
        .maybeSingle();

    if (!currentSub) {
        throw new Error('No subscription found for this user to extend.');
    }

    const baseDate = new Date(currentSub.expiry_date) > now ? new Date(currentSub.expiry_date) : now;
    const newExpiry = new Date(baseDate.getTime() + additionalDays * 24 * 60 * 60 * 1000);

    const { data: updated, error } = await supabase
        .from('subscriptions')
        .update({
            expiry_date: newExpiry.toISOString(),
            subscription_status: 'active',
            updated_at: now.toISOString()
        })
        .eq('id', currentSub.id)
        .select('*, products(*)')
        .single();

    if (error) throw error;

    await supabase.from('admin_logs').insert({
        admin_id: adminId,
        action: 'EXTEND_SUBSCRIPTION',
        target_user_id: userId,
        description: `Extended subscription by ${additionalDays} days. New expiry: ${newExpiry.toISOString()}`
    });

    return updated;
}

export async function getSystemStatistics() {
    const supabase = getSupabase();

    // Accounts
    const { count: totalAccounts } = await supabase.from('profiles').select('*', { count: 'exact', head: true });
    const { count: activeAccounts } = await supabase.from('profiles').select('*', { count: 'exact', head: true }).eq('account_status', 'active');
    const { count: suspendedAccounts } = await supabase.from('profiles').select('*', { count: 'exact', head: true }).eq('account_status', 'suspended');

    // Subscriptions
    const { count: activeSubs } = await supabase.from('subscriptions').select('*', { count: 'exact', head: true }).eq('subscription_status', 'active').gt('expiry_date', new Date().toISOString());
    const { count: expiredSubs } = await supabase.from('subscriptions').select('*', { count: 'exact', head: true }).or(`subscription_status.eq.expired,expiry_date.lte.${new Date().toISOString()}`);

    // Payments
    const { count: totalPayments } = await supabase.from('payments').select('*', { count: 'exact', head: true });
    const { count: pendingPayments } = await supabase.from('payments').select('*', { count: 'exact', head: true }).eq('payment_status', 'pending_review');

    // Revenue
    const { data: approvedPayments } = await supabase.from('payments').select('amount').eq('payment_status', 'approved');
    const totalRevenue = approvedPayments?.reduce((sum, p) => sum + Number(p.amount), 0) || 0;

    return {
        totalAccounts: totalAccounts || 0,
        activeAccounts: activeAccounts || 0,
        suspendedAccounts: suspendedAccounts || 0,
        activeSubs: activeSubs || 0,
        expiredSubs: expiredSubs || 0,
        totalPayments: totalPayments || 0,
        pendingPayments: pendingPayments || 0,
        totalRevenue
    };
}
