import nodemailer from 'nodemailer';
import { CONFIG } from '../../config/constants.js';

let mailerTransport = null;

function getMailer() {
    if (!mailerTransport && CONFIG.SMTP_HOST && CONFIG.SMTP_USER && CONFIG.SMTP_PASS) {
        mailerTransport = nodemailer.createTransport({
            host: CONFIG.SMTP_HOST,
            port: CONFIG.SMTP_PORT,
            secure: CONFIG.SMTP_PORT === 465,
            auth: {
                user: CONFIG.SMTP_USER,
                pass: CONFIG.SMTP_PASS
            }
        });
    }
    return mailerTransport;
}

export async function sendEmail({ to, subject, htmlText, plainText }) {
    const mailer = getMailer();
    if (!mailer) {
        console.log(`[SMTP Not Configured] Simulating email to ${to}: "${subject}"`);
        return false;
    }

    try {
        await mailer.sendMail({
            from: `"${CONFIG.MERCHANT_NAME}" <${CONFIG.FROM_EMAIL}>`,
            to,
            subject,
            text: plainText || htmlText.replace(/<[^>]+>/g, ''),
            html: htmlText
        });
        return true;
    } catch (err) {
        console.error(`Failed to send email to ${to}:`, err.message);
        return false;
    }
}

export async function sendDiscordDm(client, discordUserId, embed, components = []) {
    try {
        const user = await client.users.fetch(discordUserId);
        if (!user) return false;
        await user.send({ embeds: [embed], components });
        return true;
    } catch (err) {
        console.warn(`Could not send Discord DM to ${discordUserId}: ${err.message}`);
        return false;
    }
}
