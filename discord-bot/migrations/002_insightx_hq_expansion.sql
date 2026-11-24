-- =====================================================================
-- InSightX HQ Database Expansion Migration (002_insightx_hq_expansion.sql)
-- =====================================================================

-- 1. BOT CONFIGURATION TABLE (for owner settings, prefix, maintenance mode)
CREATE TABLE IF NOT EXISTS public.bot_config (
    key TEXT PRIMARY KEY,
    value JSONB NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

-- Seed default bot configuration
INSERT INTO public.bot_config (key, value)
VALUES 
    ('prefix', '"."'),
    ('maintenance_mode', 'false'),
    ('owner_id', '""'),
    ('admin_roles', '[]')
ON CONFLICT (key) DO NOTHING;

-- 2. SUPPORT TICKETS TABLE
CREATE TABLE IF NOT EXISTS public.support_tickets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_number SERIAL UNIQUE,
    user_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    discord_user_id TEXT NOT NULL,
    discord_channel_id TEXT,
    subject TEXT NOT NULL,
    status TEXT DEFAULT 'open' NOT NULL CHECK (status IN ('open', 'in_progress', 'closed', 'reopened')),
    priority TEXT DEFAULT 'normal' NOT NULL CHECK (priority IN ('low', 'normal', 'high', 'urgent')),
    assigned_to TEXT,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

-- 3. TICKET MESSAGES TABLE
CREATE TABLE IF NOT EXISTS public.ticket_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID NOT NULL REFERENCES public.support_tickets(id) ON DELETE CASCADE,
    sender_id TEXT NOT NULL,
    sender_name TEXT NOT NULL,
    is_staff BOOLEAN DEFAULT FALSE NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

-- 4. USER USAGE AND LIMITS TABLE
CREATE TABLE IF NOT EXISTS public.user_usage (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID UNIQUE NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    daily_count INTEGER DEFAULT 0 NOT NULL,
    monthly_count INTEGER DEFAULT 0 NOT NULL,
    custom_limit INTEGER DEFAULT 100 NOT NULL,
    last_reset TIMESTAMPTZ DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

-- 5. USAGE LOGS TABLE
CREATE TABLE IF NOT EXISTS public.usage_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    action TEXT NOT NULL,
    details TEXT,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

-- 6. SYSTEM ERROR LOGS TABLE
CREATE TABLE IF NOT EXISTS public.system_errors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    error_type TEXT NOT NULL,
    message TEXT NOT NULL,
    stack TEXT,
    context JSONB,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

-- 7. SEED STANDARD PRODUCT & THE 6 SPECIFIED SUBSCRIPTION PLANS
DO $$
DECLARE
    v_prod_id UUID;
BEGIN
    -- Ensure "Insight Editor" or "Instagram Clone" product exists
    INSERT INTO public.products (product_name, description, is_active)
    VALUES ('Insight Editor', 'Full Instagram Creator insights, scraper & editing tools', true)
    ON CONFLICT (product_name) DO UPDATE SET is_active = true
    RETURNING id INTO v_prod_id;

    -- If returning was empty because no update occurred, fetch it
    IF v_prod_id IS NULL THEN
        SELECT id INTO v_prod_id FROM public.products WHERE product_name = 'Insight Editor' LIMIT 1;
    END IF;

    -- Seed 6 Standard Plans:
    -- Weekly — 7 Days — ₹199
    INSERT INTO public.plans (product_id, plan_name, duration_days, price, description, is_active)
    VALUES (v_prod_id, 'Weekly Plan', 7, 199.00, '7 Days Access to Insight Editor', true)
    ON CONFLICT DO NOTHING;

    -- Monthly — 30 Days — ₹499
    INSERT INTO public.plans (product_id, plan_name, duration_days, price, description, is_active)
    VALUES (v_prod_id, 'Monthly Plan', 30, 499.00, '30 Days Access to Insight Editor', true)
    ON CONFLICT DO NOTHING;

    -- Quarterly — 90 Days — ₹799
    INSERT INTO public.plans (product_id, plan_name, duration_days, price, description, is_active)
    VALUES (v_prod_id, 'Quarterly Plan', 90, 799.00, '90 Days Access to Insight Editor', true)
    ON CONFLICT DO NOTHING;

    -- Half-Yearly — 180 Days — ₹1,199
    INSERT INTO public.plans (product_id, plan_name, duration_days, price, description, is_active)
    VALUES (v_prod_id, 'Half-Yearly Plan', 180, 1199.00, '180 Days Access to Insight Editor', true)
    ON CONFLICT DO NOTHING;

    -- Yearly — 365 Days — ₹1,599
    INSERT INTO public.plans (product_id, plan_name, duration_days, price, description, is_active)
    VALUES (v_prod_id, 'Yearly Plan', 365, 1599.00, '365 Days Access to Insight Editor', true)
    ON CONFLICT DO NOTHING;

    -- Lifetime — Unlimited Duration (36,500 Days / 100 Years) — ₹1,999
    INSERT INTO public.plans (product_id, plan_name, duration_days, price, description, is_active)
    VALUES (v_prod_id, 'Lifetime Plan', 36500, 1999.00, 'Unlimited Lifetime Access to Insight Editor', true)
    ON CONFLICT DO NOTHING;
END $$;

-- 8. INDEXES FOR HIGH-THROUGHPUT SEARCH & LOOKUPS
CREATE INDEX IF NOT EXISTS idx_support_tickets_user ON public.support_tickets(user_id);
CREATE INDEX IF NOT EXISTS idx_support_tickets_discord ON public.support_tickets(discord_user_id);
CREATE INDEX IF NOT EXISTS idx_support_tickets_status ON public.support_tickets(status);
CREATE INDEX IF NOT EXISTS idx_user_usage_user ON public.user_usage(user_id);
CREATE INDEX IF NOT EXISTS idx_usage_logs_user ON public.usage_logs(user_id);
