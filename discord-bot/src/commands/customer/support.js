import { SlashCommandBuilder, EmbedBuilder, ActionRowBuilder, ButtonBuilder, ButtonStyle } from 'discord.js';
import { getSupabase } from '../../database/supabaseClient.js';
import { CONFIG } from '../../../config/constants.js';
import { formatDate } from '../../utils/validators.js';
import { EMOJIS } from '../../utils/emojis.js';

export const data = new SlashCommandBuilder()
    .setName('support')
    .setDescription('InSightX HQ Customer Support & Ticket System')
    .addSubcommand(sub =>
        sub.setName('create')
            .setDescription('Create a new customer support ticket')
            .addStringOption(opt => opt.setName('subject').setDescription('Brief description of your issue').setRequired(true))
    )
    .addSubcommand(sub =>
        sub.setName('list')
            .setDescription('List all support tickets (Admin sees all, customer sees their own)')
            .addStringOption(opt => opt.setName('status').setDescription('Filter by status').addChoices(
                { name: 'Open', value: 'open' },
                { name: 'Closed', value: 'closed' },
                { name: 'All', value: 'all' }
            ))
    )
    .addSubcommand(sub =>
        sub.setName('reply')
            .setDescription('Reply to an active support ticket')
            .addIntegerOption(opt => opt.setName('ticket_number').setDescription('Ticket ID number').setRequired(true))
            .addStringOption(opt => opt.setName('message').setDescription('Your response message').setRequired(true))
    )
    .addSubcommand(sub =>
        sub.setName('close')
            .setDescription('Close an active support ticket')
            .addIntegerOption(opt => opt.setName('ticket_number').setDescription('Ticket ID number').setRequired(true))
    )
    .addSubcommand(sub =>
        sub.setName('reopen')
            .setDescription('Reopen a closed support ticket')
            .addIntegerOption(opt => opt.setName('ticket_number').setDescription('Ticket ID number').setRequired(true))
    )
    .addSubcommand(sub =>
        sub.setName('assign')
            .setDescription('Assign a ticket to an administrator (Admin only)')
            .addIntegerOption(opt => opt.setName('ticket_number').setDescription('Ticket ID number').setRequired(true))
            .addUserOption(opt => opt.setName('staff').setDescription('Staff member to assign').setRequired(true))
    );

export async function createTicket(discordUserId, subject) {
    const supabase = getSupabase();

    const { data: profile } = await supabase
        .from('profiles')
        .select('id')
        .eq('discord_user_id', discordUserId)
        .maybeSingle();

    const { data: ticket, error } = await supabase
        .from('support_tickets')
        .insert({
            user_id: profile?.id || null,
            discord_user_id: discordUserId,
            subject: subject.slice(0, 200),
            status: 'open'
        })
        .select()
        .single();

    if (error) throw error;
    return ticket;
}

export async function addTicketMessage(ticketNumber, senderId, senderName, isStaff, message) {
    const supabase = getSupabase();

    const { data: ticket } = await supabase
        .from('support_tickets')
        .select('*')
        .eq('ticket_number', ticketNumber)
        .single();

    if (!ticket) throw new Error(`Ticket #${ticketNumber} not found.`);

    const { error: msgErr } = await supabase
        .from('ticket_messages')
        .insert({
            ticket_id: ticket.id,
            sender_id: senderId,
            sender_name: senderName,
            is_staff: isStaff,
            message: message
        });

    if (msgErr) throw msgErr;

    await supabase
        .from('support_tickets')
        .update({ updated_at: new Date().toISOString() })
        .eq('id', ticket.id);

    return ticket;
}

export async function execute(interaction) {
    const sub = interaction.options.getSubcommand();
    const supabase = getSupabase();
    await interaction.deferReply({ ephemeral: true });

    try {
        if (sub === 'create') {
            const subject = interaction.options.getString('subject');
            const ticket = await createTicket(interaction.user.id, subject);

            const embed = new EmbedBuilder()
                .setColor(CONFIG.COLORS.SUCCESS)
                .setTitle(`${EMOJIS.TICKET} Support Ticket Created — #${ticket.ticket_number}`)
                .setDescription(
                    `Your inquiry has been submitted to the administration team.\n\n` +
                    `• **Ticket ID:** \`#${ticket.ticket_number}\`\n` +
                    `• **Subject:** ${subject}\n` +
                    `• **Status:** \`OPEN\`\n\n` +
                    `To reply or add information to this ticket, run:\n` +
                    `\`/support reply ticket_number:${ticket.ticket_number} message:your message\``
                )
                .setFooter({ text: 'InSightX HQ Customer Support Desk' })
                .setTimestamp();

            return await interaction.editReply({ embeds: [embed] });
        }

        if (sub === 'reply') {
            const num = interaction.options.getInteger('ticket_number');
            const msg = interaction.options.getString('message');
            const isStaff = interaction.member?.permissions?.has('Administrator') || false;

            const ticket = await addTicketMessage(num, interaction.user.id, interaction.user.username, isStaff, msg);

            return await interaction.editReply({
                content: `✅ Reply submitted to **Ticket #${num}** (${isStaff ? 'Staff Response' : 'User Response'}).`
            });
        }

        if (sub === 'close') {
            const num = interaction.options.getInteger('ticket_number');
            const { error } = await supabase
                .from('support_tickets')
                .update({ status: 'closed', updated_at: new Date().toISOString() })
                .eq('ticket_number', num);

            if (error) throw error;
            return await interaction.editReply({ content: `🔒 Ticket **#${num}** has been marked as **CLOSED**.` });
        }

        if (sub === 'reopen') {
            const num = interaction.options.getInteger('ticket_number');
            const { error } = await supabase
                .from('support_tickets')
                .update({ status: 'reopened', updated_at: new Date().toISOString() })
                .eq('ticket_number', num);

            if (error) throw error;
            return await interaction.editReply({ content: `🔓 Ticket **#${num}** has been marked as **REOPENED**.` });
        }

        if (sub === 'assign') {
            const num = interaction.options.getInteger('ticket_number');
            const staff = interaction.options.getUser('staff');

            const { error } = await supabase
                .from('support_tickets')
                .update({ assigned_to: staff.id, status: 'in_progress', updated_at: new Date().toISOString() })
                .eq('ticket_number', num);

            if (error) throw error;
            return await interaction.editReply({ content: `👤 Ticket **#${num}** assigned to <@${staff.id}>.` });
        }

        if (sub === 'list') {
            const statusFilter = interaction.options.getString('status') || 'all';
            let query = supabase.from('support_tickets').select('*').order('created_at', { ascending: false }).limit(10);

            const isStaff = interaction.member?.permissions?.has('Administrator') || false;
            if (!isStaff) {
                query = query.eq('discord_user_id', interaction.user.id);
            }
            if (statusFilter !== 'all') {
                query = query.eq('status', statusFilter);
            }

            const { data: tickets } = await query;

            if (!tickets || tickets.length === 0) {
                return await interaction.editReply({ content: '📭 No support tickets found matching criteria.' });
            }

            const embed = new EmbedBuilder()
                .setColor(CONFIG.COLORS.INFO)
                .setTitle('🎫 Support Tickets Queue')
                .setDescription(
                    tickets.map(t =>
                        `**#${t.ticket_number}** — \`${t.status.toUpperCase()}\` — **${t.subject}** (<@${t.discord_user_id}> | ${formatDate(t.created_at)})`
                    ).join('\n')
                )
                .setFooter({ text: 'InSightX HQ Support Desk' });

            return await interaction.editReply({ embeds: [embed] });
        }
    } catch (err) {
        return await interaction.editReply({ content: `❌ Error: ${err.message}` });
    }
}
