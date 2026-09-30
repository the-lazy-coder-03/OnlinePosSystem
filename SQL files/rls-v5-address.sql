-- Immutable address metadata upgrade.
-- Keep earlier RLS migrations unchanged so existing databases can verify their
-- recorded checksums before this ordered migration is applied.

ALTER TABLE public.customers
    ADD COLUMN IF NOT EXISTS google_place_id varchar(255),
    ADD COLUMN IF NOT EXISTS formatted_address text,
    ADD COLUMN IF NOT EXISTS latitude numeric(9,6),
    ADD COLUMN IF NOT EXISTS longitude numeric(10,6),
    ADD COLUMN IF NOT EXISTS province varchar(120),
    ADD COLUMN IF NOT EXISTS country varchar(120);

ALTER TABLE public.customer_order
    ADD COLUMN IF NOT EXISTS google_place_id varchar(255),
    ADD COLUMN IF NOT EXISTS formatted_address text,
    ADD COLUMN IF NOT EXISTS latitude numeric(9,6),
    ADD COLUMN IF NOT EXISTS longitude numeric(10,6),
    ADD COLUMN IF NOT EXISTS province varchar(120),
    ADD COLUMN IF NOT EXISTS country varchar(120);

DROP FUNCTION IF EXISTS app_security.register_customer(
    text, text, text, text, text, text, text, text, text, text, text, text,
    text, timestamp without time zone
);

CREATE OR REPLACE FUNCTION app_security.register_customer(
    first_name_value text, last_name_value text, email_value text, password_value text,
    phone1_value text, phone2_value text, house_value text, street_value text, area_value text,
    complex_value text, store_value text, postal_value text, city_value text, ordered_at timestamp,
    google_place_id_value text, formatted_address_value text, latitude_value numeric, longitude_value numeric,
    province_value text, country_value text
) RETURNS bigint LANGUAGE sql SECURITY DEFINER SET search_path = pg_catalog, pg_temp AS $$
    INSERT INTO public.customers (first_name, last_name, email, password, phone1, phone2,
        house_number, street, area, complex_name, preferred_store, postal_code, city, last_ordered_at,
        google_place_id, formatted_address, latitude, longitude, province, country, role, access_level)
    VALUES (first_name_value, last_name_value, email_value, password_value, phone1_value, phone2_value,
        house_value, street_value, area_value, complex_value, store_value, postal_value, city_value, ordered_at,
        google_place_id_value, formatted_address_value, latitude_value, longitude_value, province_value, country_value, 'USER', 0)
    RETURNING id
$$;

REVOKE ALL ON FUNCTION app_security.register_customer(
    text, text, text, text, text, text, text, text, text, text, text, text,
    text, timestamp without time zone, text, text, numeric, numeric, text, text
) FROM PUBLIC;

DO $$
DECLARE runtime_role text := current_setting('app.runtime_role');
BEGIN
    EXECUTE format(
        'GRANT EXECUTE ON FUNCTION app_security.register_customer(text,text,text,text,text,text,text,text,text,text,text,text,text,timestamp without time zone,text,text,numeric,numeric,text,text) TO %I',
        runtime_role
    );
END $$;
