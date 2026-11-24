-- =====================================================================
-- INSTAGRAM CLONE & DISCORD BOT: UNIFIED SUPABASE DATABASE SCHEMA
-- =====================================================================

-- Enable UUID extension if not enabled
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =====================================================================
-- 1. ENUMS
-- =====================================================================
DO $$ BEGIN
    CREATE TYPE account_status_type AS ENUM ('active', 'suspended', 'banned', 'inactive');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE subscription_status_type AS ENUM ('pending', 'active', 'expired', 'suspended', 'cancelled');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE payment_status_type AS ENUM ('pending', 'pending_review', 'approved', 'rejected', 'refunded');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- =====================================================================
-- 2. TABLE DEFINITIONS
-- =====================================================================

-- TABLE 1: PROFILES (Linked to Supabase Auth UUID)
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    username TEXT UNIQUE NOT NULL,
    email TEXT UNIQUE NOT NULL,
    discord_user_id TEXT,
    account_status account_status_type DEFAULT 'active' NOT NULL,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

-- TABLE 2: PRODUCTS
CREATE TABLE IF NOT EXISTS public.products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_name TEXT UNIQUE NOT NULL,
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

-- TABLE 3: PLANS
CREATE TABLE IF NOT EXISTS public.plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES public.products(id) ON DELETE CASCADE,
    plan_name TEXT NOT NULL,
    duration_days INTEGER NOT NULL CHECK (duration_days > 0),
    price NUMERIC(10, 2) NOT NULL CHECK (price >= 0),
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

-- TABLE 4: SUBSCRIPTIONS
CREATE TABLE IF NOT EXISTS public.subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES public.products(id) ON DELETE RESTRICT,
    plan_id UUID NOT NULL REFERENCES public.plans(id) ON DELETE RESTRICT,
    start_date TIMESTAMPTZ NOT NULL,
    expiry_date TIMESTAMPTZ NOT NULL,
    subscription_status subscription_status_type DEFAULT 'active' NOT NULL,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

-- TABLE 5: PAYMENTS
CREATE TABLE IF NOT EXISTS public.payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    customer_email TEXT NOT NULL,
    discord_user_id TEXT NOT NULL,
    product_id UUID NOT NULL REFERENCES public.products(id),
    plan_id UUID NOT NULL REFERENCES public.plans(id),
    payment_reference TEXT UNIQUE NOT NULL,
    transaction_id TEXT,
    amount NUMERIC(10, 2) NOT NULL,
    screenshot_path TEXT,
    payment_status payment_status_type DEFAULT 'pending' NOT NULL,
    rejection_reason TEXT,
    reviewed_by TEXT,
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

-- TABLE 6: ADMIN LOGS
CREATE TABLE IF NOT EXISTS public.admin_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id TEXT NOT NULL,
    action TEXT NOT NULL,
    target_user_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    description TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

-- TABLE 7: NOTIFICATIONS
CREATE TABLE IF NOT EXISTS public.notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    notification_type TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

-- TABLE 8: DEVICE SESSIONS
CREATE TABLE IF NOT EXISTS public.device_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    device_name TEXT NOT NULL,
    last_active TIMESTAMPTZ DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL,
    session_status TEXT DEFAULT 'active' NOT NULL,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

-- =====================================================================
-- 3. INDEXES FOR HIGH-PERFORMANCE QUERIES
-- =====================================================================
CREATE INDEX IF NOT EXISTS idx_profiles_username ON public.profiles(username);
CREATE INDEX IF NOT EXISTS idx_profiles_email ON public.profiles(email);
CREATE INDEX IF NOT EXISTS idx_profiles_discord ON public.profiles(discord_user_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_user_status ON public.subscriptions(user_id, subscription_status);
CREATE INDEX IF NOT EXISTS idx_subscriptions_expiry ON public.subscriptions(expiry_date);
CREATE INDEX IF NOT EXISTS idx_payments_reference ON public.payments(payment_reference);
CREATE INDEX IF NOT EXISTS idx_payments_status ON public.payments(payment_status);
CREATE INDEX IF NOT EXISTS idx_payments_discord ON public.payments(discord_user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user_read ON public.notifications(user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_device_sessions_user ON public.device_sessions(user_id);

-- =====================================================================
-- 4. STORAGE BUCKET FOR PAYMENT SCREENSHOTS (Private)
-- =====================================================================
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'payment-screenshots',
    'payment-screenshots',
    false,
    10485760, -- 10MB limit
    ARRAY['image/png', 'image/jpeg', 'image/jpg', 'image/webp']
)
ON CONFLICT (id) DO NOTHING;

-- Storage RLS
ALTER TABLE storage.objects ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Service role full access on payment-screenshots"
ON storage.objects
FOR ALL
TO service_role
USING (bucket_id = 'payment-screenshots');

CREATE POLICY "Users can upload screenshots"
ON storage.objects
FOR INSERT
TO anon, authenticated
WITH CHECK (bucket_id = 'payment-screenshots');

-- =====================================================================
-- 5. ROW LEVEL SECURITY (RLS) POLICIES
-- =====================================================================

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.products ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.plans ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.subscriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.admin_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.device_sessions ENABLE ROW LEVEL SECURITY;

-- Service role bypasses all RLS automatically for the Discord bot and backend.
-- Authenticated users policies:

-- PROFILES
CREATE POLICY "Users can view their own profile"
ON public.profiles FOR SELECT
TO authenticated
USING (id = auth.uid());

CREATE POLICY "Users can update their own profile username"
ON public.profiles FOR UPDATE
TO authenticated
USING (id = auth.uid())
WITH CHECK (id = auth.uid());

-- PRODUCTS (Publicly readable active products)
CREATE POLICY "Anyone can view active products"
ON public.products FOR SELECT
TO anon, authenticated
USING (is_active = TRUE);

-- PLANS (Publicly readable active plans)
CREATE POLICY "Anyone can view active plans"
ON public.plans FOR SELECT
TO anon, authenticated
USING (is_active = TRUE);

-- SUBSCRIPTIONS
CREATE POLICY "Users can view their own subscriptions"
ON public.subscriptions FOR SELECT
TO authenticated
USING (user_id = auth.uid());

-- PAYMENTS
CREATE POLICY "Users can view their own payments"
ON public.payments FOR SELECT
TO authenticated
USING (user_id = auth.uid());

-- NOTIFICATIONS
CREATE POLICY "Users can view their own notifications"
ON public.notifications FOR SELECT
TO authenticated
USING (user_id = auth.uid());

CREATE POLICY "Users can update their own notification read status"
ON public.notifications FOR UPDATE
TO authenticated
USING (user_id = auth.uid())
WITH CHECK (user_id = auth.uid());

-- DEVICE SESSIONS
CREATE POLICY "Users can view their own device sessions"
ON public.device_sessions FOR SELECT
TO authenticated
USING (user_id = auth.uid());

CREATE POLICY "Users can manage their own device sessions"
ON public.device_sessions FOR ALL
TO authenticated
USING (user_id = auth.uid())
WITH CHECK (user_id = auth.uid());

-- =====================================================================
-- 6. SECURE DATABASE FUNCTIONS (SECURITY DEFINER)
-- =====================================================================

-- 6.1 Resolve Username to Email securely (used for Username + Password Login)
-- Never exposes emails publicly via tables; only returns email for exact matching username
CREATE OR REPLACE FUNCTION public.resolve_username_to_email(p_username TEXT)
RETURNS TEXT
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth
AS $$
DECLARE
    v_email TEXT;
BEGIN
    SELECT email INTO v_email
    FROM public.profiles
    WHERE LOWER(username) = LOWER(TRIM(p_username))
    LIMIT 1;

    RETURN v_email;
END;
$$;

REVOKE ALL ON FUNCTION public.resolve_username_to_email(TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.resolve_username_to_email(TEXT) TO anon, authenticated, service_role;

-- 6.2 Check User Subscription Access & Status
CREATE OR REPLACE FUNCTION public.verify_user_access(p_user_id UUID)
RETURNS TABLE (
    is_allowed BOOLEAN,
    account_status account_status_type,
    subscription_status subscription_status_type,
    expiry_date TIMESTAMPTZ,
    product_name TEXT,
    message TEXT
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_acc_status account_status_type;
    v_sub RECORD;
BEGIN
    -- 1. Check profile existence and account status
    SELECT p.account_status INTO v_acc_status
    FROM public.profiles p
    WHERE p.id = p_user_id;

    IF NOT FOUND THEN
        RETURN QUERY SELECT FALSE, 'inactive'::account_status_type, NULL::subscription_status_type, NULL::TIMESTAMPTZ, NULL::TEXT, 'Account profile does not exist.';
        RETURN;
    END IF;

    IF v_acc_status != 'active' THEN
        RETURN QUERY SELECT FALSE, v_acc_status, NULL::subscription_status_type, NULL::TIMESTAMPTZ, NULL::TEXT, 'Account is ' || v_acc_status::TEXT || '. Please contact support.';
        RETURN;
    END IF;

    -- 2. Check active subscription
    SELECT s.subscription_status, s.expiry_date, prod.product_name
    INTO v_sub
    FROM public.subscriptions s
    JOIN public.products prod ON s.product_id = prod.id
    WHERE s.user_id = p_user_id
      AND s.subscription_status = 'active'
      AND s.expiry_date > NOW()
    ORDER BY s.expiry_date DESC
    LIMIT 1;

    IF FOUND THEN
        RETURN QUERY SELECT TRUE, v_acc_status, v_sub.subscription_status, v_sub.expiry_date, v_sub.product_name, 'Access granted.';
    ELSE
        -- Check if expired or pending
        SELECT s.subscription_status, s.expiry_date, prod.product_name
        INTO v_sub
        FROM public.subscriptions s
        JOIN public.products prod ON s.product_id = prod.id
        WHERE s.user_id = p_user_id
        ORDER BY s.expiry_date DESC
        LIMIT 1;

        IF FOUND THEN
            RETURN QUERY SELECT FALSE, v_acc_status, v_sub.subscription_status, v_sub.expiry_date, v_sub.product_name, 'Subscription has expired or is inactive.';
        ELSE
            RETURN QUERY SELECT FALSE, v_acc_status, NULL::subscription_status_type, NULL::TIMESTAMPTZ, NULL::TEXT, 'No active subscription found for this account.';
        END IF;
    END IF;
END;
$$;

REVOKE ALL ON FUNCTION public.verify_user_access(UUID) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.verify_user_access(UUID) TO authenticated, service_role;

-- 6.3 Automated Expiration Worker Function
CREATE OR REPLACE FUNCTION public.check_and_expire_subscriptions()
RETURNS INTEGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_count INTEGER := 0;
BEGIN
    UPDATE public.subscriptions
    SET subscription_status = 'expired',
        updated_at = NOW()
    WHERE subscription_status = 'active'
      AND expiry_date <= NOW();

    GET DIAGNOSTICS v_count = ROW_COUNT;
    RETURN v_count;
END;
$$;

-- =====================================================================
-- 7. SEED DATA (Default Product and Plans)
-- =====================================================================
INSERT INTO public.products (id, product_name, description, is_active)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'Insight Editor',
    'Full access to the mobile Instagram Clone application and live Reel Insights Editor.',
    TRUE
)
ON CONFLICT (product_name) DO UPDATE
SET is_active = TRUE;

INSERT INTO public.plans (product_id, plan_name, duration_days, price, description, is_active)
VALUES
    ('00000000-0000-0000-0000-000000000001', '7 Days Access', 7, 199.00, 'Weekly access plan for Insight Editor', TRUE),
    ('00000000-0000-0000-0000-000000000001', '30 Days Access', 30, 499.00, 'Monthly access plan for Insight Editor', TRUE),
    ('00000000-0000-0000-0000-000000000001', '90 Days Access', 90, 1199.00, 'Quarterly access plan for Insight Editor', TRUE),
    ('00000000-0000-0000-0000-000000000001', '180 Days Access', 180, 1999.00, 'Half-year access plan for Insight Editor', TRUE),
    ('00000000-0000-0000-0000-000000000001', '365 Days Access', 365, 3499.00, 'Full year access plan for Insight Editor', TRUE)
ON CONFLICT DO NOTHING;
