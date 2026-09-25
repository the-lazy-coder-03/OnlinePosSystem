-- Immutable follow-up migration for specials. Executed as the schema owner.
-- app.runtime_role is bound by the migration runner, not substituted into SQL text.
DO $$
DECLARE
    table_name text;
    runtime_role text := current_setting('app.runtime_role');
BEGIN
    IF runtime_role = current_user OR NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = runtime_role) THEN
        RAISE EXCEPTION 'A separate runtime role must be provisioned first';
    END IF;
    FOREACH table_name IN ARRAY ARRAY[
        'special', 'special_day', 'special_component', 'special_component_menu_item',
        'special_component_pizza', 'special_addon', 'order_special_item',
        'order_special_selection', 'order_pizza_item_removed_ingredient'
    ] LOOP
        EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY', table_name);
        EXECUTE format('ALTER TABLE public.%I FORCE ROW LEVEL SECURITY', table_name);
        EXECUTE format('DROP POLICY IF EXISTS owner_maintenance ON public.%I', table_name);
        EXECUTE format('CREATE POLICY owner_maintenance ON public.%I TO %I USING (true) WITH CHECK (true)', table_name, current_user);
    END LOOP;
END $$;

DROP POLICY IF EXISTS special_read ON public.special;
CREATE POLICY special_read ON public.special FOR SELECT USING (
    app_security.actor_role() = 'SUPER_ADMIN' OR (active AND NOT archived)
);
DROP POLICY IF EXISTS special_insert ON public.special;
CREATE POLICY special_insert ON public.special FOR INSERT WITH CHECK (app_security.actor_role() = 'SUPER_ADMIN');
DROP POLICY IF EXISTS special_update ON public.special;
CREATE POLICY special_update ON public.special FOR UPDATE USING (app_security.actor_role() = 'SUPER_ADMIN')
    WITH CHECK (app_security.actor_role() = 'SUPER_ADMIN');
DROP POLICY IF EXISTS special_delete ON public.special;
CREATE POLICY special_delete ON public.special FOR DELETE USING (app_security.actor_role() = 'SUPER_ADMIN');

DO $$
DECLARE child text; predicate text;
BEGIN
    FOREACH child IN ARRAY ARRAY['special_day', 'special_component', 'special_addon'] LOOP
        predicate := format('EXISTS (SELECT 1 FROM public.special p WHERE p.special_id = %I.special_id)', child);
        EXECUTE format('DROP POLICY IF EXISTS special_child_read ON public.%I', child);
        EXECUTE format('CREATE POLICY special_child_read ON public.%I FOR SELECT USING (%s)', child, predicate);
        EXECUTE format('DROP POLICY IF EXISTS special_child_insert ON public.%I', child);
        EXECUTE format('CREATE POLICY special_child_insert ON public.%I FOR INSERT WITH CHECK (app_security.actor_role() = ''SUPER_ADMIN'' AND %s)', child, predicate);
        EXECUTE format('DROP POLICY IF EXISTS special_child_update ON public.%I', child);
        EXECUTE format('CREATE POLICY special_child_update ON public.%I FOR UPDATE USING (app_security.actor_role() = ''SUPER_ADMIN'' AND %s) WITH CHECK (app_security.actor_role() = ''SUPER_ADMIN'' AND %s)', child, predicate, predicate);
        EXECUTE format('DROP POLICY IF EXISTS special_child_delete ON public.%I', child);
        EXECUTE format('CREATE POLICY special_child_delete ON public.%I FOR DELETE USING (app_security.actor_role() = ''SUPER_ADMIN'' AND %s)', child, predicate);
    END LOOP;
END $$;

DO $$
DECLARE child text; predicate text;
BEGIN
    FOREACH child IN ARRAY ARRAY['special_component_menu_item', 'special_component_pizza'] LOOP
        predicate := format('EXISTS (SELECT 1 FROM public.special_component c WHERE c.special_component_id = %I.special_component_id)', child);
        EXECUTE format('DROP POLICY IF EXISTS special_option_read ON public.%I', child);
        EXECUTE format('CREATE POLICY special_option_read ON public.%I FOR SELECT USING (%s)', child, predicate);
        EXECUTE format('DROP POLICY IF EXISTS special_option_insert ON public.%I', child);
        EXECUTE format('CREATE POLICY special_option_insert ON public.%I FOR INSERT WITH CHECK (app_security.actor_role() = ''SUPER_ADMIN'' AND %s)', child, predicate);
        EXECUTE format('DROP POLICY IF EXISTS special_option_delete ON public.%I', child);
        EXECUTE format('CREATE POLICY special_option_delete ON public.%I FOR DELETE USING (app_security.actor_role() = ''SUPER_ADMIN'' AND %s)', child, predicate);
    END LOOP;
END $$;

DROP POLICY IF EXISTS child_read ON public.order_special_item;
CREATE POLICY child_read ON public.order_special_item FOR SELECT USING (
    EXISTS (SELECT 1 FROM public.customer_order p WHERE p.order_id = order_special_item.order_id)
);
DROP POLICY IF EXISTS child_insert ON public.order_special_item;
CREATE POLICY child_insert ON public.order_special_item FOR INSERT WITH CHECK (
    app_security.actor_role() IN ('USER', 'SUPER_ADMIN')
    AND EXISTS (SELECT 1 FROM public.customer_order p WHERE p.order_id = order_special_item.order_id)
);
DROP POLICY IF EXISTS child_update ON public.order_special_item;
CREATE POLICY child_update ON public.order_special_item FOR UPDATE USING (
    app_security.actor_role() = 'SUPER_ADMIN'
    AND EXISTS (SELECT 1 FROM public.customer_order p WHERE p.order_id = order_special_item.order_id)
) WITH CHECK (
    app_security.actor_role() = 'SUPER_ADMIN'
    AND EXISTS (SELECT 1 FROM public.customer_order p WHERE p.order_id = order_special_item.order_id)
);
DROP POLICY IF EXISTS child_delete ON public.order_special_item;
CREATE POLICY child_delete ON public.order_special_item FOR DELETE USING (
    app_security.actor_role() = 'SUPER_ADMIN'
    AND EXISTS (SELECT 1 FROM public.customer_order p WHERE p.order_id = order_special_item.order_id)
);

DO $$
DECLARE child text; parent text; parent_key text; predicate text;
BEGIN
    FOR child, parent, parent_key IN SELECT * FROM (VALUES
        ('order_special_selection', 'order_special_item', 'order_special_item_id'),
        ('order_pizza_item_removed_ingredient', 'order_pizza_item', 'order_pizza_item_id')
    ) AS relationships(child, parent, parent_key) LOOP
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

DO $$
DECLARE table_name text; arguments text[];
BEGIN
    FOR table_name, arguments IN SELECT * FROM (VALUES
        ('special_day', ARRAY['special_id']),
        ('special_component', ARRAY['special_id']),
        ('special_component_menu_item', ARRAY['special_component_id']),
        ('special_component_pizza', ARRAY['special_component_id']),
        ('special_addon', ARRAY['special_id']),
        ('order_special_item', ARRAY['order_id']),
        ('order_special_selection', ARRAY['order_special_item_id', 'order_menu_item_id', 'order_pizza_item_id']),
        ('order_pizza_item_removed_ingredient', ARRAY['order_pizza_item_id'])
    ) AS relationships(table_name, arguments) LOOP
        EXECUTE format('DROP TRIGGER IF EXISTS guard_order_relationship ON public.%I', table_name);
        EXECUTE format('CREATE TRIGGER guard_order_relationship BEFORE UPDATE ON public.%I FOR EACH ROW EXECUTE FUNCTION app_security.guard_order_relationship(%s)',
            table_name, (SELECT string_agg(quote_literal(value), ', ') FROM unnest(arguments) value));
    END LOOP;
END $$;

CREATE INDEX IF NOT EXISTS idx_order_special_parent ON public.order_special_item(order_id);
CREATE INDEX IF NOT EXISTS idx_order_special_selection_parent ON public.order_special_selection(order_special_item_id);
CREATE INDEX IF NOT EXISTS idx_removed_pizza_ingredient_parent ON public.order_pizza_item_removed_ingredient(order_pizza_item_id);

DO $$
DECLARE runtime_role text := current_setting('app.runtime_role'); object_name text;
BEGIN
    FOREACH object_name IN ARRAY ARRAY[
        'special', 'special_day', 'special_component', 'special_component_menu_item',
        'special_component_pizza', 'special_addon', 'order_special_item',
        'order_special_selection', 'order_pizza_item_removed_ingredient'
    ] LOOP
        EXECUTE format('REVOKE ALL ON public.%I FROM PUBLIC, %I', object_name, runtime_role);
        EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON public.%I TO %I', object_name, runtime_role);
    END LOOP;
    FOR object_name IN SELECT c.relname FROM pg_class c JOIN pg_namespace n ON c.relnamespace=n.oid
        WHERE n.nspname='public' AND c.relkind='S' AND EXISTS (
            SELECT 1 FROM pg_depend d JOIN pg_class t ON t.oid=d.refobjid
            WHERE d.objid=c.oid AND d.deptype IN ('a', 'i') AND t.relkind='r'
                AND has_table_privilege(runtime_role, t.oid, 'INSERT')
        ) LOOP
        EXECUTE format('GRANT USAGE, SELECT ON SEQUENCE public.%I TO %I', object_name, runtime_role);
    END LOOP;
END $$;
