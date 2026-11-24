import { CONFIG } from '../../config/constants.js';

/**
 * Generates an official UPI Intent URI for manual customer payments.
 * Spec: upi://pay?pa={UPI_ID}&pn={MERCHANT_NAME}&am={AMOUNT}&cu=INR&tn={REFERENCE}
 */
export function generateUpiIntentLink({ amount, reference }) {
    const pa = encodeURIComponent(CONFIG.UPI_ID.trim());
    const pn = encodeURIComponent(CONFIG.MERCHANT_NAME.trim());
    const am = encodeURIComponent(Number(amount).toFixed(2));
    const tn = encodeURIComponent(`Sub ${reference}`.slice(0, 30));

    return `upi://pay?pa=${pa}&pn=${pn}&am=${am}&cu=INR&tn=${tn}`;
}
