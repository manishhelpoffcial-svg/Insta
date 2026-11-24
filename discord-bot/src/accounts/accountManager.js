import { getSupabase } from '../database/supabaseClient.js';

/**
 * Ensures a Supabase Auth user and corresponding profile exist.
 * Uses username and direct password credentials. No email or SMTP required.
 */
export async function getOrCreateAccount({ email, username, password, discordUserId }) {
    const supabase = getSupabase();

    const cleanUsername = (username || (email ? email.split('@')[0] : ''))
        .trim().replace(/[^a-zA-Z0-9_.]/g, '').toLowerCase() || `user_${Date.now().toString(36)}`;
    const internalEmail = `${cleanUsername}@insta.app`;

    const plainPassword = password || `Insta_${Math.random().toString(36).slice(-6)}!`;

    // 1. Check if profile exists by username or discordUserId
    let existingProfile = null;
    try {
        const res = await supabase
            .from('profiles')
            .select('*')
            .or(`username.eq.${cleanUsername},discord_user_id.eq.${discordUserId}`)
            .maybeSingle();
        existingProfile = res.data;
    } catch (e) {
        console.warn('[Supabase Warning] Could not read existing profile:', e.message);
    }

    if (existingProfile) {
        if (!existingProfile.discord_user_id && discordUserId) {
            await supabase
                .from('profiles')
                .update({ discord_user_id: discordUserId })
                .eq('id', existingProfile.id);
        }

        // Update password if customer requested custom password
        if (password) {
            await supabase.auth.admin.updateUserById(existingProfile.id, {
                password: password
            });
        }

        return {
            user: { id: existingProfile.id, email: existingProfile.email },
            profile: existingProfile,
            isNewUser: false,
            plainPassword: password || null
        };
    }

    // 2. User does not exist in profiles, check auth.users directly
    const { data: authList } = await supabase.auth.admin.listUsers();
    let authUser = authList?.users?.find(u => u.email?.toLowerCase() === internalEmail);

    if (!authUser) {
        // Create user in Supabase Auth with direct password and instant email confirmation
        const { data: createData, error: createError } = await supabase.auth.admin.createUser({
            email: internalEmail,
            password: plainPassword,
            email_confirm: true,
            user_metadata: {
                username: cleanUsername,
                discord_user_id: discordUserId
            }
        });

        if (createError) throw new Error(`Supabase Auth creation error: ${createError.message}`);
        authUser = createData.user;
    } else if (password) {
        await supabase.auth.admin.updateUserById(authUser.id, {
            password: password
        });
    }

    // 3. Upsert into public.profiles (with fallback if table permissions pending)
    let newProfile = null;
    try {
        const { data: upsertData, error: profileError } = await supabase
            .from('profiles')
            .upsert({
                id: authUser.id,
                username: cleanUsername,
                email: internalEmail,
                discord_user_id: discordUserId,
                account_status: 'active',
                updated_at: new Date().toISOString()
            })
            .select()
            .single();

        if (profileError) {
            console.warn('[Supabase Warning] Profiles upsert error:', profileError.message);
        } else {
            newProfile = upsertData;
        }
    } catch (e) {
        console.warn('[Supabase Warning] Profiles table inaccessible:', e.message);
    }

    if (!newProfile) {
        newProfile = {
            id: authUser.id,
            username: cleanUsername,
            email: internalEmail,
            discord_user_id: discordUserId,
            account_status: 'active'
        };
    }

    return {
        user: authUser,
        profile: newProfile,
        isNewUser: true,
        plainPassword: plainPassword
    };
}
