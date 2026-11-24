# Instagram Clone: Supabase Auth & Discord Account Management System

This document explains the end-to-end architecture, database schema, Discord bot deployment, and Android application integration.

---

## 1. System Architecture

```
                     ┌──────────────────────────────────────────────┐
                     │          SHARED SUPABASE PROJECT             │
                     │  • Auth (GoTrue)  • PostgreSQL + RLS         │
                     │  • Private Storage (payment-screenshots)     │
                     │  • Secure RPCs (resolve_username, verify)    │
                     └───────────────┬──────────────────────────────┘
                                     │
           ┌─────────────────────────┴────────────────────────┐
           ▼                                                  ▼
┌───────────────────────────────┐              ┌───────────────────────────────┐
│     INSTAGRAM CLONE APP       │              │      DISCORD BOT SERVER       │
│  (Kotlin + Jetpack Compose)   │              │   (Node.js / Discord.js v14)  │
│  • Email / Username Login     │              │  • /purchase Command          │
│  • Registration & Pass Reset  │              │  • UPI Payment Link Intent    │
│  • Subscription Gate Check    │              │  • Manual Screenshot Review   │
│  • Full Instagram UI & Reels  │              │  • Admin Approval & Expiry    │
└───────────────────────────────┘              └───────────────────────────────┘
```

---

## 2. Supabase Database Configuration

1. Log into your [Supabase Dashboard](https://supabase.com).
2. Go to **SQL Editor** → **New query**.
3. Paste and run the entire migration script located at:
   [`discord-bot/migrations/001_initial_schema.sql`](file:///data/user/0/ai.antigem/files/home/.gemini/antigravity-cli/scratch/Insta/discord-bot/migrations/001_initial_schema.sql)
4. This script sets up:
   - **8 Core Tables**: `profiles`, `products`, `plans`, `subscriptions`, `payments`, `admin_logs`, `notifications`, `device_sessions`.
   - **Row Level Security (RLS)**: Protects user privacy (emails are never queryable publicly).
   - **Private Storage Bucket**: `payment-screenshots` for manual payment proof.
   - **Secure Database Functions**:
     - `resolve_username_to_email(p_username)`: Securely looks up the email for username login without leaking tables.
     - `verify_user_access(p_user_id)`: Checks account status and validates active subscription expiry dates.
     - `check_and_expire_subscriptions()`: Server-side automatic expiry processor.
   - **Initial Seed Data**: Product `Insight Editor` and 5 dynamic duration plans (7, 30, 90, 180, and 365 Days).

---

## 3. Discord Bot Deployment (`/discord-bot`)

1. Open a terminal and navigate to the bot directory:
   ```bash
   cd discord-bot
   pnpm install
   ```

2. Configure environment variables in `discord-bot/.env`:
   ```env
   # Shared Supabase Configuration
   SUPABASE_URL=https://your-supabase-project.supabase.co
   SUPABASE_PUBLISHABLE_KEY=your-supabase-publishable-key
   SUPABASE_SERVICE_ROLE_KEY=your-supabase-service-role-key

   # Discord Bot Configuration
   DISCORD_BOT_TOKEN=your-discord-bot-token
   DISCORD_CLIENT_ID=your-discord-application-client-id
   DISCORD_GUILD_ID=your-discord-server-id

   # Administration
   ADMIN_ROLE_ID=your-admin-role-id
   PAYMENT_LOG_CHANNEL_ID=your-private-logs-channel-id

   # UPI Payment Details
   UPI_ID=yourbusiness@upi
   MERCHANT_NAME=Instagram Pro Tools

   # Optional SMTP for Transactional Emails
   SMTP_HOST=smtp.gmail.com
   SMTP_PORT=587
   SMTP_USER=
   SMTP_PASS=
   FROM_EMAIL=support@instagramclone.app
   ```

3. Start the bot:
   ```bash
   pnpm start
   ```

### Customer Flow (`/purchase`)
1. User types `/purchase` in Discord.
2. Selects **Insight Editor** from the product dropdown.
3. Selects a duration plan (7, 30, 90, 180, 365 Days).
4. Enters their email address and optional preferred username via Modal.
5. Receives a payment card with the generated UPI Intent URL and unique payment reference.
6. Clicks **Upload Payment Screenshot** to submit proof.
7. The bot uploads the screenshot to the private Supabase Storage bucket and creates a `pending_review` record.

### Administrator Flow
1. Administrators receive an embed in `PAYMENT_LOG_CHANNEL_ID` with the screenshot, transaction reference, amount, and customer email.
2. Clicking **Approve Payment**:
   - Creates the user in Supabase Auth (or associates with existing account).
   - Provisions/extends the subscription with calculated expiry.
   - Sends the customer an account activation/password setup link via Discord DM and email.
   - Logs the action in `admin_logs`.
3. Clicking **Reject Payment**:
   - Opens a modal asking for the rejection reason.
   - Updates payment to `rejected` and sends the reason to the customer.

### Administrator Slash Commands
- `/admin userinfo <target>`: View user details, subscriptions, and payment history.
- `/admin search <query>`: Search accounts by username or email.
- `/admin activate <target>` / `/admin suspend <target>` / `/admin ban <target>`: Modify account status.
- `/admin extend <target> <days>`: Grant additional days to an active subscription.
- `/admin createplan <product> <plan_name> <days> <price>`: Dynamically add plans without restarting the bot.
- `/admin statistics`: View total accounts, active subscriptions, and revenue.
- `/admin logs`: Inspect recent audit logs.

---

## 4. Android Instagram Clone Integration

1. Configure `.env` in the root repository folder:
   ```env
   SUPABASE_URL=https://your-supabase-project.supabase.co
   SUPABASE_PUBLISHABLE_KEY=your-supabase-publishable-key
   ```
2. Build and launch the Android application:
   ```bash
   ./gradlew assembleDebug
   ```
3. **Authentication & Access Control**:
   - **Login**: Supports both **Email + Password** and **Username + Password** (using the secure `resolve_username_to_email` RPC).
   - **Registration**: Allows signing up with email, username, and password.
   - **Forgot Password**: Sends password recovery link.
   - **Subscription Verification**: Upon login, calls `verify_user_access`.
     - If the account has an active, unexpired subscription, the full Instagram Clone app opens with all existing features (Home Feed, Reels, Stories, DMs, Profile, Insights).
     - If the account is pending, expired, suspended, or banned, the **Access Gated Screen** is shown, displaying the reason, expiry date, and a direct button to renew via Discord (`/purchase`).
   - **Log Out**: Accessible from the Profile menu modal.
   - **Offline Demo**: Includes an optional guest demo mode if developers need to test without Supabase credentials.
