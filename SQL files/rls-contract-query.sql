-- Read with search_path=pg_catalog so PostgreSQL emits fully qualified definitions.
-- The expected contract is shipped with the application, never learned from a live database.
WITH protected AS (
    SELECT c.oid, c.relname, c.relowner
    FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = 'public' AND c.relname IN (
        'customers', 'customer_order', 'order_menu_item', 'order_menu_item_extra',
        'order_burger_protein', 'order_burger_removed_component', 'order_burger_extra_component',
        'order_pizza_item', 'order_pizza_item_extra', 'order_pizza_item_base_option',
        'customer_notes', 'staff', 'password_reset_tokens',
        'special', 'special_day', 'special_component', 'special_component_menu_item',
        'special_component_pizza', 'special_addon', 'order_special_item',
        'order_special_selection', 'order_pizza_item_removed_ingredient'
    )
)
SELECT jsonb_build_object(
    'policies', (SELECT jsonb_object_agg(c.relname || '.' || p.polname, jsonb_build_object(
        'command', p.polcmd, 'permissive', p.polpermissive,
        'roles', (SELECT jsonb_agg(CASE WHEN role_id = 0 THEN 'PUBLIC'
            WHEN role_id = c.relowner THEN 'TABLE_OWNER' ELSE 'ROLE:' || pg_get_userbyid(role_id) END ORDER BY role_id)
            FROM unnest(p.polroles) role_id),
        'using', pg_get_expr(p.polqual, p.polrelid),
        'check', pg_get_expr(p.polwithcheck, p.polrelid)))
        FROM protected c JOIN pg_policy p ON p.polrelid = c.oid),
    'triggers', (SELECT jsonb_object_agg(c.relname || '.' || t.tgname, jsonb_build_object(
        'enabled', t.tgenabled, 'definition', pg_get_triggerdef(t.oid)))
        FROM protected c JOIN pg_trigger t ON t.tgrelid = c.oid WHERE NOT t.tgisinternal),
    'functions', (SELECT jsonb_object_agg(p.proname || '(' || pg_get_function_identity_arguments(p.oid) || ')',
        pg_get_functiondef(p.oid)) FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
        WHERE n.nspname = 'app_security')
)
