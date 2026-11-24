import cron from 'node-cron';
import { getSupabase } from '../database/supabaseClient.js';
import { sendDiscordDm, sendEmail } from '../notifications/notifier.js';
import { createExpirationNoticeEmbed } from '../embeds/customerEmbeds.js';

export function startExpirationScheduler(client) {
    // Run every hour to check subscriptions
    cron.schedule('0 * * * *', async () => {
        try {
            console.log('[Scheduler] Running subscription expiration & reminder checks...');
            await checkExpiringSubscriptions(client);
            await processExpiredSubscriptions(client);
        } catch (err) {
            console.error('[Scheduler Error]', err);
        }
    });

    console.log('[Scheduler] Subscription expiration scheduler initialized (hourly).');
}

async function checkExpiringSubscriptions(client) {
    const supabase = getSupabase();
    const now = new Date();

    // Check for 3-day and 1-day reminders
    for (const days of [3, 1]) {
        const targetStart = new Date(now.getTime() + days * 24 * 60 * 60 * 1000);
        const targetEnd = new Date(targetStart.getTime() + 60 * 60 * 1000); // 1-hour window

        const { data: expiringSubs } = await supabase
            .from('subscriptions')
            .select('*, profiles(*), products(*)')
            .eq('subscription_status', 'active')
            .gte('expiry_date', targetStart.toISOString())
            .lt('expiry_date', targetEnd.toISOString());

        if (expiringSubs && expiringSubs.length > 0) {
            for (const sub of expiringSubs) {
                if (!sub.profiles) continue;

                const embed = createExpirationNoticeEmbed({
                    productName: sub.products?.product_name || 'Instagram Clone',
                    daysRemaining: days,
                    expiryDate: sub.expiry_date
                });

                if (sub.profiles.discord_user_id) {
                    await sendDiscordDm(client, sub.profiles.discord_user_id, embed);
                }

                if (sub.profiles.email) {
                    await sendEmail({
                        to: sub.profiles.email,
                        subject: `Reminder: Your subscription expires in ${days} day${days > 1 ? 's' : ''}`,
                        htmlText: `<p>Hello <b>${sub.profiles.username}</b>,</p><p>Your subscription to <b>${sub.products?.product_name}</b> will expire in ${days} day(s) on ${new Date(sub.expiry_date).toLocaleDateString()}. Use /purchase in Discord to renew seamlessly.</p>`
                    });
                }
            }
        }
    }
}

async function processExpiredSubscriptions(client) {
    const supabase = getSupabase();
    const now = new Date().toISOString();

    // Find subscriptions that have passed expiry and are still marked 'active'
    const { data: expiredSubs } = await supabase
        .from('subscriptions')
        .select('*, profiles(*), products(*)')
        .eq('subscription_status', 'active')
        .lte('expiry_date', now);

    if (!expiredSubs || expiredSubs.length === 0) return;

    for (const sub of expiredSubs) {
        // 1. Mark subscription as expired (NEVER delete user auth or profile)
        await supabase
            .from('subscriptions')
            .update({
                subscription_status: 'expired',
                updated_at: new Date().toISOString()
            })
            .eq('id', sub.id);

        // 2. Invalidate active device sessions
        await supabase
            .from('device_sessions')
            .update({ session_status: 'expired' })
            .eq('user_id', sub.user_id);

        // 3. Notify user
        if (sub.profiles) {
            const embed = createExpirationNoticeEmbed({
                productName: sub.products?.product_name || 'Instagram Clone',
                daysRemaining: 0,
                expiryDate: sub.expiry_date
            });

            if (sub.profiles.discord_user_id) {
                await sendDiscordDm(client, sub.profiles.discord_user_id, embed);
            }

            if (sub.profiles.email) {
                await sendEmail({
                    to: sub.profiles.email,
                    subject: `Subscription Expired: ${sub.products?.product_name}`,
                    htmlText: `<p>Hello <b>${sub.profiles.username}</b>,</p><p>Your subscription to <b>${sub.products?.product_name}</b> has expired. To restore your access to the mobile app, please renew by using /purchase in Discord.</p>`
                });
            }
        }
    }
}
