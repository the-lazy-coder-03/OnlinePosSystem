-- =========================================================
-- ONLINEPOS - FOOD CATALOG MIGRATION (ORDER HISTORY KEPT)
-- - Clears aborted transaction state
-- - Creates missing catalog/order tables without dropping existing data
-- - Updates food/menu catalog rows in place
-- - Stores each burger component only once
-- - Uses one reusable Standard Burger recipe
-- - Supports product-specific default additions without duplicating recipes
-- - Requires one protein type per burger
-- - Mega Burger uses two portions of the same selected protein
-- - Stores only burger selections and customer changes on orders
--
-- BURGER PRODUCTS:
-- - Default Burger: Standard Burger recipe
-- - Cheese Burger: Standard Burger recipe + Cheese
-- - Mega Burger: Standard Burger recipe + two portions of one selected protein
-- - Steak Burger: Standard Burger recipe + required steak doneness choice
-- - Bacon and Cheese Burger: Standard Burger recipe + Bacon + Cheese
-- - Bacon and Egg Burger: Standard Burger recipe + Bacon + Egg
-- - Dagwood: Standard Burger recipe + Bacon + Egg + Cheese
-- - Hawaiian Burger: Standard Burger recipe + Pineapple + Cheese
-- - Every burger has a matching burger combo product
--
-- WARNING:
-- This migration is designed to preserve customer orders and order history.
-- Do not add DROP TABLE statements here unless you intentionally want to
-- remove production data.
-- =========================================================

-- 1) Clear any aborted transaction state (safe even if none open)
ROLLBACK;

BEGIN;

-- =========================================================
-- 2) RECREATE VIEWS ONLY
-- =========================================================

-- Views
DROP VIEW IF EXISTS v_burger_builder_defaults CASCADE;
DROP VIEW IF EXISTS v_burger_builder_proteins CASCADE;
DROP VIEW IF EXISTS v_burger_builder_extras   CASCADE;

-- =========================================================
-- 3) CUSTOMERS (CREATE IF NOT EXISTS, LIKE-FOR-LIKE)
--    (We are NOT dropping it, but we ensure it exists)
-- =========================================================

CREATE SEQUENCE IF NOT EXISTS public.customers_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE IF NOT EXISTS public.customers (
                                                id              bigint NOT NULL DEFAULT nextval('public.customers_id_seq'::regclass),
                                                phone1          varchar(20),
                                                phone2          varchar(20),
                                                email           varchar(255),
                                                house_number    varchar(50),
                                                street          varchar(255),
                                                area            varchar(255),
                                                complex_name    varchar(255),
                                                last_ordered_at timestamp without time zone,
                                                preferred_store text,
                                                city            varchar(255),
                                                password        varchar(255),
                                                postal_code     varchar(255),
                                                first_name      varchar(255),
                                                last_name       varchar(255),
                                                role            varchar(255) NOT NULL DEFAULT 'USER',

                                                CONSTRAINT customers_pkey PRIMARY KEY (id),
                                                CONSTRAINT customers_email_key UNIQUE (email),
                                                CONSTRAINT unique_phone1 UNIQUE (phone1)
);

ALTER SEQUENCE public.customers_id_seq OWNED BY public.customers.id;

-- Ensure the role column also works correctly when this migration runs
-- against an existing customers table created before the USER default.
ALTER TABLE public.customers
    ADD COLUMN IF NOT EXISTS role varchar(255);

UPDATE public.customers
SET role = 'USER'
WHERE role IS NULL OR btrim(role) = '';

ALTER TABLE public.customers
    ALTER COLUMN role SET DEFAULT 'USER',
    ALTER COLUMN role SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_customers_email  ON public.customers (email);
CREATE INDEX IF NOT EXISTS idx_customers_phone1 ON public.customers (phone1);
CREATE INDEX IF NOT EXISTS idx_customers_phone2 ON public.customers (phone2);

-- =========================================================
-- 3.1) PASSWORD RESET TOKENS
--      Stores only token hashes. Existing raw reset tokens are short-lived and
--      are invalidated when moving to token_hash.
-- =========================================================

CREATE TABLE IF NOT EXISTS public.password_reset_tokens (
                                                            id          BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                                                            customer_id BIGINT NOT NULL REFERENCES public.customers(id) ON DELETE CASCADE,
                                                            token_hash  VARCHAR(64) NOT NULL,
                                                            expires_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                                                            used        BOOLEAN NOT NULL DEFAULT FALSE,
                                                            created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE public.password_reset_tokens
    ADD COLUMN IF NOT EXISTS token_hash VARCHAR(64);

ALTER TABLE public.password_reset_tokens
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE public.password_reset_tokens
    ADD COLUMN IF NOT EXISTS used BOOLEAN NOT NULL DEFAULT FALSE;

DELETE FROM public.password_reset_tokens
WHERE token_hash IS NULL;

ALTER TABLE public.password_reset_tokens
    ALTER COLUMN token_hash TYPE VARCHAR(64),
    ALTER COLUMN token_hash SET NOT NULL,
    ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN used SET DEFAULT FALSE,
    ALTER COLUMN used SET NOT NULL;

ALTER TABLE public.password_reset_tokens
    DROP COLUMN IF EXISTS token;

CREATE UNIQUE INDEX IF NOT EXISTS uq_password_reset_tokens_token_hash
    ON public.password_reset_tokens (token_hash);

CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_customer_used
    ON public.password_reset_tokens (customer_id, used);

-- =========================================================
-- 4) CORE TABLES
-- =========================================================

CREATE TABLE IF NOT EXISTS branch (
                                      branch_id INT PRIMARY KEY,
                                      name      TEXT NOT NULL UNIQUE,
                                      active    BOOLEAN NOT NULL DEFAULT TRUE
);

-- =========================================================
-- 5) MENU (BURGERS / PASTAS / DRINKS / SIDES etc.)
-- =========================================================

CREATE TABLE IF NOT EXISTS menu_category (
                                             id         INT PRIMARY KEY,
                                             name       TEXT NOT NULL UNIQUE,
                                             sort_order INT NOT NULL DEFAULT 0,
                                             active     BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS menu_item (
                                         id          INT PRIMARY KEY,
                                         category_id INT NOT NULL REFERENCES menu_category(id) ON UPDATE RESTRICT ON DELETE RESTRICT,
                                         name        TEXT NOT NULL,
                                         description TEXT,
                                         active      BOOLEAN NOT NULL DEFAULT TRUE,
                                         sort_order  INT NOT NULL DEFAULT 0,
                                         is_300ml    BOOLEAN NOT NULL DEFAULT FALSE,
                                         is_2l       BOOLEAN NOT NULL DEFAULT FALSE,
                                         UNIQUE (category_id, name)
);

CREATE TABLE IF NOT EXISTS branch_menu_item_price (
                                                      branch_id    INT NOT NULL REFERENCES branch(branch_id) ON UPDATE RESTRICT ON DELETE RESTRICT,
                                                      menu_item_id INT NOT NULL REFERENCES menu_item(id)     ON UPDATE RESTRICT ON DELETE RESTRICT,
                                                      price        NUMERIC(10,2) NOT NULL CHECK (price >= 0),
                                                      PRIMARY KEY (branch_id, menu_item_id)
);

CREATE TABLE IF NOT EXISTS modifier_group (
                                              id         INT PRIMARY KEY,
                                              name       TEXT NOT NULL UNIQUE,
                                              required   BOOLEAN NOT NULL DEFAULT FALSE,
                                              min_select INT NOT NULL DEFAULT 0,
                                              max_select INT NOT NULL DEFAULT 1,
                                              CONSTRAINT chk_modifier_group_selection CHECK (
                                                  min_select >= 0 AND max_select >= min_select
                                                  )
);

CREATE TABLE IF NOT EXISTS modifier_option (
                                               id           INT PRIMARY KEY,
                                               group_id     INT NOT NULL REFERENCES modifier_group(id) ON DELETE CASCADE,
                                               name         TEXT NOT NULL,
                                               menu_item_id INT REFERENCES menu_item(id) ON DELETE SET NULL,
                                               additional_price NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (additional_price >= 0),
                                               UNIQUE (group_id, name)
);

ALTER TABLE modifier_option
    ADD COLUMN IF NOT EXISTS additional_price NUMERIC(10,2) NOT NULL DEFAULT 0;

ALTER TABLE modifier_option
    ALTER COLUMN additional_price TYPE NUMERIC(10,2) USING additional_price::NUMERIC(10,2),
    ALTER COLUMN additional_price SET DEFAULT 0;

UPDATE modifier_option
SET additional_price = 0
WHERE additional_price IS NULL;

ALTER TABLE modifier_option
    ALTER COLUMN additional_price SET NOT NULL;

ALTER TABLE modifier_option
    DROP CONSTRAINT IF EXISTS chk_modifier_option_additional_price;

ALTER TABLE modifier_option
    ADD CONSTRAINT chk_modifier_option_additional_price CHECK (additional_price >= 0);

CREATE TABLE IF NOT EXISTS menu_item_modifier_group (
                                                        menu_item_id INT NOT NULL REFERENCES menu_item(id) ON DELETE CASCADE,
                                                        group_id     INT NOT NULL REFERENCES modifier_group(id) ON DELETE CASCADE,
                                                        PRIMARY KEY (menu_item_id, group_id)
);

-- Every burger component is stored once.
CREATE TABLE IF NOT EXISTS burger_component (
                                                component_id   INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                                                name           TEXT NOT NULL UNIQUE,
                                                component_type TEXT NOT NULL CHECK (
                                                    component_type IN ('default_topping', 'protein', 'extra_topping')
                                                    ),
                                                active         BOOLEAN NOT NULL DEFAULT TRUE,
                                                seasonal       BOOLEAN NOT NULL DEFAULT FALSE
);

-- Reusable burger recipes.
CREATE TABLE IF NOT EXISTS burger_recipe (
                                             recipe_id INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                                             name      TEXT NOT NULL UNIQUE,
                                             active    BOOLEAN NOT NULL DEFAULT TRUE
);

-- Default components included in a reusable recipe.
CREATE TABLE IF NOT EXISTS burger_recipe_component (
                                                       recipe_id     INT NOT NULL REFERENCES burger_recipe(recipe_id) ON DELETE CASCADE,
                                                       component_id  INT NOT NULL REFERENCES burger_component(component_id) ON DELETE RESTRICT,
                                                       is_removable  BOOLEAN NOT NULL DEFAULT TRUE,
                                                       sort_order    INT NOT NULL DEFAULT 0,
                                                       PRIMARY KEY (recipe_id, component_id)
);

-- Assigns a reusable recipe and optional protein rule to each burger.
-- Most burgers require exactly one protein type. Steak burgers instead use a
-- required steak doneness modifier group.
CREATE TABLE IF NOT EXISTS burger_recipe_assignment (
                                                        burger_id                   INT PRIMARY KEY REFERENCES menu_item(id) ON DELETE CASCADE,
                                                        recipe_id                   INT NOT NULL REFERENCES burger_recipe(recipe_id) ON DELETE RESTRICT,
                                                        protein_quantity_required   INT NOT NULL DEFAULT 1 CHECK (protein_quantity_required > 0),
                                                        protein_required            BOOLEAN NOT NULL DEFAULT TRUE
);

ALTER TABLE burger_recipe_assignment
    ADD COLUMN IF NOT EXISTS protein_required BOOLEAN NOT NULL DEFAULT TRUE;

-- Product-specific default additions.
-- This stores only differences from the shared Standard Burger recipe.
-- For example, Cheese Burger adds Cheese, while Bacon and Cheese Burger
-- adds Cheese and Bacon. The shared standard toppings are not duplicated.
CREATE TABLE IF NOT EXISTS burger_item_default_component (
                                                             burger_id      INT NOT NULL REFERENCES menu_item(id) ON DELETE CASCADE,
                                                             component_id   INT NOT NULL REFERENCES burger_component(component_id) ON DELETE RESTRICT,
                                                             is_removable   BOOLEAN NOT NULL DEFAULT TRUE,
                                                             sort_order     INT NOT NULL DEFAULT 100,
                                                             PRIMARY KEY (burger_id, component_id)
);

-- Optional branch-specific prices for protein choices and extra toppings.
CREATE TABLE IF NOT EXISTS branch_burger_component_price (
                                                             branch_id     INT NOT NULL REFERENCES branch(branch_id) ON DELETE CASCADE,
                                                             component_id  INT NOT NULL REFERENCES burger_component(component_id) ON DELETE CASCADE,
                                                             price         NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (price >= 0),
                                                             PRIMARY KEY (branch_id, component_id)
);

CREATE TABLE IF NOT EXISTS salad_ingredients (
                                                 id              INT PRIMARY KEY,
                                                 salad_id        INT NOT NULL REFERENCES menu_item(id) ON UPDATE RESTRICT ON DELETE CASCADE,
                                                 ingredient_name TEXT NOT NULL,
                                                 price           NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (price >= 0),
                                                 UNIQUE (salad_id, ingredient_name)
);

-- =========================================================
-- 6) PIZZA CATALOG
-- =========================================================

CREATE TABLE IF NOT EXISTS pizza_category (
                                              pizza_category_id INT PRIMARY KEY,
                                              name              TEXT NOT NULL UNIQUE,
                                              sort_order        INT NOT NULL DEFAULT 0,
                                              active            BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS pizza (
                                     pizza_id          INT PRIMARY KEY,
                                     pizza_category_id INT NOT NULL REFERENCES pizza_category(pizza_category_id) ON UPDATE RESTRICT ON DELETE RESTRICT,
                                     name              TEXT NOT NULL,
                                     description       TEXT,
                                     sort_order        INT NOT NULL DEFAULT 0,
                                     active            BOOLEAN NOT NULL DEFAULT TRUE,
                                     CONSTRAINT uq_pizza_name UNIQUE (pizza_category_id, name)
);

CREATE TABLE IF NOT EXISTS pizza_size (
                                          pizza_size_id INT PRIMARY KEY,
                                          cm            INT NOT NULL UNIQUE,
                                          sort_order    INT NOT NULL DEFAULT 0,
                                          active        BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS pizza_allowed_size (
                                                  pizza_id      INT NOT NULL REFERENCES pizza(pizza_id) ON DELETE CASCADE,
                                                  pizza_size_id INT NOT NULL REFERENCES pizza_size(pizza_size_id) ON UPDATE RESTRICT ON DELETE RESTRICT,
                                                  PRIMARY KEY (pizza_id, pizza_size_id)
);

CREATE TABLE IF NOT EXISTS price_category (
                                              price_category_id INT PRIMARY KEY,
                                              name              TEXT NOT NULL UNIQUE,
                                              sort_order        INT NOT NULL DEFAULT 0,
                                              active            BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS ingredient (
                                          ingredient_id     INT PRIMARY KEY,
                                          name              TEXT NOT NULL UNIQUE,
                                          price_category_id INT NOT NULL REFERENCES price_category(price_category_id) ON UPDATE RESTRICT ON DELETE RESTRICT,
                                          active            BOOLEAN NOT NULL DEFAULT TRUE,
                                          seasonal          BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS pizza_default_ingredient (
                                                        pizza_id      INT NOT NULL REFERENCES pizza(pizza_id) ON DELETE CASCADE,
                                                        ingredient_id INT NOT NULL REFERENCES ingredient(ingredient_id) ON UPDATE RESTRICT ON DELETE RESTRICT,
                                                        is_removable  BOOLEAN NOT NULL DEFAULT TRUE,
                                                        default_qty   INT NOT NULL DEFAULT 1 CHECK (default_qty > 0),
                                                        sort_order    INT NOT NULL DEFAULT 0,
                                                        PRIMARY KEY (pizza_id, ingredient_id)
);

CREATE TABLE IF NOT EXISTS branch_pizza_price (
                                                  branch_id     INT NOT NULL REFERENCES branch(branch_id) ON DELETE CASCADE,
                                                  pizza_id      INT NOT NULL REFERENCES pizza(pizza_id) ON DELETE CASCADE,
                                                  pizza_size_id INT NOT NULL REFERENCES pizza_size(pizza_size_id) ON UPDATE RESTRICT ON DELETE RESTRICT,
                                                  price         NUMERIC(10,2) NOT NULL CHECK (price >= 0),
                                                  PRIMARY KEY (branch_id, pizza_id, pizza_size_id)
);

CREATE TABLE IF NOT EXISTS branch_extra_price (
                                                  branch_id         INT NOT NULL REFERENCES branch(branch_id) ON DELETE CASCADE,
                                                  price_category_id INT NOT NULL REFERENCES price_category(price_category_id) ON UPDATE RESTRICT ON DELETE RESTRICT,
                                                  pizza_size_id     INT NOT NULL REFERENCES pizza_size(pizza_size_id) ON UPDATE RESTRICT ON DELETE RESTRICT,
                                                  price             NUMERIC(10,2) NOT NULL CHECK (price >= 0),
                                                  PRIMARY KEY (branch_id, price_category_id, pizza_size_id)
);

-- Pizza base choices are separate from toppings. This supports the Kenridge
-- wheat/gluten-free large-base surcharge without pretending it is a topping.
CREATE TABLE IF NOT EXISTS pizza_base_option (
                                                 pizza_base_option_id INT PRIMARY KEY,
                                                 name                 TEXT NOT NULL UNIQUE,
                                                 active               BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS branch_pizza_base_option_price (
                                                              branch_id            INT NOT NULL REFERENCES branch(branch_id) ON DELETE CASCADE,
                                                              pizza_base_option_id INT NOT NULL REFERENCES pizza_base_option(pizza_base_option_id) ON DELETE CASCADE,
                                                              pizza_size_id        INT NOT NULL REFERENCES pizza_size(pizza_size_id) ON DELETE CASCADE,
                                                              price                NUMERIC(10,2) NOT NULL CHECK (price >= 0),
                                                              PRIMARY KEY (branch_id, pizza_base_option_id, pizza_size_id)
);

-- =========================================================
-- 7) ORDERING (ONE HEADER TABLE)
-- =========================================================

CREATE TABLE IF NOT EXISTS customer_order (
                                              order_id     BIGSERIAL PRIMARY KEY,
                                              branch_id    INT NOT NULL REFERENCES branch(branch_id) ON UPDATE RESTRICT ON DELETE RESTRICT,
                                              customer_id  BIGINT REFERENCES public.customers(id) ON UPDATE RESTRICT ON DELETE SET NULL,

                                              order_type   TEXT NOT NULL DEFAULT 'pickup',
                                              status       TEXT NOT NULL DEFAULT 'created',
                                              created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- optional contact snapshot
                                              customer_name TEXT,
                                              phone        TEXT,
                                              notes        TEXT
);

-- Menu order lines
CREATE TABLE IF NOT EXISTS order_menu_item (
                                               order_menu_item_id BIGSERIAL PRIMARY KEY,
                                               order_id           BIGINT NOT NULL REFERENCES customer_order(order_id) ON DELETE CASCADE,
                                               menu_item_id       INT NOT NULL REFERENCES menu_item(id) ON UPDATE RESTRICT ON DELETE RESTRICT,
                                               qty                INT NOT NULL DEFAULT 1 CHECK (qty > 0),
                                               unit_price_at_time NUMERIC(10,2) NOT NULL CHECK (unit_price_at_time >= 0),
                                               item_name_at_time  TEXT,
                                               notes              TEXT
);

-- Generic extras for non-burger menu items.
CREATE TABLE IF NOT EXISTS order_menu_item_extra (
                                                     order_menu_item_extra_id BIGSERIAL PRIMARY KEY,
                                                     order_menu_item_id       BIGINT NOT NULL REFERENCES order_menu_item(order_menu_item_id) ON DELETE CASCADE,
                                                     name                     TEXT NOT NULL,
                                                     qty                      INT NOT NULL DEFAULT 1 CHECK (qty > 0),
                                                     unit_price_at_time       NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (unit_price_at_time >= 0)
);

-- One selected protein type per burger line.
-- protein_qty_per_burger is 2 for the Mega Burger, ensuring that both
-- protein portions are the same selected component.
CREATE TABLE IF NOT EXISTS order_burger_protein (
                                                    order_menu_item_id       BIGINT PRIMARY KEY
                                                        REFERENCES order_menu_item(order_menu_item_id) ON DELETE CASCADE,
                                                    component_id             INT NOT NULL
                                                        REFERENCES burger_component(component_id) ON UPDATE RESTRICT ON DELETE RESTRICT,
                                                    protein_qty_per_burger   INT NOT NULL CHECK (protein_qty_per_burger > 0),
                                                    unit_price_at_time       NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (unit_price_at_time >= 0)
);

-- Stores only default components the customer removed.
CREATE TABLE IF NOT EXISTS order_burger_removed_component (
                                                              order_menu_item_id BIGINT NOT NULL
                                                                  REFERENCES order_menu_item(order_menu_item_id) ON DELETE CASCADE,
                                                              component_id       INT NOT NULL
                                                                  REFERENCES burger_component(component_id) ON UPDATE RESTRICT ON DELETE RESTRICT,
                                                              PRIMARY KEY (order_menu_item_id, component_id)
);

-- Stores only optional toppings the customer added.
CREATE TABLE IF NOT EXISTS order_burger_extra_component (
                                                            order_menu_item_id BIGINT NOT NULL
                                                                REFERENCES order_menu_item(order_menu_item_id) ON DELETE CASCADE,
                                                            component_id       INT NOT NULL
                                                                REFERENCES burger_component(component_id) ON UPDATE RESTRICT ON DELETE RESTRICT,
                                                            qty                INT NOT NULL DEFAULT 1 CHECK (qty > 0),
                                                            unit_price_at_time NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (unit_price_at_time >= 0),
                                                            PRIMARY KEY (order_menu_item_id, component_id)
);

-- Pizza order lines
CREATE TABLE IF NOT EXISTS order_pizza_item (
                                                order_pizza_item_id  BIGSERIAL PRIMARY KEY,
                                                order_id             BIGINT NOT NULL REFERENCES customer_order(order_id) ON DELETE CASCADE,
                                                pizza_id             INT NOT NULL REFERENCES pizza(pizza_id) ON UPDATE RESTRICT ON DELETE RESTRICT,
                                                pizza_size_id        INT NOT NULL REFERENCES pizza_size(pizza_size_id) ON UPDATE RESTRICT ON DELETE RESTRICT,
                                                qty                  INT NOT NULL DEFAULT 1 CHECK (qty > 0),
                                                base_price_at_time   NUMERIC(10,2) NOT NULL CHECK (base_price_at_time >= 0),
                                                notes                TEXT
);

CREATE TABLE IF NOT EXISTS order_pizza_item_extra (
                                                      order_pizza_item_extra_id BIGSERIAL PRIMARY KEY,
                                                      order_pizza_item_id       BIGINT NOT NULL REFERENCES order_pizza_item(order_pizza_item_id) ON DELETE CASCADE,
                                                      ingredient_id             INT NOT NULL REFERENCES ingredient(ingredient_id) ON UPDATE RESTRICT ON DELETE RESTRICT,
                                                      qty                       INT NOT NULL DEFAULT 1 CHECK (qty > 0),
                                                      unit_price_at_time        NUMERIC(10,2) NOT NULL CHECK (unit_price_at_time >= 0)
);

-- Optional non-standard pizza base selected for an order line.
CREATE TABLE IF NOT EXISTS order_pizza_item_base_option (
                                                            order_pizza_item_id  BIGINT PRIMARY KEY REFERENCES order_pizza_item(order_pizza_item_id) ON DELETE CASCADE,
                                                            pizza_base_option_id INT NOT NULL REFERENCES pizza_base_option(pizza_base_option_id) ON UPDATE RESTRICT ON DELETE RESTRICT,
                                                            unit_price_at_time   NUMERIC(10,2) NOT NULL CHECK (unit_price_at_time >= 0)
);


-- =========================================================
-- 7.1) BURGER ORDER VALIDATION
-- =========================================================

-- Validates that the selected component is an active protein and that
-- the quantity matches the burger configuration.
CREATE OR REPLACE FUNCTION validate_order_burger_protein()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
DECLARE
    v_menu_item_id INT;
    v_required_qty INT;
    v_component_type TEXT;
    v_component_active BOOLEAN;
BEGIN
    SELECT menu_item_id
    INTO v_menu_item_id
    FROM order_menu_item
    WHERE order_menu_item_id = NEW.order_menu_item_id;

    IF v_menu_item_id IS NULL THEN
        RAISE EXCEPTION 'Order menu item % does not exist', NEW.order_menu_item_id;
    END IF;

    SELECT protein_quantity_required
    INTO v_required_qty
    FROM burger_recipe_assignment
    WHERE burger_id = v_menu_item_id;

    IF v_required_qty IS NULL THEN
        RAISE EXCEPTION 'Menu item % is not configured as a burger', v_menu_item_id;
    END IF;

    SELECT component_type, active
    INTO v_component_type, v_component_active
    FROM burger_component
    WHERE component_id = NEW.component_id;

    IF v_component_type IS DISTINCT FROM 'protein' OR v_component_active IS DISTINCT FROM TRUE THEN
        RAISE EXCEPTION 'Component % is not an active protein', NEW.component_id;
    END IF;

    IF NEW.protein_qty_per_burger <> v_required_qty THEN
        RAISE EXCEPTION
            'Burger % requires % portion(s) of one protein, but % was supplied',
            v_menu_item_id,
            v_required_qty,
            NEW.protein_qty_per_burger;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_validate_order_burger_protein ON order_burger_protein;

CREATE TRIGGER trg_validate_order_burger_protein
    BEFORE INSERT OR UPDATE
    ON order_burger_protein
    FOR EACH ROW
EXECUTE FUNCTION validate_order_burger_protein();

-- Validates that removed components are part of the burger's default recipe.
CREATE OR REPLACE FUNCTION validate_removed_burger_component()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
DECLARE
    v_valid BOOLEAN;
BEGIN
    SELECT EXISTS (
        -- Shared Standard Burger recipe components
        SELECT 1
        FROM order_menu_item omi
                 JOIN burger_recipe_assignment assignment
                      ON assignment.burger_id = omi.menu_item_id
                 JOIN burger_recipe_component recipe_component
                      ON recipe_component.recipe_id = assignment.recipe_id
        WHERE omi.order_menu_item_id = NEW.order_menu_item_id
          AND recipe_component.component_id = NEW.component_id
          AND recipe_component.is_removable = TRUE

        UNION ALL

        -- Product-specific defaults such as Cheese or Bacon
        SELECT 1
        FROM order_menu_item omi
                 JOIN burger_item_default_component item_component
                      ON item_component.burger_id = omi.menu_item_id
        WHERE omi.order_menu_item_id = NEW.order_menu_item_id
          AND item_component.component_id = NEW.component_id
          AND item_component.is_removable = TRUE
    )
    INTO v_valid;

    IF v_valid IS DISTINCT FROM TRUE THEN
        RAISE EXCEPTION
            'Component % is not a removable default component for order line %',
            NEW.component_id,
            NEW.order_menu_item_id;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_validate_removed_burger_component ON order_burger_removed_component;

CREATE TRIGGER trg_validate_removed_burger_component
    BEFORE INSERT OR UPDATE
    ON order_burger_removed_component
    FOR EACH ROW
EXECUTE FUNCTION validate_removed_burger_component();

-- Validates that added components are active optional burger toppings.
CREATE OR REPLACE FUNCTION validate_extra_burger_component()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
DECLARE
    v_component_type TEXT;
    v_component_active BOOLEAN;
    v_is_burger BOOLEAN;
BEGIN
    SELECT EXISTS (
        SELECT 1
        FROM order_menu_item omi
                 JOIN burger_recipe_assignment assignment
                      ON assignment.burger_id = omi.menu_item_id
        WHERE omi.order_menu_item_id = NEW.order_menu_item_id
    )
    INTO v_is_burger;

    IF v_is_burger IS DISTINCT FROM TRUE THEN
        RAISE EXCEPTION 'Order line % is not a burger', NEW.order_menu_item_id;
    END IF;

    SELECT component_type, active
    INTO v_component_type, v_component_active
    FROM burger_component
    WHERE component_id = NEW.component_id;

    IF v_component_type IS NULL
        OR v_component_type = 'protein'
        OR v_component_active IS DISTINCT FROM TRUE THEN
        RAISE EXCEPTION 'Component % is not an active burger topping extra', NEW.component_id;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_validate_extra_burger_component ON order_burger_extra_component;

CREATE TRIGGER trg_validate_extra_burger_component
    BEFORE INSERT OR UPDATE
    ON order_burger_extra_component
    FOR EACH ROW
EXECUTE FUNCTION validate_extra_burger_component();

-- Hibernate creates existing tables without the SQL defaults above when the app
-- starts against a fresh database, so ensure seed inserts can rely on them.
ALTER TABLE menu_category ALTER COLUMN active SET DEFAULT TRUE;
ALTER TABLE menu_item ALTER COLUMN active SET DEFAULT TRUE;
ALTER TABLE menu_item ALTER COLUMN sort_order SET DEFAULT 0;
ALTER TABLE ingredient ALTER COLUMN active SET DEFAULT TRUE;
ALTER TABLE pizza_category ALTER COLUMN active SET DEFAULT TRUE;
ALTER TABLE pizza ALTER COLUMN active SET DEFAULT TRUE;
ALTER TABLE pizza_size ALTER COLUMN active SET DEFAULT TRUE;
ALTER TABLE price_category ALTER COLUMN active SET DEFAULT TRUE;
ALTER TABLE pizza_default_ingredient ALTER COLUMN default_qty SET DEFAULT 1;
ALTER TABLE pizza_default_ingredient ALTER COLUMN is_removable SET DEFAULT TRUE;
ALTER TABLE pizza_default_ingredient ALTER COLUMN sort_order SET DEFAULT 0;

-- =========================================================
-- 8) SEED DATA (FIXED IDs)
-- =========================================================

-- 8.1 Branches
INSERT INTO branch (branch_id, name) VALUES
                                         (1, 'Kenridge'),
                                         (2, 'Uitzicht')
ON CONFLICT (branch_id) DO UPDATE
    SET name = EXCLUDED.name;

-- 8.2 Menu categories
INSERT INTO menu_category (id, name, sort_order) VALUES
                                                     (1, 'Burgers', 10),
                                                     (2, 'Burger Combos', 20),
                                                     (3, 'Pastas', 30),
                                                     (4, 'Cool Drinks', 40),
                                                     (5, 'Sides', 50),
                                                     (6, 'Sauces', 60),
                                                     (7, 'Salads', 70),
                                                     (8, 'Ribs', 80),
                                                     (9, 'Kiddies Meals', 90),
                                                     (10, 'Toasted Sandwiches', 100),
                                                     (11, 'Desserts', 110)
ON CONFLICT (id) DO UPDATE
    SET name = EXCLUDED.name,
        sort_order = EXCLUDED.sort_order,
        active = TRUE;

UPDATE menu_category
SET active = FALSE
WHERE id = 6;

-- 8.3 Menu items (fixed IDs)
INSERT INTO menu_item (id, category_id, name, description, is_300ml, is_2l) VALUES
                                                                                -- Drinks
                                                                                (101, 4, 'Coke 300ml', '300ml can', TRUE, FALSE),
                                                                                (102, 4, 'Coke 2L', '2 litre bottle', FALSE, TRUE),
                                                                                (103, 4, 'Coke Zero 300ml', '300ml zero sugar can', TRUE, FALSE),
