# ⚡ InSightX HQ — Discord Management & Payment Bot

The official **InSightX HQ** management system for Instagram Clone, Creator Tools, subscriptions, and UPI payments powered by a shared Supabase database.

## 🌟 Capabilities & Architecture

- **Dual Interaction Engine**: Full support for both **Slash Commands** (`/`) and **Prefix Commands** (`.`).
- **Zero Email Requirement**: Pure in-Discord provisioning and direct password generation with DM delivery.
- **Dynamic UPI Payment Gateway**: Intent link generation with merchant verification remarks and automated screenshot intake.
- **Private Admin Verification**: Queue logs channel with instant `[Approve]` and `[Reject]` action buttons.
- **Supabase Auth Integration**: Synchronized profiles, active/suspended/banned status enforcement, and automated expiry scheduling.

---

## 📋 Command List

### Customer Commands (Slash & Prefix)
| Command | Prefix | Description |
| :--- | :--- | :--- |
| `/purchase` | `.purchase` | Browse dynamic products & plans, enter username, and generate UPI payment link |
| `/account` | `.account` | View personal subscription validity, username, and account status |
| `/ping` | `.ping` | View bot latency, WebSocket health, and Supabase database connection speed |
| `/help` | `.help` | Open the InSightX HQ command center and documentation |

### Administrator Commands (Permission: Administrator or Admin Role)
| Command | Prefix Alias | Description |
| :--- | :--- | :--- |
| `/admin userinfo <target>` | `.userinfo <target>` | View comprehensive customer profile, subscriptions, and payment history |
| `/admin search <query>` | `.search <query>` | Search accounts by username, email, or UUID keyword |
| `/admin stats` | `.stats` | View real-time platform metrics, active subscriptions, and revenue |
| `/admin extend <target> <days>`| `.extend <target> <days>`| Grant extra subscription duration to an existing user |
| `/admin activate <target>` | `.activate <target>` | Reactivate a suspended or expired account |
| `/admin suspend <target>` | `.suspend <target> [reason]` | Temporarily block app access for policy violations |
| `/admin ban <target>` | `.ban <target> [reason]` | Permanently ban user account |
| `/admin createplan ...` | `.createplan ...` | Dynamically create new subscription durations and prices in Supabase |

---

## 🚀 Setup & Launch

1. **Install dependencies**:
   ```bash
   pnpm install
   ```

2. **Configure `.env`**:
   Fill in credentials in [.env](file:///sdcard/insight%20bot/.env):
   - `SUPABASE_URL`
   - `SUPABASE_PUBLISHABLE_KEY`
   - `SUPABASE_SERVICE_ROLE_KEY`
   - `SUPABASE_JWT_SECRET`
   - `DISCORD_BOT_TOKEN`, `DISCORD_CLIENT_ID`, `DISCORD_GUILD_ID`
   - `ADMIN_ROLE_ID`, `PAYMENT_LOG_CHANNEL_ID`
   - `UPI_ID`, `MERCHANT_NAME`

3. **Run Migrations**:
   Execute `migrations/001_initial_schema.sql` in Supabase SQL Editor.

4. **Start InSightX HQ**:
   ```bash
   pnpm start
   ```
