import { getSupabase } from '../database/supabaseClient.js';

export async function uploadScreenshotToStorage({ paymentReference, attachmentUrl, contentType }) {
    const supabase = getSupabase();

    // 1. Fetch file buffer from Discord CDN
    const response = await fetch(attachmentUrl);
    if (!response.ok) {
        throw new Error(`Failed to download screenshot from Discord: ${response.statusText}`);
    }

    const arrayBuffer = await response.arrayBuffer();
    const buffer = Buffer.from(arrayBuffer);

    // 2. Determine extension
    let ext = 'jpg';
    if (contentType?.includes('png')) ext = 'png';
    else if (contentType?.includes('webp')) ext = 'webp';

    const filePath = `receipts/${paymentReference}.${ext}`;

    // 3. Upload to private Supabase Storage bucket
    const { error: uploadError } = await supabase.storage
        .from('payment-screenshots')
        .upload(filePath, buffer, {
            contentType: contentType || 'image/jpeg',
            upsert: true
        });

    if (uploadError) {
        throw new Error(`Supabase Storage upload error: ${uploadError.message}`);
    }

    return filePath;
}

export async function getSignedScreenshotUrl(filePath) {
    if (!filePath) return null;
    const supabase = getSupabase();
    // Generate temporary 1-hour signed URL for administrators
    const { data, error } = await supabase.storage
        .from('payment-screenshots')
        .createSignedUrl(filePath, 3600);

    if (error) {
        console.error('Error creating signed URL for screenshot:', error);
        return null;
    }
    return data?.signedUrl || null;
}
