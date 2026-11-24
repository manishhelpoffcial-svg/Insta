import { createClient } from '@supabase/supabase-js';
import { CONFIG } from '../../config/constants.js';

let supabaseInstance = null;

export function getSupabase() {
    if (!supabaseInstance) {
        if (!CONFIG.SUPABASE_URL || !CONFIG.SUPABASE_SERVICE_ROLE_KEY) {
            throw new Error('Supabase URL or Service Role Key missing in environment.');
        }

        supabaseInstance = createClient(CONFIG.SUPABASE_URL, CONFIG.SUPABASE_SERVICE_ROLE_KEY, {
            auth: {
                autoRefreshToken: false,
                persistSession: false
            }
        });
    }
    return supabaseInstance;
}
