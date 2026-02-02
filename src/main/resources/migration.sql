-- =========================================================
-- ONLINEPOS - FULL MIGRATION (CUSTOMERS KEPT)
-- - Clears aborted tx state
-- - Drops/recreates all non-customers tables
-- - One branch table for everything
-- - One customer_order table referencing customers(id)
-- - Separate order lines for menu vs pizza
-- =========================================================

-- 1) Clear any aborted transaction state (safe even if none open)
ROLLBACK;

BEGIN;

-- =========================================================
-- 2) DROP (SAFE RE-RUNS) - DO NOT DROP customers
-- =========================================================

-- Order tables (depend on everything)
DROP TABLE IF EXISTS order_pizza_item_extra CASCADE;
DROP TABLE IF EXISTS order_pizza_item       CASCADE;
DROP TABLE IF EXISTS order_menu_item_extra  CASCADE;
DROP TABLE IF EXISTS order_menu_item        CASCADE;
DROP TABLE IF EXISTS customer_order         CASCADE;

-- Menu tables
DROP TABLE IF EXISTS salad_ingredients        CASCADE;
DROP TABLE IF EXISTS burger_toppings          CASCADE;
DROP TABLE IF EXISTS branch_menu_item_price   CASCADE;
DROP TABLE IF EXISTS menu_item                CASCADE;
DROP TABLE IF EXISTS menu_category            CASCADE;

-- Pizza tables
DROP TABLE IF EXISTS branch_extra_price       CASCADE;
DROP TABLE IF EXISTS branch_pizza_price       CASCADE;
DROP TABLE IF EXISTS pizza_default_ingredient CASCADE;
DROP TABLE IF EXISTS pizza_allowed_size       CASCADE;
DROP TABLE IF EXISTS ingredient               CASCADE;
DROP TABLE IF EXISTS price_category           CASCADE;
DROP TABLE IF EXISTS pizza                    CASCADE;
DROP TABLE IF EXISTS pizza_category           CASCADE;
DROP TABLE IF EXISTS pizza_size               CASCADE;

-- Core
DROP TABLE IF EXISTS branch                   CASCADE;

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

  CONSTRAINT customers_pkey PRIMARY KEY (id),
  CONSTRAINT customers_email_key UNIQUE (email),
  CONSTRAINT unique_phone1 UNIQUE (phone1)
);

ALTER SEQUENCE public.customers_id_seq OWNED BY public.customers.id;

CREATE INDEX IF NOT EXISTS idx_customers_email  ON public.customers (email);
CREATE INDEX IF NOT EXISTS idx_customers_phone1 ON public.customers (phone1);
CREATE INDEX IF NOT EXISTS idx_customers_phone2 ON public.customers (phone2);

-- =========================================================
-- 4) CORE TABLES
-- =========================================================

CREATE TABLE branch (
  branch_id INT PRIMARY KEY,
  name      TEXT NOT NULL UNIQUE,
  active    BOOLEAN NOT NULL DEFAULT TRUE
);

-- =========================================================
-- 5) MENU (BURGERS / PASTAS / DRINKS / SIDES etc.)
-- =========================================================

CREATE TABLE menu_category (
  id         INT PRIMARY KEY,
  name       TEXT NOT NULL UNIQUE,
  sort_order INT NOT NULL DEFAULT 0,
  active     BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE menu_item (
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

CREATE TABLE branch_menu_item_price (
  branch_id    INT NOT NULL REFERENCES branch(branch_id) ON UPDATE RESTRICT ON DELETE RESTRICT,
  menu_item_id INT NOT NULL REFERENCES menu_item(id)     ON UPDATE RESTRICT ON DELETE RESTRICT,
  price        NUMERIC(10,2) NOT NULL CHECK (price >= 0),
  PRIMARY KEY (branch_id, menu_item_id)
);

CREATE TABLE burger_toppings (
  id           INT PRIMARY KEY,
  burger_id    INT NOT NULL REFERENCES menu_item(id) ON UPDATE RESTRICT ON DELETE CASCADE,
  topping_name TEXT NOT NULL,
  is_default   BOOLEAN NOT NULL DEFAULT TRUE,
  price        NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (price >= 0),
  UNIQUE (burger_id, topping_name)
);

CREATE TABLE salad_ingredients (
  id              INT PRIMARY KEY,
  salad_id        INT NOT NULL REFERENCES menu_item(id) ON UPDATE RESTRICT ON DELETE CASCADE,
  ingredient_name TEXT NOT NULL,
  price           NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (price >= 0),
  UNIQUE (salad_id, ingredient_name)
);

-- =========================================================
-- 6) PIZZA CATALOG
-- =========================================================

CREATE TABLE pizza_category (
  pizza_category_id INT PRIMARY KEY,
  name              TEXT NOT NULL UNIQUE,
  sort_order        INT NOT NULL DEFAULT 0,
  active            BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE pizza (
  pizza_id          INT PRIMARY KEY,
  pizza_category_id INT NOT NULL REFERENCES pizza_category(pizza_category_id) ON UPDATE RESTRICT ON DELETE RESTRICT,
  name              TEXT NOT NULL,
  description       TEXT,
  sort_order        INT NOT NULL DEFAULT 0,
  active            BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT uq_pizza_name UNIQUE (pizza_category_id, name)
);

CREATE TABLE pizza_size (
  pizza_size_id INT PRIMARY KEY,
  cm            INT NOT NULL UNIQUE,
  sort_order    INT NOT NULL DEFAULT 0,
  active        BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE pizza_allowed_size (
  pizza_id      INT NOT NULL REFERENCES pizza(pizza_id) ON DELETE CASCADE,
  pizza_size_id INT NOT NULL REFERENCES pizza_size(pizza_size_id) ON UPDATE RESTRICT ON DELETE RESTRICT,
  PRIMARY KEY (pizza_id, pizza_size_id)
);

CREATE TABLE price_category (
  price_category_id INT PRIMARY KEY,
  name              TEXT NOT NULL UNIQUE,
  sort_order        INT NOT NULL DEFAULT 0,
  active            BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE ingredient (
  ingredient_id     INT PRIMARY KEY,
  name              TEXT NOT NULL UNIQUE,
  price_category_id INT NOT NULL REFERENCES price_category(price_category_id) ON UPDATE RESTRICT ON DELETE RESTRICT,
  active            BOOLEAN NOT NULL DEFAULT TRUE,
  seasonal          BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE pizza_default_ingredient (
  pizza_id      INT NOT NULL REFERENCES pizza(pizza_id) ON DELETE CASCADE,
  ingredient_id INT NOT NULL REFERENCES ingredient(ingredient_id) ON UPDATE RESTRICT ON DELETE RESTRICT,
  is_removable  BOOLEAN NOT NULL DEFAULT TRUE,
  default_qty   INT NOT NULL DEFAULT 1 CHECK (default_qty > 0),
  sort_order    INT NOT NULL DEFAULT 0,
  PRIMARY KEY (pizza_id, ingredient_id)
);

CREATE TABLE branch_pizza_price (
  branch_id     INT NOT NULL REFERENCES branch(branch_id) ON DELETE CASCADE,
  pizza_id      INT NOT NULL REFERENCES pizza(pizza_id) ON DELETE CASCADE,
  pizza_size_id INT NOT NULL REFERENCES pizza_size(pizza_size_id) ON UPDATE RESTRICT ON DELETE RESTRICT,
  price         NUMERIC(10,2) NOT NULL CHECK (price >= 0),
  PRIMARY KEY (branch_id, pizza_id, pizza_size_id)
);

CREATE TABLE branch_extra_price (
  branch_id         INT NOT NULL REFERENCES branch(branch_id) ON DELETE CASCADE,
  price_category_id INT NOT NULL REFERENCES price_category(price_category_id) ON UPDATE RESTRICT ON DELETE RESTRICT,
  pizza_size_id     INT NOT NULL REFERENCES pizza_size(pizza_size_id) ON UPDATE RESTRICT ON DELETE RESTRICT,
  price             NUMERIC(10,2) NOT NULL CHECK (price >= 0),
  PRIMARY KEY (branch_id, price_category_id, pizza_size_id)
);

-- =========================================================
-- 7) ORDERING (ONE HEADER TABLE)
-- =========================================================

CREATE TABLE customer_order (
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
CREATE TABLE order_menu_item (
  order_menu_item_id BIGSERIAL PRIMARY KEY,
  order_id           BIGINT NOT NULL REFERENCES customer_order(order_id) ON DELETE CASCADE,
  menu_item_id       INT NOT NULL REFERENCES menu_item(id) ON UPDATE RESTRICT ON DELETE RESTRICT,
  qty                INT NOT NULL DEFAULT 1 CHECK (qty > 0),
  unit_price_at_time NUMERIC(10,2) NOT NULL CHECK (unit_price_at_time >= 0),
  notes              TEXT
);

-- Extras for menu item lines (e.g., burger extra cheese, salad extra ingredient)
CREATE TABLE order_menu_item_extra (
  order_menu_item_extra_id BIGSERIAL PRIMARY KEY,
  order_menu_item_id       BIGINT NOT NULL REFERENCES order_menu_item(order_menu_item_id) ON DELETE CASCADE,
  name                     TEXT NOT NULL,
  qty                      INT NOT NULL DEFAULT 1 CHECK (qty > 0),
  unit_price_at_time       NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (unit_price_at_time >= 0)
);

-- Pizza order lines
CREATE TABLE order_pizza_item (
  order_pizza_item_id  BIGSERIAL PRIMARY KEY,
  order_id             BIGINT NOT NULL REFERENCES customer_order(order_id) ON DELETE CASCADE,
  pizza_id             INT NOT NULL REFERENCES pizza(pizza_id) ON UPDATE RESTRICT ON DELETE RESTRICT,
  pizza_size_id        INT NOT NULL REFERENCES pizza_size(pizza_size_id) ON UPDATE RESTRICT ON DELETE RESTRICT,
  qty                  INT NOT NULL DEFAULT 1 CHECK (qty > 0),
  base_price_at_time   NUMERIC(10,2) NOT NULL CHECK (base_price_at_time >= 0),
  notes                TEXT
);

CREATE TABLE order_pizza_item_extra (
  order_pizza_item_extra_id BIGSERIAL PRIMARY KEY,
  order_pizza_item_id       BIGINT NOT NULL REFERENCES order_pizza_item(order_pizza_item_id) ON DELETE CASCADE,
  ingredient_id             INT NOT NULL REFERENCES ingredient(ingredient_id) ON UPDATE RESTRICT ON DELETE RESTRICT,
  qty                       INT NOT NULL DEFAULT 1 CHECK (qty > 0),
  unit_price_at_time        NUMERIC(10,2) NOT NULL CHECK (unit_price_at_time >= 0)
);

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
  (9, 'Kiddies Meals', 90)
ON CONFLICT (id) DO NOTHING;

-- 8.3 Menu items (fixed IDs)
INSERT INTO menu_item (id, category_id, name, description, is_300ml, is_2l) VALUES
  -- Drinks
  (101, 4, 'Coke 300ml', 'Coke can', TRUE, FALSE),
  (102, 4, 'Coke 2L', 'Coke bottle', FALSE, TRUE),
  (103, 4, 'Coke Zero 300ml', 'Zero sugar', TRUE, FALSE),
  (104, 4, 'Still Water 500ml', 'Bottled water', TRUE, FALSE),

  -- Burgers
  (201, 1, 'Cheese Burger', 'Single patty burger', FALSE, FALSE),
  (202, 1, 'Mega Burger', 'Double patty burger', FALSE, FALSE),
  (203, 1, 'Steak Burger', 'Steak burger only', FALSE, FALSE),

  -- Burger Combos
  (301, 2, 'Cheese Burger Combo', 'Combo with 300ml drink and combo chips', FALSE, FALSE),
  (302, 2, 'Mega Burger Combo', 'Combo with 300ml drink and combo chips', FALSE, FALSE),

  -- Pastas
  (401, 3, 'Chicken Alfredo Medium', 'Medium portion with protein and sauce', FALSE, FALSE),
  (402, 3, 'Chicken Alfredo Large', 'Large portion with protein and sauce', FALSE, FALSE),

  -- Ribs
  (501, 8, 'Ribs 400g', 'Ribs with one side choice', FALSE, FALSE),
  (502, 8, 'Ribs 1kg', 'Ribs with one side choice', FALSE, FALSE),

  -- Sides
  (601, 5, 'Chips Small', 'Small chips', FALSE, FALSE),
  (602, 5, 'Chips Med', 'Medium chips', FALSE, FALSE),
  (603, 5, 'Chips Large', 'Large chips', FALSE, FALSE),
  (604, 5, 'Onion Rings', 'Single size onion rings', FALSE, FALSE),
  (605, 5, 'Salad', 'Single salad portion', FALSE, FALSE)
ON CONFLICT (id) DO NOTHING;

-- 8.4 Branch menu prices (example; expand this properly)
INSERT INTO branch_menu_item_price (branch_id, menu_item_id, price) VALUES
  (1, 101, 18.00),
  (2, 101, 16.00)
ON CONFLICT (branch_id, menu_item_id) DO UPDATE
SET price = EXCLUDED.price;

-- 8.5 Burger toppings (example)
INSERT INTO burger_toppings (id, burger_id, topping_name, is_default, price) VALUES
  (1, 201, 'Beef Patty', TRUE, 0),
  (2, 201, 'Lettuce', TRUE, 0),
  (3, 201, 'Tomato', TRUE, 0),
  (4, 201, 'Cheese', TRUE, 0)
ON CONFLICT (id) DO NOTHING;

-- =========================================================
-- 9) PIZZA SEED (FROM YOUR BIG SCRIPT)
-- =========================================================

-- Pizza sizes
INSERT INTO pizza_size (pizza_size_id, cm, sort_order) VALUES
  (1, 19, 1),
  (2, 23, 2),
  (3, 30, 3)
ON CONFLICT (pizza_size_id) DO NOTHING;

-- Categories
INSERT INTO pizza_category (pizza_category_id, name, sort_order) VALUES
  (1, 'Favourites', 1),
  (2, 'Supremes', 2)
ON CONFLICT (pizza_category_id) DO NOTHING;

-- Extra price categories
INSERT INTO price_category (price_category_id, name, sort_order) VALUES
  (1, 'Chilli / Garlic', 1),
  (2, 'Onion / Green Pepper / Capers / Banana / Fresh Tomato', 2),
  (3, 'Olives / Asparagus / Spinach / Mushrooms / Peppadew / Sundried Tomato / Pineapple', 3),
  (4, 'All Cheeses / Meats / Avo (seasonal)', 4)
ON CONFLICT (price_category_id) DO NOTHING;

-- Ingredients (toppings)
INSERT INTO ingredient (ingredient_id, name, price_category_id, seasonal) VALUES
  (1,  'chilli',          1, FALSE),
  (2,  'garlic',          1, FALSE),
  (3,  'onion',           2, FALSE),
  (4,  'green pepper',    2, FALSE),
  (5,  'capers',          2, FALSE),
  (6,  'banana',          2, FALSE),
  (7,  'fresh tomato',    2, FALSE),
  (8,  'olives',          3, FALSE),
  (9,  'asparagus',       3, FALSE),
  (10, 'spinach',         3, FALSE),
  (11, 'mushrooms',       3, FALSE),
  (12, 'peppadew',        3, FALSE),
  (13, 'sundried tomato', 3, FALSE),
  (14, 'pineapple',       3, FALSE),
  (15, 'mozzarella',      4, FALSE),
  (16, 'feta',            4, FALSE),
  (17, 'cheddar',         4, FALSE),
  (18, 'ham',             4, FALSE),
  (19, 'bacon',           4, FALSE),
  (20, 'salami',          4, FALSE),
  (21, 'chicken',         4, FALSE),
  (22, 'bbq chicken',     4, FALSE),
  (23, 'mince',           4, FALSE),
  (24, 'boerewors',       4, FALSE),
  (25, 'anchovies',       4, FALSE),
  (26, 'avo',             4, TRUE),
  (27, 'origanum',        1, FALSE),
  (28, 'tomato base',     2, FALSE),
  (29, 'rib',             4, FALSE),
  (30, 'rib sauce',       2, FALSE),
  (31, 'bbq sauce',       2, FALSE),
  (32, 'tikka sauce',     2, FALSE),
  (33, 'sweet chilli sauce', 2, FALSE),
  (34, 'sweet & sour sauce', 2, FALSE),
  (35, 'honey',           2, FALSE),
  (36, 'mustard',         2, FALSE),
  (37, 'mayonnaise',      2, FALSE),
  (38, 'chutney',         2, FALSE),
  (39, 'creamy chicken',  4, FALSE),
  (40, 'shrimps',         4, FALSE),
  (41, 'mussels',         4, FALSE),
  (42, 'calamari',        4, FALSE),
  (43, 'crab sticks',     4, FALSE)
ON CONFLICT (ingredient_id) DO NOTHING;

-- Pizzas (Favourites + Supremes)
INSERT INTO pizza (pizza_id, pizza_category_id, name, description, sort_order) VALUES
  (101, 1, 'Garlic Pita',  'fresh garlic & origanum', 1),
  (102, 1, 'Cheesy Pita',  'fresh garlic, origanum & feta or mozzarella', 2),
  (103, 1, 'Margherita',   'tomato base and mozzarella cheese', 3),
  (104, 1, 'Regina',       'ham & mushrooms', 4),
  (105, 1, 'Hawaiian',     'ham & pineapple', 5),
  (106, 1, 'Chicken Delite','chicken & peppadew', 6),
  (107, 1, '3 Cheeses',    'cheddar, feta & mozzarella', 7),
  (108, 1, 'New Yorker',   'bacon, mushrooms & onions', 8),
  (109, 1, 'Caribbean',    'bacon, banana & garlic', 9),
  (110, 1, 'Tropical',     'bacon & avo', 10),
  (111, 1, 'Mona Lisa',    'olives, green peppers, mushrooms & garlic', 11),
  (112, 1, 'Manhattan',    'mince, mushrooms & peppadew', 12),
  (113, 1, 'Chicken Fungi','chicken & mushrooms', 13),
  (114, 1, 'Cosmo',        'salami, feta & onion', 14),
  (115, 1, 'Exotica',      'cheese, tomato, pineapple & onion', 15),
  (116, 1, 'Salamina',     'salami, mushrooms & pineapple', 16),

  (201, 2, 'Greek',            'bacon, spinach, feta & olives', 1),
  (202, 2, 'A Lotta Meat',     'ham, bacon, salami & BBQ chicken', 2),
  (203, 2, 'Matt''s Rib Delight','rib, onion, pineapple, peppadew & rib sauce', 3),
  (204, 2, 'Oriental',         'bbq chicken, mushrooms, onion, green pepper & bbq sauce', 4),
  (205, 2, 'Carli''s Super',   'ham, salami, mushrooms, olives & avo', 5),
  (206, 2, 'Tikka Chicken',    'chicken, onion, peppadew & tikka sauce', 6),
  (207, 2, 'Mexicana',         'mince, onion, green pepper, chilli & garlic', 7),
  (208, 2, 'Sweet & Sour',     'chicken, green pepper, pineapple & sweet & sour sauce', 8),
  (209, 2, 'Honey & Mustard',  'chicken, mushrooms, feta, pineapple & honey and mustard sauce', 9),
  (210, 2, 'Four Seasons',     'salami, olives, mushrooms & asparagus', 10),
  (211, 2, 'Chicken Mayo',     'chicken, onion, mushrooms & mayonnaise', 11),
  (212, 2, 'Sweet Chilli Chic','chicken, peppadew, feta & sweet chilli sauce', 12),
  (213, 2, 'Tahita',           'mushrooms, olives, onion, feta & peppadew', 13),
  (214, 2, 'South African',    'boerewors, fresh tomato, mushrooms, onions, garlic & chutney', 14),
  (215, 2, 'Creamy Chicken',   'creamy chicken, mushrooms, asparagus & garlic', 15),
  (216, 2, 'Fruti Di Mare',    'shrimps, mussels, calamari, crab sticks & garlic', 16),
  (217, 2, 'Al Greeka',        'anchovies & olives', 17)
ON CONFLICT (pizza_id) DO NOTHING;

-- Allowed sizes (as you specified)
INSERT INTO pizza_allowed_size (pizza_id, pizza_size_id) VALUES
  (101,2),(101,3),
  (102,2),(102,3),
  (103,1),(103,2),(103,3),
  (104,1),(104,2),(104,3),
  (105,1),(105,2),(105,3),
  (106,2),(106,3),
  (107,1),(107,2),(107,3),
  (108,2),(108,3),
  (109,2),(109,3),
  (110,1),(110,2),(110,3),
  (111,2),(111,3),
  (112,2),(112,3),
  (113,1),(113,2),(113,3),
  (114,2),(114,3),
  (115,2),(115,3),
  (116,1),(116,2),(116,3),

  (201,2),(201,3),
  (202,2),(202,3),
  (203,2),(203,3),
  (204,2),(204,3),
  (205,2),(205,3),
  (206,2),(206,3),
  (207,2),(207,3),
  (208,2),(208,3),
  (209,2),(209,3),
  (210,2),(210,3),
  (211,2),(211,3),
  (212,2),(212,3),
  (213,2),(213,3),
  (214,2),(214,3),
  (215,2),(215,3),
  (216,3),
  (217,2),(217,3)
ON CONFLICT DO NOTHING;

-- Default recipes
INSERT INTO pizza_default_ingredient (pizza_id, ingredient_id, sort_order) VALUES
  (101, 2, 1), (101,27,2),
  (102, 2, 1), (102,27,2), (102,16,3), (102,15,4),
  (103,28,1), (103,15,2),
  (104,18,1), (104,11,2),
  (105,18,1), (105,14,2),
  (106,21,1), (106,12,2),
  (107,17,1), (107,16,2), (107,15,3),
  (108,19,1), (108,11,2), (108,3,3),
  (109,19,1), (109,6,2), (109,2,3),
  (110,19,1), (110,26,2),
  (111, 8,1), (111,4,2), (111,11,3), (111,2,4),
  (112,23,1), (112,11,2), (112,12,3),
  (113,21,1), (113,11,2),
  (114,20,1), (114,16,2), (114,3,3),
  (115,15,1), (115,7,2), (115,14,3), (115,3,4),
  (116,20,1), (116,11,2), (116,14,3),

  (201,19,1),(201,10,2),(201,16,3),(201,8,4),
  (202,18,1),(202,19,2),(202,20,3),(202,22,4),
  (203,29,1),(203,3,2),(203,14,3),(203,12,4),(203,30,5),
  (204,22,1),(204,11,2),(204,3,3),(204,4,4),(204,31,5),
  (205,18,1),(205,20,2),(205,11,3),(205,8,4),(205,26,5),
  (206,21,1),(206,3,2),(206,12,3),(206,32,4),
  (207,23,1),(207,3,2),(207,4,3),(207,1,4),(207,2,5),
  (208,21,1),(208,4,2),(208,14,3),(208,34,4),
  (209,21,1),(209,11,2),(209,16,3),(209,14,4),(209,35,5),(209,36,6),
  (210,20,1),(210,8,2),(210,11,3),(210,9,4),
  (211,21,1),(211,3,2),(211,11,3),(211,37,4),
  (212,21,1),(212,12,2),(212,16,3),(212,33,4),
  (213,11,1),(213,8,2),(213,3,3),(213,16,4),(213,12,5),
  (214,24,1),(214,7,2),(214,11,3),(214,3,4),(214,2,5),(214,38,6),
  (215,39,1),(215,11,2),(215,9,3),(215,2,4),
  (216,40,1),(216,41,2),(216,42,3),(216,43,4),(216,2,5),
  (217,25,1),(217,8,2)
ON CONFLICT DO NOTHING;

-- Branch pizza prices (Kenridge=1, Uitzicht=2) - currently identical to what you provided
-- NOTE: you can replace Uitzicht with different values later via DELETE+INSERT for branch_id=2
INSERT INTO branch_pizza_price (branch_id, pizza_id, pizza_size_id, price) VALUES
  -- Branch 1
  (1,101,2,59),(1,101,3,78),
  (1,102,2,92),(1,102,3,112),
  (1,103,1,58),(1,103,2,92),(1,103,3,112),
  (1,104,1,64),(1,104,2,109),(1,104,3,138),
  (1,105,1,64),(1,105,2,109),(1,105,3,138),
  (1,106,2,109),(1,106,3,138),
  (1,107,1,64),(1,107,2,109),(1,107,3,138),
  (1,108,2,109),(1,108,3,138),
  (1,109,2,109),(1,109,3,138),
  (1,110,1,64),(1,110,2,109),(1,110,3,138),
  (1,111,2,109),(1,111,3,138),
  (1,112,2,109),(1,112,3,138),
  (1,113,1,64),(1,113,2,109),(1,113,3,138),
  (1,114,2,109),(1,114,3,138),
  (1,115,2,109),(1,115,3,138),
  (1,116,1,64),(1,116,2,109),(1,116,3,138),

  (1,201,2,116),(1,201,3,147),
  (1,202,2,131),(1,202,3,159),
  (1,203,2,116),(1,203,3,147),
  (1,204,2,116),(1,204,3,147),
  (1,205,2,116),(1,205,3,147),
  (1,206,2,116),(1,206,3,147),
  (1,207,2,116),(1,207,3,147),
  (1,208,2,116),(1,208,3,147),
  (1,209,2,116),(1,209,3,147),
  (1,210,2,116),(1,210,3,147),
  (1,211,2,116),(1,211,3,147),
  (1,212,2,116),(1,212,3,147),
  (1,213,2,116),(1,213,3,147),
  (1,214,2,116),(1,214,3,147),
  (1,215,2,116),(1,215,3,147),
  (1,216,3,152),
  (1,217,2,120),(1,217,3,152),

  -- Branch 2
  (2,101,2,59),(2,101,3,78),
  (2,102,2,92),(2,102,3,112),
  (2,103,1,58),(2,103,2,92),(2,103,3,112),
  (2,104,1,64),(2,104,2,109),(2,104,3,138),
  (2,105,1,64),(2,105,2,109),(2,105,3,138),
  (2,106,2,109),(2,106,3,138),
  (2,107,1,64),(2,107,2,109),(2,107,3,138),
  (2,108,2,109),(2,108,3,138),
  (2,109,2,109),(2,109,3,138),
  (2,110,1,64),(2,110,2,109),(2,110,3,138),
  (2,111,2,109),(2,111,3,138),
  (2,112,2,109),(2,112,3,138),
  (2,113,1,64),(2,113,2,109),(2,113,3,138),
  (2,114,2,109),(2,114,3,138),
  (2,115,2,109),(2,115,3,138),
  (2,116,1,64),(2,116,2,109),(2,116,3,138),

  (2,201,2,116),(2,201,3,147),
  (2,202,2,131),(2,202,3,159),
  (2,203,2,116),(2,203,3,147),
  (2,204,2,116),(2,204,3,147),
  (2,205,2,116),(2,205,3,147),
  (2,206,2,116),(2,206,3,147),
  (2,207,2,116),(2,207,3,147),
  (2,208,2,116),(2,208,3,147),
  (2,209,2,116),(2,209,3,147),
  (2,210,2,116),(2,210,3,147),
  (2,211,2,116),(2,211,3,147),
  (2,212,2,116),(2,212,3,147),
  (2,213,2,116),(2,213,3,147),
  (2,214,2,116),(2,214,3,147),
  (2,215,2,116),(2,215,3,147),
  (2,216,3,152),
  (2,217,2,120),(2,217,3,152)
ON CONFLICT (branch_id, pizza_id, pizza_size_id) DO UPDATE
SET price = EXCLUDED.price;

-- Extras pricing grid
INSERT INTO branch_extra_price (branch_id, price_category_id, pizza_size_id, price) VALUES
  (1,1,1,7),(1,1,2,8),(1,1,3,9),
  (1,2,1,14),(1,2,2,17),(1,2,3,18),
  (1,3,1,16),(1,3,2,20),(1,3,3,22),
  (1,4,1,17),(1,4,2,22),(1,4,3,25),

  (2,1,1,7),(2,1,2,8),(2,1,3,9),
  (2,2,1,14),(2,2,2,17),(2,2,3,18),
  (2,3,1,16),(2,3,2,20),(2,3,3,22),
  (2,4,1,17),(2,4,2,22),(2,4,3,25)
ON CONFLICT (branch_id, price_category_id, pizza_size_id) DO UPDATE
SET price = EXCLUDED.price;

COMMIT;

-- =========================================================
-- 10) NORMALIZE MENU CATEGORY IDS (FAVOURITE / SUPREME)
-- =========================================================

BEGIN;

-- Normalize names to exact labels
UPDATE menu_category
SET name = 'Favourite',
    sort_order = 1
WHERE LOWER(name) IN ('favourite', 'favourites');

UPDATE menu_category
SET name = 'Supreme',
    sort_order = 2
WHERE LOWER(name) IN ('supreme', 'supremes');

-- Ensure required IDs exist
INSERT INTO menu_category (id, name, sort_order)
VALUES (1, 'Favourite', 1)
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name,
    sort_order = EXCLUDED.sort_order;

INSERT INTO menu_category (id, name, sort_order)
VALUES (2, 'Supreme', 2)
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name,
    sort_order = EXCLUDED.sort_order;

-- Remap menu items based on category name
UPDATE menu_item
SET category_id = 1
WHERE category_id IN (
    SELECT id FROM menu_category
    WHERE LOWER(name) IN ('favourite', 'favourites')
);

UPDATE menu_item
SET category_id = 2
WHERE category_id IN (
    SELECT id FROM menu_category
    WHERE LOWER(name) IN ('supreme', 'supremes')
);

-- Remove duplicates after remap
DELETE FROM menu_category
WHERE id <> 1
  AND LOWER(name) IN ('favourite', 'favourites');

DELETE FROM menu_category
WHERE id <> 2
  AND LOWER(name) IN ('supreme', 'supremes');

COMMIT;
