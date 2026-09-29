-- Immutable security-audit follow-up migration. Executed as the schema owner.
-- app.runtime_role is transaction-local and supplied by MigrationSqlRunner.

DO $$
DECLARE runtime_role text := current_setting('app.runtime_role');
BEGIN
    IF runtime_role = current_user OR NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = runtime_role) THEN
        RAISE EXCEPTION 'A separate runtime role must be provisioned first';
    END IF;
END $$;

-- The recovery administrator has no customer identity. Its transaction-local
-- marker must carry the same backend- and transaction-bound signature as a
-- customer identity before policies can treat it as a super administrator.
CREATE OR REPLACE FUNCTION app_security.verified_environment_admin() RETURNS boolean
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path = pg_catalog, pg_temp AS $$
DECLARE
    signing_key text;
    supplied_signature text;
    expected_signature text;
    payload text;
BEGIN
    IF current_setting('app.environment_admin', true) IS DISTINCT FROM 'true' THEN
        RETURN false;
    END IF;
    supplied_signature := nullif(current_setting('app.context_signature', true), '');
    IF supplied_signature IS NULL THEN RETURN false; END IF;
    SELECT s.secret INTO signing_key FROM app_security.rls_context_secret s WHERE s.singleton;
    IF signing_key IS NULL THEN RETURN false; END IF;
    payload := 'rls-context-v1|environment-admin|' || pg_backend_pid()::text
        || '|' || pg_current_xact_id()::text;
    expected_signature := encode(public.hmac(payload, signing_key, 'sha256'), 'hex');
    RETURN supplied_signature = expected_signature;
END $$;

CREATE OR REPLACE FUNCTION app_security.actor_role() RETURNS text
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = pg_catalog, pg_temp AS $$
    SELECT CASE WHEN app_security.verified_environment_admin() THEN 'SUPER_ADMIN' ELSE coalesce((
        SELECT CASE c.access_level
            WHEN 1 THEN 'ADMIN' WHEN 2 THEN 'ADMIN' WHEN 3 THEN 'SUPER_ADMIN'
            WHEN 4 THEN 'DRIVER' ELSE 'USER'
        END
        FROM public.customers c
        WHERE c.id = app_security.verified_customer_id() AND c.access_level BETWEEN 0 AND 4
    ), '') END
$$;

-- Remove the synthetic customer introduced by v3. Recovery administration is
-- represented only by the signed marker above and therefore cannot own orders.
DELETE FROM public.customers WHERE environment_admin;
DROP FUNCTION IF EXISTS app_security.environment_admin_account_id();

-- Email identities are canonical and unique irrespective of case. Secondary
-- phone numbers are profile data, not authentication identifiers.
UPDATE public.customers SET email = lower(btrim(email)) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_customers_email_normalized
    ON public.customers (lower(email)) WHERE email IS NOT NULL;

-- Serialize browser retries into a single customer order. The key is scoped to
-- the authenticated customer so unrelated accounts cannot collide.
ALTER TABLE public.customer_order ADD COLUMN IF NOT EXISTS idempotency_key varchar(128);
CREATE UNIQUE INDEX IF NOT EXISTS uq_customer_order_idempotency
    ON public.customer_order (customer_id, idempotency_key)
    WHERE customer_id IS NOT NULL AND idempotency_key IS NOT NULL;

CREATE OR REPLACE FUNCTION app_security.login_credentials(identifier text)
RETURNS TABLE(id bigint, email varchar, password varchar, access_level smallint)
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = pg_catalog, pg_temp AS $$
    SELECT c.id, c.email, c.password, c.access_level FROM public.customers c
    WHERE lower(c.email) = lower(btrim(identifier)) OR c.phone1 = btrim(identifier)
    ORDER BY CASE WHEN lower(c.email) = lower(btrim(identifier)) THEN 0 ELSE 1 END, c.id LIMIT 1
$$;

CREATE OR REPLACE FUNCTION app_security.account_exists(email_value text, phone_value text) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = pg_catalog, pg_temp AS $$
    SELECT EXISTS (SELECT 1 FROM public.customers c
        WHERE lower(c.email) = lower(btrim(email_value)) OR c.phone1 = btrim(phone_value))
$$;

-- Return only the affected username so the application can invalidate active
-- HTTP and WebSocket credentials after a successful reset.
DROP FUNCTION IF EXISTS app_security.consume_reset(text, text);
CREATE FUNCTION app_security.consume_reset(hash_value text, password_value text) RETURNS text
LANGUAGE plpgsql SECURITY DEFINER SET search_path = pg_catalog, pg_temp AS $$
DECLARE account_id bigint; token_id bigint; account_email text;
BEGIN
    SELECT customer_id INTO account_id FROM public.password_reset_tokens WHERE token_hash = hash_value;
    IF account_id IS NULL THEN RETURN NULL; END IF;
    PERFORM id FROM public.customers WHERE id = account_id FOR UPDATE;
    SELECT id INTO token_id FROM public.password_reset_tokens
        WHERE token_hash = hash_value AND NOT used AND expires_at > LOCALTIMESTAMP FOR UPDATE;
    IF token_id IS NULL THEN RETURN NULL; END IF;
    UPDATE public.customers SET password = password_value WHERE id = account_id RETURNING email INTO account_email;
    UPDATE public.password_reset_tokens SET used = true WHERE id = token_id;
    DELETE FROM public.password_reset_tokens WHERE customer_id = account_id AND NOT used;
    RETURN account_email;
END $$;

-- The runtime can execute only functions required by policies and narrow
-- authentication/recovery operations. A domain-separated fingerprint lets the
-- runtime detect a deployment with the wrong key without accepting candidate
-- secrets as SQL function arguments.
DO $$
DECLARE runtime_role text := current_setting('app.runtime_role'); routine regprocedure;
BEGIN
    FOR routine IN SELECT p.oid::regprocedure FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace
        WHERE n.nspname='app_security' LOOP
        EXECUTE format('REVOKE ALL ON FUNCTION %s FROM PUBLIC, %I', routine, runtime_role);
    END LOOP;
END $$;
DROP FUNCTION IF EXISTS app_security.context_secret_matches(text);
CREATE OR REPLACE FUNCTION app_security.context_key_fingerprint() RETURNS text
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = pg_catalog, pg_temp AS $$
    SELECT encode(public.hmac('rls-key-check-v1', s.secret, 'sha256'), 'hex')
    FROM app_security.rls_context_secret s WHERE s.singleton
$$;
REVOKE ALL ON FUNCTION app_security.context_key_fingerprint() FROM PUBLIC;

DO $$
DECLARE runtime_role text := current_setting('app.runtime_role'); routine regprocedure;
BEGIN
    FOREACH routine IN ARRAY ARRAY[
        'app_security.actor_role()'::regprocedure,
        'app_security.actor_customer_id()'::regprocedure,
        'app_security.actor_branch_id()'::regprocedure,
        'app_security.account_context(bigint)'::regprocedure,
        'app_security.login_credentials(text)'::regprocedure,
        'app_security.account_exists(text,text)'::regprocedure,
        'app_security.register_customer(text,text,text,text,text,text,text,text,text,text,text,text,text,timestamp without time zone,text,text,numeric,numeric,text,text)'::regprocedure,
        'app_security.create_reset(text,text,timestamp without time zone)'::regprocedure,
        'app_security.cancel_reset(text)'::regprocedure,
        'app_security.consume_reset(text,text)'::regprocedure,
        'app_security.verified_environment_admin()'::regprocedure,
        'app_security.context_key_fingerprint()'::regprocedure
    ] LOOP
        EXECUTE format('GRANT EXECUTE ON FUNCTION %s TO %I', routine, runtime_role);
    END LOOP;
END $$;
