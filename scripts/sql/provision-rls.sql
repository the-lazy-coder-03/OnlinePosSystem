\set ON_ERROR_STOP on
-- Run as the database administrator against the application database only.
-- psql reads passwords from the environment, not process arguments or committed files.
\getenv owner_name MIGRATION_DATASOURCE_USERNAME
\getenv owner_password MIGRATION_DATASOURCE_PASSWORD
\getenv runtime_name SPRING_DATASOURCE_USERNAME
\getenv runtime_password SPRING_DATASOURCE_PASSWORD
BEGIN;
SELECT format('CREATE ROLE %I LOGIN', :'owner_name') WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname=:'owner_name') \gexec
SELECT format('CREATE ROLE %I LOGIN', :'runtime_name') WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname=:'runtime_name') \gexec
SELECT format('ALTER ROLE %I NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE NOREPLICATION LOGIN PASSWORD %L', :'owner_name', :'owner_password') \gexec
SELECT format('ALTER ROLE %I NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE NOREPLICATION NOINHERIT LOGIN PASSWORD %L', :'runtime_name', :'runtime_password') \gexec
SELECT format('REVOKE %I FROM %I', :'owner_name', :'runtime_name') \gexec
SELECT format('GRANT CONNECT ON DATABASE %I TO %I, %I', current_database(), :'owner_name', :'runtime_name') \gexec
SELECT format('GRANT CREATE ON DATABASE %I TO %I', current_database(), :'owner_name') \gexec
SELECT format('REVOKE CREATE ON DATABASE %I FROM PUBLIC, %I', current_database(), :'runtime_name') \gexec
SELECT format('ALTER SCHEMA public OWNER TO %I', :'owner_name') \gexec
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
SELECT format('REVOKE CREATE ON SCHEMA public FROM %I', :'runtime_name') \gexec
-- A dedicated application database is required. Transfer only recognized application objects.
SELECT format('ALTER TABLE public.%I OWNER TO %I', tablename, :'owner_name')
FROM pg_tables WHERE schemaname='public' AND tablename IN (
    'customers','customer_notes','password_reset_tokens','staff','orders','customer_order','order_menu_item','order_menu_item_extra',
    'order_burger_protein','order_burger_removed_component','order_burger_extra_component','order_pizza_item',
    'order_pizza_item_extra','order_pizza_item_base_option','app_migration_state','branch','menu_category','menu_item',
    'branch_menu_item_price','modifier_group','modifier_option','menu_item_modifier_group','burger_component',
    'burger_recipe','burger_recipe_component','burger_recipe_assignment','burger_item_default_component',
    'branch_burger_component_price','salad_ingredients','pizza_category','pizza','pizza_size','pizza_allowed_size',
    'price_category','ingredient','pizza_default_ingredient','branch_pizza_price','branch_extra_price',
    'pizza_base_option','branch_pizza_base_option_price') \gexec
SELECT format('ALTER VIEW public.%I OWNER TO %I', viewname, :'owner_name') FROM pg_views
WHERE schemaname='public' AND viewname IN ('v_burger_builder_defaults','v_burger_builder_proteins','v_burger_builder_extras') \gexec
SELECT format('ALTER FUNCTION %s OWNER TO %I', p.oid::regprocedure, :'owner_name')
FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace WHERE n.nspname='public'
AND p.proname IN ('validate_order_burger_protein','validate_removed_burger_component','validate_extra_burger_component') \gexec
COMMIT;
