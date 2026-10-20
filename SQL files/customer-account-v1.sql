-- Create the requested customer login once during deployment, as the schema owner.
-- Preserve an existing account and any subsequent password or profile changes.
INSERT INTO public.customers (email, password, first_name, last_name, role, access_level, environment_admin)
SELECT 'customer', '{bcrypt}$2a$10$8Eh8T84aqQ2D8akCOe3IVegrD6WOqfWNRxvaxLChrTH8AjVyFUOg6',
       'Customer', 'Account', 'USER', 0, false
WHERE NOT EXISTS (
    SELECT 1 FROM public.customers WHERE lower(btrim(email)) = 'customer' OR phone1 = 'customer'
)
ON CONFLICT DO NOTHING;
