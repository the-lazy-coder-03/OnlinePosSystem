-- Immutable RLS hardening migration. Executed as the schema owner.
-- app.runtime_role is transaction-local and supplied by MigrationSqlRunner.

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_catalog.pg_extension WHERE extname = 'pgcrypto'
    ) THEN
        RAISE EXCEPTION 'pgcrypto is required; rerun scripts/provision-rls.sh as the database administrator';
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS app_security.rls_context_secret (
    singleton boolean PRIMARY KEY DEFAULT true CHECK (singleton),
    secret text NOT NULL CHECK (octet_length(secret) >= 32),
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);
REVOKE ALL ON app_security.rls_context_secret FROM PUBLIC;

-- Only a signed customer id is accepted as an actor. The signature is bound to
-- this backend and transaction, so it cannot be replayed on another request.
CREATE OR REPLACE FUNCTION app_security.verified_customer_id() RETURNS bigint
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path = pg_catalog, pg_temp AS $$
DECLARE
    candidate bigint;
    signing_key text;
    supplied_signature text;
    expected_signature text;
    payload text;
BEGIN
    BEGIN
        candidate := nullif(current_setting('app.customer_id', true), '')::bigint;
    EXCEPTION WHEN invalid_text_representation OR numeric_value_out_of_range THEN
        RETURN NULL;
    END;
    supplied_signature := nullif(current_setting('app.context_signature', true), '');
    IF candidate IS NULL OR candidate <= 0 OR supplied_signature IS NULL THEN
        RETURN NULL;
    END IF;
    SELECT s.secret INTO signing_key
    FROM app_security.rls_context_secret s
    WHERE s.singleton;
    IF signing_key IS NULL THEN RETURN NULL; END IF;
    payload := 'rls-context-v1|' || candidate::text || '|' || pg_backend_pid()::text
        || '|' || pg_current_xact_id()::text;
    expected_signature := encode(public.hmac(payload, signing_key, 'sha256'), 'hex');
    IF supplied_signature <> expected_signature THEN RETURN NULL; END IF;
    RETURN candidate;
END $$;

CREATE OR REPLACE FUNCTION app_security.actor_customer_id() RETURNS bigint
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = pg_catalog, pg_temp AS $$
    SELECT c.id
    FROM public.customers c
    WHERE c.id = app_security.verified_customer_id()
$$;

CREATE OR REPLACE FUNCTION app_security.actor_role() RETURNS text
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = pg_catalog, pg_temp AS $$
    SELECT coalesce((
        SELECT CASE c.access_level
            WHEN 1 THEN 'ADMIN'
            WHEN 2 THEN 'ADMIN'
            WHEN 3 THEN 'SUPER_ADMIN'
            WHEN 4 THEN 'DRIVER'
            ELSE 'USER'
        END
        FROM public.customers c
        WHERE c.id = app_security.verified_customer_id()
          AND c.access_level BETWEEN 0 AND 4
    ), '')
$$;

CREATE OR REPLACE FUNCTION app_security.actor_branch_id() RETURNS bigint
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = pg_catalog, pg_temp AS $$
    SELECT CASE c.access_level WHEN 1 THEN 1::bigint WHEN 2 THEN 2::bigint ELSE NULL END
    FROM public.customers c
    WHERE c.id = app_security.verified_customer_id()
$$;

INSERT INTO public.customers (first_name, last_name, role, access_level, environment_admin)
VALUES ('System', 'Admin', 'SUPER_ADMIN', 3, true)
ON CONFLICT DO NOTHING;

CREATE OR REPLACE FUNCTION app_security.environment_admin_account_id() RETURNS bigint
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = pg_catalog, pg_temp AS $$
    SELECT c.id FROM public.customers c WHERE c.environment_admin
$$;

CREATE OR REPLACE FUNCTION app_security.context_secret_matches(candidate text) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = pg_catalog, pg_temp AS $$
    SELECT candidate IS NOT NULL AND octet_length(candidate) >= 32 AND EXISTS (
        SELECT 1 FROM app_security.rls_context_secret s
        WHERE s.singleton
          AND encode(public.hmac('rls-key-check-v1', s.secret, 'sha256'), 'hex')
            = encode(public.hmac('rls-key-check-v1', candidate, 'sha256'), 'hex')
    )
$$;

-- Specials definitions are shared catalog data. Authorization for writes stays
-- in the super-admin application endpoints and ordinary SQL privileges.
DO $$
DECLARE table_name text; policy_name text;
BEGIN
    FOREACH table_name IN ARRAY ARRAY[
        'special', 'special_day', 'special_component', 'special_component_menu_item',
        'special_component_pizza', 'special_addon'
    ] LOOP
        FOR policy_name IN
            SELECT p.polname FROM pg_catalog.pg_policy p
            WHERE p.polrelid = ('public.' || table_name)::regclass
        LOOP
            EXECUTE format('DROP POLICY %I ON public.%I', policy_name, table_name);
        END LOOP;
        EXECUTE format('ALTER TABLE public.%I NO FORCE ROW LEVEL SECURITY', table_name);
        EXECUTE format('ALTER TABLE public.%I DISABLE ROW LEVEL SECURITY', table_name);
    END LOOP;
END $$;

-- Replace every runtime policy. Owner maintenance policies remain restricted to
-- the table owner and keep FORCE RLS compatible with migrations.
DO $$
DECLARE runtime_role text := current_setting('app.runtime_role');
BEGIN
    IF runtime_role = current_user OR NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = runtime_role) THEN
        RAISE EXCEPTION 'A separate runtime role must be provisioned first';
    END IF;

    DROP POLICY IF EXISTS orders_read ON public.customer_order;
    EXECUTE format('CREATE POLICY orders_read ON public.customer_order FOR SELECT TO %I USING (
        app_security.actor_role() = ''SUPER_ADMIN''
        OR (app_security.actor_role() = ''USER'' AND customer_id = app_security.actor_customer_id())
        OR (app_security.actor_role() = ''ADMIN'' AND branch_id = app_security.actor_branch_id()))', runtime_role);
    DROP POLICY IF EXISTS orders_insert ON public.customer_order;
    EXECUTE format('CREATE POLICY orders_insert ON public.customer_order FOR INSERT TO %I WITH CHECK (
        app_security.actor_role() = ''SUPER_ADMIN''
        OR (app_security.actor_role() = ''USER'' AND customer_id = app_security.actor_customer_id()))', runtime_role);
    DROP POLICY IF EXISTS orders_update ON public.customer_order;
    EXECUTE format('CREATE POLICY orders_update ON public.customer_order FOR UPDATE TO %I USING (
        app_security.actor_role() = ''SUPER_ADMIN''
        OR (app_security.actor_role() = ''ADMIN'' AND branch_id = app_security.actor_branch_id())) WITH CHECK (
        app_security.actor_role() = ''SUPER_ADMIN''
        OR (app_security.actor_role() = ''ADMIN'' AND branch_id = app_security.actor_branch_id()))', runtime_role);
    DROP POLICY IF EXISTS orders_delete ON public.customer_order;
    EXECUTE format('CREATE POLICY orders_delete ON public.customer_order FOR DELETE TO %I USING (
        app_security.actor_role() = ''SUPER_ADMIN'')', runtime_role);

    DROP POLICY IF EXISTS customers_read ON public.customers;
    EXECUTE format('CREATE POLICY customers_read ON public.customers FOR SELECT TO %I USING (
        app_security.actor_role() = ''SUPER_ADMIN''
        OR (app_security.actor_role() IN (''USER'', ''ADMIN'', ''DRIVER'') AND id = app_security.actor_customer_id())
        OR (app_security.actor_role() = ''ADMIN'' AND EXISTS (
            SELECT 1 FROM public.customer_order o WHERE o.customer_id = customers.id
              AND o.branch_id = app_security.actor_branch_id())))', runtime_role);
    DROP POLICY IF EXISTS customers_update ON public.customers;
    EXECUTE format('CREATE POLICY customers_update ON public.customers FOR UPDATE TO %I USING (
        app_security.actor_role() = ''SUPER_ADMIN''
        OR (app_security.actor_role() IN (''USER'', ''ADMIN'', ''DRIVER'') AND id = app_security.actor_customer_id())) WITH CHECK (
        app_security.actor_role() = ''SUPER_ADMIN''
        OR (app_security.actor_role() IN (''USER'', ''ADMIN'', ''DRIVER'') AND id = app_security.actor_customer_id()))', runtime_role);
    DROP POLICY IF EXISTS customers_insert ON public.customers;
    EXECUTE format('CREATE POLICY customers_insert ON public.customers FOR INSERT TO %I WITH CHECK (
        app_security.actor_role() = ''SUPER_ADMIN'')', runtime_role);
    DROP POLICY IF EXISTS customers_delete ON public.customers;
    EXECUTE format('CREATE POLICY customers_delete ON public.customers FOR DELETE TO %I USING (
        app_security.actor_role() = ''SUPER_ADMIN'')', runtime_role);

    DROP POLICY IF EXISTS customer_notes_read ON public.customer_notes;
    EXECUTE format('CREATE POLICY customer_notes_read ON public.customer_notes FOR SELECT TO %I USING (
        app_security.actor_role() = ''SUPER_ADMIN'' OR (app_security.actor_role() = ''ADMIN'' AND EXISTS (
            SELECT 1 FROM public.customer_order o WHERE o.customer_id = customer_notes.customer_id
              AND o.branch_id = app_security.actor_branch_id())))', runtime_role);
    DROP POLICY IF EXISTS customer_notes_insert ON public.customer_notes;
    EXECUTE format('CREATE POLICY customer_notes_insert ON public.customer_notes FOR INSERT TO %I WITH CHECK (
        app_security.actor_role() = ''SUPER_ADMIN'' OR (app_security.actor_role() = ''ADMIN'' AND EXISTS (
            SELECT 1 FROM public.customer_order o WHERE o.customer_id = customer_notes.customer_id
              AND o.branch_id = app_security.actor_branch_id())))', runtime_role);

    DROP POLICY IF EXISTS staff_admin ON public.staff;
    EXECUTE format('CREATE POLICY staff_admin ON public.staff TO %I USING (
        app_security.actor_role() = ''SUPER_ADMIN'') WITH CHECK (
        app_security.actor_role() = ''SUPER_ADMIN'')', runtime_role);
END $$;

-- Each child predicate reaches customer_order and states ownership explicitly.
DO $$
DECLARE
    runtime_role text := current_setting('app.runtime_role');
    child text;
    joins text;
    relationship text;
    ownership text;
    predicate text;
BEGIN
    ownership := '(app_security.actor_role() = ''SUPER_ADMIN''
        OR (app_security.actor_role() = ''USER'' AND o.customer_id = app_security.actor_customer_id())
        OR (app_security.actor_role() = ''ADMIN'' AND o.branch_id = app_security.actor_branch_id()))';
    FOR child, joins, relationship IN SELECT * FROM (VALUES
        ('order_menu_item', '', 'o.order_id = order_menu_item.order_id'),
        ('order_pizza_item', '', 'o.order_id = order_pizza_item.order_id'),
        ('order_special_item', '', 'o.order_id = order_special_item.order_id'),
        ('order_menu_item_extra', 'JOIN public.order_menu_item p ON p.order_id = o.order_id',
            'p.order_menu_item_id = order_menu_item_extra.order_menu_item_id'),
        ('order_burger_protein', 'JOIN public.order_menu_item p ON p.order_id = o.order_id',
            'p.order_menu_item_id = order_burger_protein.order_menu_item_id'),
        ('order_burger_removed_component', 'JOIN public.order_menu_item p ON p.order_id = o.order_id',
            'p.order_menu_item_id = order_burger_removed_component.order_menu_item_id'),
        ('order_burger_extra_component', 'JOIN public.order_menu_item p ON p.order_id = o.order_id',
            'p.order_menu_item_id = order_burger_extra_component.order_menu_item_id'),
        ('order_pizza_item_extra', 'JOIN public.order_pizza_item p ON p.order_id = o.order_id',
            'p.order_pizza_item_id = order_pizza_item_extra.order_pizza_item_id'),
        ('order_pizza_item_base_option', 'JOIN public.order_pizza_item p ON p.order_id = o.order_id',
            'p.order_pizza_item_id = order_pizza_item_base_option.order_pizza_item_id'),
        ('order_pizza_item_removed_ingredient', 'JOIN public.order_pizza_item p ON p.order_id = o.order_id',
            'p.order_pizza_item_id = order_pizza_item_removed_ingredient.order_pizza_item_id'),
        ('order_special_selection', 'JOIN public.order_special_item p ON p.order_id = o.order_id',
            'p.order_special_item_id = order_special_selection.order_special_item_id')
    ) AS relationships(child, joins, relationship) LOOP
        predicate := format('EXISTS (SELECT 1 FROM public.customer_order o %s WHERE %s AND %s)',
            joins, relationship, ownership);
        EXECUTE format('DROP POLICY IF EXISTS child_read ON public.%I', child);
        EXECUTE format('CREATE POLICY child_read ON public.%I FOR SELECT TO %I USING (%s)',
            child, runtime_role, predicate);
        EXECUTE format('DROP POLICY IF EXISTS child_insert ON public.%I', child);
        EXECUTE format('CREATE POLICY child_insert ON public.%I FOR INSERT TO %I WITH CHECK (
            app_security.actor_role() IN (''USER'', ''SUPER_ADMIN'') AND %s)', child, runtime_role, predicate);
        EXECUTE format('DROP POLICY IF EXISTS child_update ON public.%I', child);
        EXECUTE format('CREATE POLICY child_update ON public.%I FOR UPDATE TO %I USING (
            app_security.actor_role() = ''SUPER_ADMIN'' AND %s) WITH CHECK (
            app_security.actor_role() = ''SUPER_ADMIN'' AND %s)', child, runtime_role, predicate, predicate);
        EXECUTE format('DROP POLICY IF EXISTS child_delete ON public.%I', child);
        EXECUTE format('CREATE POLICY child_delete ON public.%I FOR DELETE TO %I USING (
            app_security.actor_role() = ''SUPER_ADMIN'' AND %s)', child, runtime_role, predicate);
    END LOOP;
END $$;

CREATE OR REPLACE FUNCTION app_security.guard_account_update() RETURNS trigger
LANGUAGE plpgsql SET search_path = pg_catalog, pg_temp AS $$
DECLARE expected_role text;
BEGIN
    IF current_user = pg_get_userbyid((SELECT relowner FROM pg_class WHERE oid = TG_RELID)) THEN RETURN NEW; END IF;
    IF NEW.id IS DISTINCT FROM OLD.id OR NEW.environment_admin IS DISTINCT FROM OLD.environment_admin THEN
        RAISE EXCEPTION 'Operation is not permitted' USING ERRCODE = '42501';
    END IF;
    IF (NEW.access_level IS DISTINCT FROM OLD.access_level OR NEW.role IS DISTINCT FROM OLD.role)
       AND app_security.actor_role() <> 'SUPER_ADMIN' THEN
        RAISE EXCEPTION 'Operation is not permitted' USING ERRCODE = '42501';
    END IF;
    expected_role := CASE NEW.access_level
        WHEN 1 THEN 'ADMIN' WHEN 2 THEN 'ADMIN' WHEN 3 THEN 'SUPER_ADMIN'
        WHEN 4 THEN 'DRIVER' ELSE 'USER' END;
    IF NEW.role IS DISTINCT FROM expected_role THEN
        RAISE EXCEPTION 'Role and access level are inconsistent' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END $$;

CREATE OR REPLACE FUNCTION app_security.validate_special_selection_relationship() RETURNS trigger
LANGUAGE plpgsql SET search_path = pg_catalog, pg_temp AS $$
DECLARE special_order_id bigint; selected_order_id bigint;
BEGIN
    SELECT s.order_id INTO special_order_id
    FROM public.order_special_item s
    WHERE s.order_special_item_id = NEW.order_special_item_id;
    IF NEW.order_menu_item_id IS NOT NULL THEN
        SELECT m.order_id INTO selected_order_id FROM public.order_menu_item m
        WHERE m.order_menu_item_id = NEW.order_menu_item_id;
    ELSE
        SELECT p.order_id INTO selected_order_id FROM public.order_pizza_item p
        WHERE p.order_pizza_item_id = NEW.order_pizza_item_id;
    END IF;
    IF special_order_id IS NULL OR selected_order_id IS NULL OR special_order_id <> selected_order_id THEN
        RAISE EXCEPTION 'Special selection must reference an item from the same order' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS validate_special_selection_relationship ON public.order_special_selection;
CREATE TRIGGER validate_special_selection_relationship
    BEFORE INSERT OR UPDATE ON public.order_special_selection
    FOR EACH ROW EXECUTE FUNCTION app_security.validate_special_selection_relationship();

-- Direct token-table access is unnecessary; recovery uses the narrow functions.
DO $$
DECLARE runtime_role text := current_setting('app.runtime_role'); routine regprocedure;
BEGIN
    EXECUTE format('REVOKE ALL ON app_security.rls_context_secret FROM PUBLIC, %I', runtime_role);
    EXECUTE format('REVOKE ALL ON public.password_reset_tokens FROM PUBLIC, %I', runtime_role);

    FOR routine IN SELECT p.oid::regprocedure FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace
        WHERE n.nspname='app_security' LOOP
        EXECUTE format('REVOKE ALL ON FUNCTION %s FROM PUBLIC, %I', routine, runtime_role);
    END LOOP;
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
        'app_security.environment_admin_account_id()'::regprocedure,
        'app_security.context_secret_matches(text)'::regprocedure
    ] LOOP
        EXECUTE format('GRANT EXECUTE ON FUNCTION %s TO %I', routine, runtime_role);
    END LOOP;
END $$;
