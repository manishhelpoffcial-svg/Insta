import { SlashCommandBuilder, EmbedBuilder } from 'discord.js';
import { getSupabase } from '../../database/supabaseClient.js';
import { CONFIG } from '../../../config/constants.js';
import { isAdministrator } from '../../interactions/buttons/approve_payment.js';
import { findUser, extendUserSubscription } from '../../admin/adminActions.js';
import { provisionSubscription } from '../../subscriptions/subscriptionManager.js';
import { formatDate } from '../../utils/validators.js';
import { EMOJIS } from '../../utils/emojis.js';

export const data = new SlashCommandBuilder()
    .setName('subscription-admin')
    .setDescription('Manage customer subscriptions, plans, duration extensions and expirations')
    // 1. add
    .addSubcommand(sub =>
        sub.setName('add')
            .setDescription('Grant or provision a subscription to a user')
            .addStringOption(opt => opt.setName('target').setDescription('Username or Discord ID').setRequired(true))
            .addIntegerOption(opt => opt.setName('days').setDescription('Duration in days').setRequired(true).setMinValue(1))
    )
    // 2. extend
    .addSubcommand(sub =>
        sub.setName('extend')
            .setDescription('Extend active subscription duration')
            .addStringOption(opt => opt.setName('target').setDescription('Username or Discord ID').setRequired(true))
            .addIntegerOption(opt => opt.setName('days').setDescription('Additional days').setRequired(true).setMinValue(1))
    )
    // 3. remove
    .addSubcommand(sub =>
        sub.setName('remove')
            .setDescription('Remove a subscription from a user')
            .addStringOption(opt => opt.setName('target').setDescription('Username or Discord ID').setRequired(true))
    )
    // 4. expire
    .addSubcommand(sub =>
        sub.setName('expire')
            .setDescription('Immediately expire a user subscription')
            .addStringOption(opt => opt.setName('target').setDescription('Username or Discord ID').setRequired(true))
    )
    // 5. list
    .addSubcommand(sub =>
        sub.setName('list')
            .setDescription('List all active subscriptions in the platform')
    )
    // 6. expired
    .addSubcommand(sub =>
        sub.setName('expired')
            .setDescription('List recently expired subscriptions')
    )
    // 7. expiring
    .addSubcommand(sub =>
        sub.setName('expiring')
            .setDescription('List subscriptions expiring within 3 days')
    );

export async function execute(interaction) {
    if (!isAdministrator(interaction.member)) {
        return interaction.reply({ content: `${EMOJIS.ERROR} You must be an administrator or owner to use this command.`, ephemeral: true });
    }

    const sub = interaction.options.getSubcommand();
    const supabase = getSupabase();
    await interaction.deferReply({ ephemeral: true });

    try {
        if (sub === 'extend') {
            const target = interaction.options.getString('target');
            const days = interaction.options.getInteger('days');
            const profile = await findUser(target);
            if (!profile) return await interaction.editReply(`${EMOJIS.ERROR} User not found: \`${target}\``);

            const updated = await extendUserSubscription(interaction.user.id, profile.id, days);
            return await interaction.editReply(`${EMOJIS.SUCCESS} Extended subscription for \`@${profile.username}\` by **${days} days**. New expiry: **${formatDate(updated.expiry_date)}**`);
        }

        if (sub === 'add') {
            const target = interaction.options.getString('target');
            const days = interaction.options.getInteger('days');
            const profile = await findUser(target);
            if (!profile) return await interaction.editReply(`${EMOJIS.ERROR} User not found: \`${target}\``);

            const { data: prod } = await supabase.from('products').select('id').limit(1).single();
            const { data: plan } = await supabase.from('plans').select('id').limit(1).single();

            const { subscription } = await provisionSubscription({
                userId: profile.id,
                productId: prod.id,
                planId: plan.id,
                durationDays: days
            });

            return await interaction.editReply(`${EMOJIS.SUBSCRIPTION} Provisioned **${days} Days** subscription for \`@${profile.username}\`. Valid until: **${formatDate(subscription.expiry_date)}**`);
        }

        if (sub === 'expire') {
            const target = interaction.options.getString('target');
            const profile = await findUser(target);
            if (!profile) return await interaction.editReply(`${EMOJIS.ERROR} User not found: \`${target}\``);

            await supabase
                .from('subscriptions')
                .update({ subscription_status: 'expired', expiry_date: new Date().toISOString() })
                .eq('user_id', profile.id);

            return await interaction.editReply(`${EMOJIS.EXPIRY} Subscriptions for \`@${profile.username}\` marked as **EXPIRED**.`);
        }

        if (sub === 'remove') {
            const target = interaction.options.getString('target');
            const profile = await findUser(target);
            if (!profile) return await interaction.editReply(`${EMOJIS.ERROR} User not found: \`${target}\``);

            await supabase.from('subscriptions').delete().eq('user_id', profile.id);
            return await interaction.editReply(`${EMOJIS.USER_REMOVE} Subscriptions removed for \`@${profile.username}\`.`);
        }

        if (sub === 'list') {
            const { data: subs } = await supabase
                .from('subscriptions')
                .select('*, profiles(username), products(product_name), plans(plan_name)')
                .eq('subscription_status', 'active')
                .gt('expiry_date', new Date().toISOString())
                .limit(15);

            if (!subs || subs.length === 0) return await interaction.editReply(`${EMOJIS.WARNING} No active subscriptions found.`);

            const embed = new EmbedBuilder()
                .setColor(CONFIG.COLORS.SUCCESS)
                .setTitle(`${EMOJIS.SUBSCRIPTION} Active Platform Subscriptions`)
                .setDescription(subs.map(s => `• ${EMOJIS.PLAN} \`@${s.profiles?.username}\` — **${s.plans?.plan_name}** — Expiry: ${formatDate(s.expiry_date)}`).join('\n'));

            return await interaction.editReply({ embeds: [embed] });
        }

        if (sub === 'expired') {
            const { data: subs } = await supabase
                .from('subscriptions')
                .select('*, profiles(username), plans(plan_name)')
                .or(`subscription_status.eq.expired,expiry_date.lte.${new Date().toISOString()}`)
                .order('expiry_date', { ascending: false })
                .limit(15);

            if (!subs || subs.length === 0) return await interaction.editReply(`${EMOJIS.WARNING} No expired subscriptions found.`);

            const embed = new EmbedBuilder()
                .setColor(CONFIG.COLORS.DANGER)
                .setTitle(`${EMOJIS.EXPIRY} Expired Subscriptions Registry`)
                .setDescription(subs.map(s => `• \`@${s.profiles?.username}\` — **${s.plans?.plan_name}** — Expired: ${formatDate(s.expiry_date)}`).join('\n'));

            return await interaction.editReply({ embeds: [embed] });
        }

        if (sub === 'expiring') {
            const inThreeDays = new Date(Date.now() + 3 * 24 * 60 * 60 * 1000).toISOString();
            const now = new Date().toISOString();

            const { data: subs } = await supabase
                .from('subscriptions')
                .select('*, profiles(username), plans(plan_name)')
                .eq('subscription_status', 'active')
                .gt('expiry_date', now)
                .lte('expiry_date', inThreeDays);

            if (!subs || subs.length === 0) return await interaction.editReply(`${EMOJIS.SUCCESS} No subscriptions expiring in the next 3 days.`);

            const embed = new EmbedBuilder()
                .setColor(CONFIG.COLORS.WARNING)
                .setTitle(`${EMOJIS.EXPIRY} Subscriptions Expiring Soon (Next 3 Days)`)
                .setDescription(subs.map(s => `• ${EMOJIS.PLAN} \`@${s.profiles?.username}\` — **${s.plans?.plan_name}** — Expires: **${formatDate(s.expiry_date)}**`).join('\n'));

            return await interaction.editReply({ embeds: [embed] });
        }
    } catch (err) {
        return await interaction.editReply(`${EMOJIS.ERROR} Error: ${err.message}`);
    }
}
