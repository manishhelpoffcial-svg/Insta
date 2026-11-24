import { SlashCommandBuilder, EmbedBuilder } from 'discord.js';
import { getSupabase } from '../../database/supabaseClient.js';
import { CONFIG } from '../../../config/constants.js';

export const data = new SlashCommandBuilder()
    .setName('change-password')
    .setDescription('Change your InSightX app login password')
    .addStringOption(opt =>
        opt.setName('new_password')
            .setDescription('Your new app login password (minimum 6 characters)')
            .setRequired(true)
    );

export async function changeUserPassword(discordUserId, newPassword) {
    if (!newPassword || newPassword.length < 6) {
        throw new Error('Password must be at least 6 characters long.');
    }

    const supabase = getSupabase();
    const { data: profile } = await supabase
        .from('profiles')
        .select('*')
        .eq('discord_user_id', discordUserId)
        .maybeSingle();

    if (!profile) {
        throw new Error('No linked account found for your Discord user.');
    }

    // Update password via Supabase Auth Admin API
    const { error: authErr } = await supabase.auth.admin.updateUserById(profile.id, {
        password: newPassword
    });

    if (authErr) throw authErr;

    return profile;
}

export async function linkAccount(discordUserId, username) {
    const supabase = getSupabase();
    const cleanUser = username.trim().toLowerCase().replace('@', '');

    const { data: profile } = await supabase
        .from('profiles')
        .select('*')
        .ilike('username', cleanUser)
        .maybeSingle();

    if (!profile) {
        throw new Error(`Account @${cleanUser} does not exist in the database.`);
    }

    if (profile.discord_user_id && profile.discord_user_id !== discordUserId) {
        throw new Error(`Account @${cleanUser} is already linked to another Discord user.`);
    }

    const { error } = await supabase
        .from('profiles')
        .update({ discord_user_id: discordUserId, updated_at: new Date().toISOString() })
        .eq('id', profile.id);

    if (error) throw error;
    return profile;
}

export async function unlinkAccount(discordUserId) {
    const supabase = getSupabase();
    const { data: profile } = await supabase
        .from('profiles')
        .select('*')
        .eq('discord_user_id', discordUserId)
        .maybeSingle();

    if (!profile) {
        throw new Error('You do not have any linked InSightX account.');
    }

    const { error } = await supabase
        .from('profiles')
        .update({ discord_user_id: null, updated_at: new Date().toISOString() })
        .eq('id', profile.id);

    if (error) throw error;
    return profile;
}

export async function execute(interaction) {
    const newPass = interaction.options.getString('new_password');
    await interaction.deferReply({ ephemeral: true });

    try {
        const profile = await changeUserPassword(interaction.user.id, newPass);
        const embed = new EmbedBuilder()
            .setColor(CONFIG.COLORS.SUCCESS)
            .setTitle('🔑 Password Successfully Updated')
            .setDescription(
                `Your login password for **@${profile.username}** has been updated successfully.\n\n` +
                'You can now use your new password immediately to log into the Instagram Clone app.'
            )
            .setFooter({ text: 'InSightX HQ Direct Security' });

        await interaction.editReply({ embeds: [embed] });
    } catch (err) {
        await interaction.editReply({ content: `❌ ${err.message}` });
    }
}
