-- Immutable migration. Executed as the schema owner, never as the application login.
-- app.runtime_role is bound by the migration runner, not substituted into SQL text.
CREATE SCHEMA IF NOT EXISTS app_security;
REVOKE ALL ON SCHEMA app_security FROM PUBLIC;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;

CREATE OR REPLACE FUNCTION app_security.setting_id(setting_name text) RETURNS bigint
LANGUAGE plpgsql STABLE SET search_path = pg_catalog, pg_temp AS $$
BEGIN
    RETURN nullif(current_setting(setting_name, true), '')::bigint;
EXCEPTION WHEN invalid_text_representation OR numeric_value_out_of_range THEN
    RETURN NULL;
END $$;

CREATE OR REPLACE FUNCTION app_security.actor_role() RETURNS text
LANGUAGE sql STABLE SET search_path = pg_catalog, pg_temp AS $$
    SELECT CASE
        WHEN current_setting('app.role', true) = 'SUPER_ADMIN' THEN 'SUPER_ADMIN'
        WHEN app_security.setting_id('app.customer_id') > 0
         AND app_security.setting_id('app.user_id') = app_security.setting_id('app.customer_id')
         AND current_setting('app.role', true) IN ('USER', 'DRIVER')
            THEN current_setting('app.role', true)
        WHEN app_security.setting_id('app.customer_id') > 0
         AND app_security.setting_id('app.user_id') = app_security.setting_id('app.customer_id')
         AND app_security.setting_id('app.branch_id') IN (1, 2)
         AND current_setting('app.role', true) = 'ADMIN' THEN 'ADMIN'
        ELSE '' END
$$;

-- Owner-only policies make FORCE RLS compatible with migrations and the functions below.
DO $$
DECLARE
    table_name text;
    runtime_role text := current_setting('app.runtime_role');
BEGIN
    IF runtime_role = current_user OR NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = runtime_role) THEN
        RAISE EXCEPTION 'A separate runtime role must be provisioned first';
    END IF;
    FOREACH table_name IN ARRAY ARRAY[
        'customers', 'customer_order', 'order_menu_item', 'order_menu_item_extra',
        'order_burger_protein', 'order_burger_removed_component', 'order_burger_extra_component',
        'order_pizza_item', 'order_pizza_item_extra', 'order_pizza_item_base_option',
        'staff', 'password_reset_tokens', 'orders', 'customer_notes'
    ] LOOP
        IF to_regclass('public.' || table_name) IS NULL AND table_name = 'orders' THEN CONTINUE; END IF;
        EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY', table_name);
        EXECUTE format('ALTER TABLE public.%I FORCE ROW LEVEL SECURITY', table_name);
        EXECUTE format('DROP POLICY IF EXISTS owner_maintenance ON public.%I', table_name);
        EXECUTE format('CREATE POLICY owner_maintenance ON public.%I TO %I USING (true) WITH CHECK (true)', table_name, current_user);
    END LOOP;
END $$;

DROP POLICY IF EXISTS orders_read ON public.customer_order;
CREATE POLICY orders_read ON public.customer_order FOR SELECT USING (
    app_security.actor_role() = 'SUPER_ADMIN'
    OR (app_security.actor_role() = 'USER' AND customer_id = app_security.setting_id('app.customer_id'))
    OR (app_security.actor_role() = 'ADMIN' AND branch_id = app_security.setting_id('app.branch_id'))
);
DROP POLICY IF EXISTS orders_insert ON public.customer_order;
CREATE POLICY orders_insert ON public.customer_order FOR INSERT WITH CHECK (
    app_security.actor_role() = 'SUPER_ADMIN'
    OR (app_security.actor_role() = 'USER' AND customer_id = app_security.setting_id('app.customer_id'))
);
DROP POLICY IF EXISTS orders_update ON public.customer_order;
CREATE POLICY orders_update ON public.customer_order FOR UPDATE USING (
    app_security.actor_role() = 'SUPER_ADMIN'
    OR (app_security.actor_role() = 'ADMIN' AND branch_id = app_security.setting_id('app.branch_id'))
) WITH CHECK (
    app_security.actor_role() = 'SUPER_ADMIN'
    OR (app_security.actor_role() = 'ADMIN' AND branch_id = app_security.setting_id('app.branch_id'))
);
DROP POLICY IF EXISTS orders_delete ON public.customer_order;
CREATE POLICY orders_delete ON public.customer_order FOR DELETE USING (app_security.actor_role() = 'SUPER_ADMIN');

DROP POLICY IF EXISTS customers_read ON public.customers;
CREATE POLICY customers_read ON public.customers FOR SELECT USING (
    app_security.actor_role() = 'SUPER_ADMIN'
    OR (app_security.actor_role() IN ('USER', 'ADMIN', 'DRIVER') AND id = app_security.setting_id('app.customer_id'))
    OR (app_security.actor_role() = 'ADMIN' AND EXISTS (
        SELECT 1 FROM public.customer_order o WHERE o.customer_id = customers.id
            AND o.branch_id = app_security.setting_id('app.branch_id')
    ))
);

DROP POLICY IF EXISTS customer_notes_read ON public.customer_notes;
CREATE POLICY customer_notes_read ON public.customer_notes FOR SELECT USING (
    app_security.actor_role() = 'SUPER_ADMIN' OR (app_security.actor_role() = 'ADMIN' AND EXISTS (
        SELECT 1 FROM public.customer_order o WHERE o.customer_id = customer_notes.customer_id
            AND o.branch_id = app_security.setting_id('app.branch_id')
    ))
);
DROP POLICY IF EXISTS customer_notes_insert ON public.customer_notes;
CREATE POLICY customer_notes_insert ON public.customer_notes FOR INSERT WITH CHECK (
    app_security.actor_role() = 'SUPER_ADMIN' OR (app_security.actor_role() = 'ADMIN' AND EXISTS (
        SELECT 1 FROM public.customer_order o WHERE o.customer_id = customer_notes.customer_id
            AND o.branch_id = app_security.setting_id('app.branch_id')
    ))
);
DROP POLICY IF EXISTS customers_update ON public.customers;
CREATE POLICY customers_update ON public.customers FOR UPDATE USING (
    app_security.actor_role() = 'SUPER_ADMIN'
    OR (app_security.actor_role() IN ('USER', 'ADMIN', 'DRIVER') AND id = app_security.setting_id('app.customer_id'))
) WITH CHECK (
    app_security.actor_role() = 'SUPER_ADMIN'
    OR (app_security.actor_role() IN ('USER', 'ADMIN', 'DRIVER') AND id = app_security.setting_id('app.customer_id'))
);
DROP POLICY IF EXISTS customers_insert ON public.customers;
CREATE POLICY customers_insert ON public.customers FOR INSERT WITH CHECK (app_security.actor_role() = 'SUPER_ADMIN');
DROP POLICY IF EXISTS customers_delete ON public.customers;
CREATE POLICY customers_delete ON public.customers FOR DELETE USING (app_security.actor_role() = 'SUPER_ADMIN');
DROP POLICY IF EXISTS staff_admin ON public.staff;
CREATE POLICY staff_admin ON public.staff USING (app_security.actor_role() = 'SUPER_ADMIN')
    WITH CHECK (app_security.actor_role() = 'SUPER_ADMIN');

-- Every descendant checks its actual parent. Parent SELECT policies enforce ownership/branch.
DO $$
DECLARE
    child text;
    parent text;
    parent_key text;
    predicate text;
BEGIN
    FOR child, parent, parent_key IN
        SELECT * FROM (VALUES
            ('order_menu_item', 'customer_order', 'order_id'),
            ('order_pizza_item', 'customer_order', 'order_id'),
            ('order_menu_item_extra', 'order_menu_item', 'order_menu_item_id'),
            ('order_burger_protein', 'order_menu_item', 'order_menu_item_id'),
            ('order_burger_removed_component', 'order_menu_item', 'order_menu_item_id'),
            ('order_burger_extra_component', 'order_menu_item', 'order_menu_item_id'),
            ('order_pizza_item_extra', 'order_pizza_item', 'order_pizza_item_id'),
            ('order_pizza_item_base_option', 'order_pizza_item', 'order_pizza_item_id')
        ) AS relationships(child, parent, parent_key)
    LOOP
        predicate := format('EXISTS (SELECT 1 FROM public.%I p WHERE p.%I = %I.%I)', parent, parent_key, child, parent_key);
        EXECUTE format('DROP POLICY IF EXISTS child_read ON public.%I', child);
        EXECUTE format('CREATE POLICY child_read ON public.%I FOR SELECT USING (%s)', child, predicate);
        EXECUTE format('DROP POLICY IF EXISTS child_insert ON public.%I', child);
        EXECUTE format('CREATE POLICY child_insert ON public.%I FOR INSERT WITH CHECK (app_security.actor_role() IN (''USER'', ''SUPER_ADMIN'') AND %s)', child, predicate);
        EXECUTE format('DROP POLICY IF EXISTS child_update ON public.%I', child);
        EXECUTE format('CREATE POLICY child_update ON public.%I FOR UPDATE USING (app_security.actor_role() = ''SUPER_ADMIN'' AND %s) WITH CHECK (app_security.actor_role() = ''SUPER_ADMIN'' AND %s)', child, predicate, predicate);
        EXECUTE format('DROP POLICY IF EXISTS child_delete ON public.%I', child);
        EXECUTE format('CREATE POLICY child_delete ON public.%I FOR DELETE USING (app_security.actor_role() = ''SUPER_ADMIN'' AND %s)', child, predicate);
    END LOOP;
END $$;

CREATE OR REPLACE FUNCTION app_security.guard_account_update() RETURNS trigger
LANGUAGE plpgsql SET search_path = pg_catalog, pg_temp AS $$
BEGIN
    IF current_user = pg_get_userbyid((SELECT relowner FROM pg_class WHERE oid = TG_RELID)) THEN RETURN NEW; END IF;
    IF NEW.id IS DISTINCT FROM OLD.id
       OR ((NEW.access_level IS DISTINCT FROM OLD.access_level OR NEW.role IS DISTINCT FROM OLD.role)
           AND app_security.actor_role() <> 'SUPER_ADMIN') THEN
        RAISE EXCEPTION 'Operation is not permitted' USING ERRCODE = '42501';
    END IF;
    RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS guard_account_update ON public.customers;
CREATE TRIGGER guard_account_update BEFORE UPDATE ON public.customers
    FOR EACH ROW EXECUTE FUNCTION app_security.guard_account_update();

CREATE OR REPLACE FUNCTION app_security.guard_order_relationship() RETURNS trigger
LANGUAGE plpgsql SET search_path = pg_catalog, pg_temp AS $$
DECLARE column_name text;
BEGIN
    IF current_user = pg_get_userbyid((SELECT relowner FROM pg_class WHERE oid = TG_RELID)) THEN RETURN NEW; END IF;
    FOREACH column_name IN ARRAY TG_ARGV LOOP
        IF (to_jsonb(NEW) -> column_name) IS DISTINCT FROM (to_jsonb(OLD) -> column_name) THEN
            RAISE EXCEPTION 'Operation is not permitted' USING ERRCODE = '42501';
        END IF;
    END LOOP;
    RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS guard_order_relationship ON public.customer_order;
CREATE TRIGGER guard_order_relationship BEFORE UPDATE ON public.customer_order
    FOR EACH ROW EXECUTE FUNCTION app_security.guard_order_relationship('order_id', 'customer_id', 'branch_id');
DO $$
DECLARE child text; parent_key text;
BEGIN
    FOR child, parent_key IN SELECT * FROM (VALUES
        ('order_menu_item', 'order_id'), ('order_pizza_item', 'order_id'),
        ('order_menu_item_extra', 'order_menu_item_id'), ('order_burger_protein', 'order_menu_item_id'),
        ('order_burger_removed_component', 'order_menu_item_id'), ('order_burger_extra_component', 'order_menu_item_id'),
        ('order_pizza_item_extra', 'order_pizza_item_id'), ('order_pizza_item_base_option', 'order_pizza_item_id')
    ) AS relationships(child, parent_key) LOOP
        EXECUTE format('DROP TRIGGER IF EXISTS guard_order_relationship ON public.%I', child);
        EXECUTE format('CREATE TRIGGER guard_order_relationship BEFORE UPDATE ON public.%I FOR EACH ROW EXECUTE FUNCTION app_security.guard_order_relationship(%L)', child, parent_key);
    END LOOP;
END $$;

-- Authentication functions never expose complete customer rows.
CREATE OR REPLACE FUNCTION app_security.account_context(account_id bigint) RETURNS TABLE(access_level smallint)
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = pg_catalog, pg_temp AS $$
    SELECT c.access_level FROM public.customers c WHERE c.id = account_id
$$;
CREATE OR REPLACE FUNCTION app_security.login_credentials(identifier text)
RETURNS TABLE(id bigint, email varchar, password varchar, access_level smallint)
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = pg_catalog, pg_temp AS $$
    SELECT c.id, c.email, c.password, c.access_level FROM public.customers c
    WHERE c.email = identifier OR c.phone1 = identifier OR c.phone2 = identifier
    ORDER BY CASE WHEN c.email = identifier THEN 0 WHEN c.phone1 = identifier THEN 1 ELSE 2 END, c.id LIMIT 1
$$;
CREATE OR REPLACE FUNCTION app_security.account_exists(email_value text, phone_value text) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = pg_catalog, pg_temp AS $$
    SELECT EXISTS (SELECT 1 FROM public.customers c WHERE c.email = email_value OR c.phone1 = phone_value)
$$;
CREATE OR REPLACE FUNCTION app_security.register_customer(
    first_name_value text, last_name_value text, email_value text, password_value text,
    phone1_value text, phone2_value text, house_value text, street_value text, area_value text,
    complex_value text, store_value text, postal_value text, city_value text, ordered_at timestamp
) RETURNS bigint LANGUAGE sql SECURITY DEFINER SET search_path = pg_catalog, pg_temp AS $$
    INSERT INTO public.customers (first_name, last_name, email, password, phone1, phone2,
        house_number, street, area, complex_name, preferred_store, postal_code, city, last_ordered_at, role, access_level)
    VALUES (first_name_value, last_name_value, email_value, password_value, phone1_value, phone2_value,
        house_value, street_value, area_value, complex_value, store_value, postal_value, city_value, ordered_at, 'USER', 0)
    RETURNING id
$$;
CREATE OR REPLACE FUNCTION app_security.create_reset(email_value text, hash_value text, expiry timestamp) RETURNS text
LANGUAGE plpgsql SECURITY DEFINER SET search_path = pg_catalog, pg_temp AS $$
DECLARE account_id bigint; recipient text;
BEGIN
    SELECT id, email INTO account_id, recipient FROM public.customers WHERE email = email_value FOR UPDATE;
    IF account_id IS NULL THEN RETURN NULL; END IF;
    IF hash_value !~ '^[0-9a-f]{64}$' OR expiry <= LOCALTIMESTAMP OR expiry > LOCALTIMESTAMP + interval '31 minutes' THEN
        RAISE EXCEPTION 'Invalid recovery request' USING ERRCODE = '22023';
    END IF;
    DELETE FROM public.password_reset_tokens WHERE customer_id = account_id AND NOT used;
    INSERT INTO public.password_reset_tokens (customer_id, token_hash, expires_at, created_at, used)
    VALUES (account_id, hash_value, expiry, LOCALTIMESTAMP, false);
    RETURN recipient;
END $$;
CREATE OR REPLACE FUNCTION app_security.cancel_reset(hash_value text) RETURNS void
LANGUAGE sql SECURITY DEFINER SET search_path = pg_catalog, pg_temp AS $$
    DELETE FROM public.password_reset_tokens WHERE token_hash = hash_value AND NOT used
$$;
CREATE OR REPLACE FUNCTION app_security.consume_reset(hash_value text, password_value text) RETURNS boolean
LANGUAGE plpgsql SECURITY DEFINER SET search_path = pg_catalog, pg_temp AS $$
DECLARE account_id bigint; token_id bigint;
BEGIN
    SELECT customer_id INTO account_id FROM public.password_reset_tokens WHERE token_hash = hash_value;
    IF account_id IS NULL THEN RETURN false; END IF;
    -- Lock account first, consistently with create_reset, so competing resets cannot both succeed.
    PERFORM id FROM public.customers WHERE id = account_id FOR UPDATE;
    SELECT id INTO token_id FROM public.password_reset_tokens
        WHERE token_hash = hash_value AND NOT used AND expires_at > LOCALTIMESTAMP FOR UPDATE;
    IF token_id IS NULL THEN RETURN false; END IF;
    UPDATE public.customers SET password = password_value WHERE id = account_id;
    UPDATE public.password_reset_tokens SET used = true WHERE id = token_id;
    DELETE FROM public.password_reset_tokens WHERE customer_id = account_id AND NOT used;
    RETURN true;
END $$;

CREATE INDEX IF NOT EXISTS idx_customer_order_customer_branch ON public.customer_order(customer_id, branch_id);
CREATE INDEX IF NOT EXISTS idx_order_menu_extra_parent ON public.order_menu_item_extra(order_menu_item_id);
CREATE INDEX IF NOT EXISTS idx_order_pizza_extra_parent ON public.order_pizza_item_extra(order_pizza_item_id);

-- Grant only application objects; never ALL TABLES, TRUNCATE, or schema creation.
DO $$
DECLARE runtime_role text := current_setting('app.runtime_role'); object_name text; routine regprocedure;
BEGIN
    EXECUTE format('GRANT USAGE ON SCHEMA public, app_security TO %I', runtime_role);
    FOREACH object_name IN ARRAY ARRAY[
        'customers', 'customer_order', 'order_menu_item', 'order_menu_item_extra',
        'order_burger_protein', 'order_burger_removed_component', 'order_burger_extra_component',
        'order_pizza_item', 'order_pizza_item_extra', 'order_pizza_item_base_option', 'staff',
        'branch', 'menu_category', 'menu_item', 'branch_menu_item_price', 'modifier_group', 'modifier_option',
        'menu_item_modifier_group', 'burger_component', 'burger_recipe', 'burger_recipe_component',
        'burger_recipe_assignment', 'burger_item_default_component', 'branch_burger_component_price',
        'salad_ingredients', 'pizza_category', 'pizza', 'pizza_size', 'pizza_allowed_size', 'price_category',
        'ingredient', 'pizza_default_ingredient', 'branch_pizza_price', 'branch_extra_price',
        'pizza_base_option', 'branch_pizza_base_option_price'
    ] LOOP
        EXECUTE format('REVOKE ALL ON public.%I FROM PUBLIC, %I', object_name, runtime_role);
        EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON public.%I TO %I', object_name, runtime_role);
    END LOOP;
    FOREACH object_name IN ARRAY ARRAY['password_reset_tokens', 'orders', 'app_migration_state'] LOOP
        IF to_regclass('public.' || object_name) IS NOT NULL THEN
            EXECUTE format('REVOKE ALL ON public.%I FROM PUBLIC, %I', object_name, runtime_role);
            EXECUTE format('GRANT SELECT ON public.%I TO %I', object_name, runtime_role);
        END IF;
    END LOOP;
    EXECUTE format('REVOKE ALL ON public.customer_notes FROM PUBLIC, %I', runtime_role);
    EXECUTE format('GRANT SELECT, INSERT ON public.customer_notes TO %I', runtime_role);
    FOR object_name IN SELECT c.relname FROM pg_class c JOIN pg_namespace n ON c.relnamespace=n.oid
        WHERE n.nspname='public' AND c.relkind='S' AND EXISTS (
            SELECT 1 FROM pg_depend d JOIN pg_class t ON t.oid=d.refobjid
            WHERE d.objid=c.oid AND d.deptype IN ('a', 'i') AND t.relkind='r'
                AND has_table_privilege(runtime_role, t.oid, 'INSERT')
        ) LOOP
        EXECUTE format('GRANT USAGE, SELECT ON SEQUENCE public.%I TO %I', object_name, runtime_role);
    END LOOP;
    FOREACH object_name IN ARRAY ARRAY['v_burger_builder_defaults', 'v_burger_builder_proteins', 'v_burger_builder_extras'] LOOP
        EXECUTE format('GRANT SELECT ON public.%I TO %I', object_name, runtime_role);
    END LOOP;
    FOR routine IN SELECT p.oid::regprocedure FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace
        WHERE n.nspname='app_security' LOOP
        EXECUTE format('REVOKE ALL ON FUNCTION %s FROM PUBLIC', routine);
        IF routine::text NOT LIKE '%guard_%' THEN
            EXECUTE format('GRANT EXECUTE ON FUNCTION %s TO %I', routine, runtime_role);
        END IF;
    END LOOP;
END $$;
