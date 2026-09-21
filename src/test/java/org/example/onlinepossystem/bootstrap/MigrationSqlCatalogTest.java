package org.example.onlinepossystem.bootstrap;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class MigrationSqlCatalogTest {

    private static final Pattern BRANCH_MENU_PRICE = Pattern.compile(
            "\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+\\.\\d{2})\\s*\\)"
    );

    @Test
    void seedsRequestedUitzichtBurgerPricesWithoutInventingBaconAndEggComboPrice() throws IOException {
        String priceSeed = section(migrationSql(), "-- 8.4 Branch menu prices", "-- 8.5 Normalised burger components");
        Map<Integer, BigDecimal> actual = branchPrices(priceSeed, 2, 200, 399);

        assertThat(actual).containsExactlyInAnyOrderEntriesOf(Map.ofEntries(
                Map.entry(201, new BigDecimal("80.00")),
                Map.entry(202, new BigDecimal("114.00")),
                Map.entry(203, new BigDecimal("116.00")),
                Map.entry(204, new BigDecimal("68.00")),
                Map.entry(205, new BigDecimal("90.00")),
                Map.entry(206, new BigDecimal("90.00")),
                Map.entry(207, new BigDecimal("103.00")),
                Map.entry(208, new BigDecimal("90.00")),
                Map.entry(301, new BigDecimal("113.00")),
                Map.entry(302, new BigDecimal("136.00")),
                Map.entry(303, new BigDecimal("138.00")),
                Map.entry(304, new BigDecimal("105.00")),
                Map.entry(305, new BigDecimal("120.00")),
                Map.entry(307, new BigDecimal("123.00")),
                Map.entry(308, new BigDecimal("120.00"))
        ));
        assertThat(actual).doesNotContainKey(306);
    }

    @Test
    void configuresHawaiianBurgersAndAllComboModifierGroups() throws IOException {
        String sql = migrationSql();

        assertThat(sql)
                .contains("(208, 1, 'Hawaiian Burger'")
                .contains("(308, 2, 'Hawaiian Burger Combo'")
                .contains("(208, 1, 1, TRUE),  -- Hawaiian Burger")
                .contains("(308, 1, 1, TRUE)   -- Hawaiian Burger Combo")
                .contains("(208, 201, TRUE, 101), -- Hawaiian Burger: Cheese")
                .contains("(208, 203, TRUE, 102), -- Hawaiian Burger: Pineapple")
                .contains("(308, 201, TRUE, 101), -- Hawaiian Burger Combo: Cheese")
                .contains("(308, 203, TRUE, 102)  -- Hawaiian Burger Combo: Pineapple");

        for (int comboId = 301; comboId <= 308; comboId++) {
            assertThat(sql)
                    .contains("(" + comboId + ", 1)")
                    .contains("(" + comboId + ", 2)");
        }
    }

    @Test
    void migrationDoesNotRewriteHistoricalOrders() throws IOException {
        String sql = migrationSql().toLowerCase();

        assertThat(sql)
                .doesNotContain("update customer_order")
                .doesNotContain("delete from customer_order")
                .doesNotContain("update order_menu_item")
                .doesNotContain("delete from order_menu_item");
    }

    @Test
    void seedsExpandedMenuAndOrderablePizzaBaseOption() throws IOException {
        String sql = migrationSql();

        assertThat(sql)
                .contains("(10, 'Toasted Sandwiches', 100)")
                .contains("(11, 'Desserts', 110)")
                .contains("(114, 4, 'Grapetiser White 330ml'")
                .contains("(122, 4, 'Sprite 2L'")
                .contains("(410, 3, 'Lasagne Large'")
                .contains("(422, 3, 'Vegetarian Pasta Large'")
                .contains("(701, 7, 'Greek Salad'")
                .contains("(905, 9, '10 x Mini Cheese Grillers'")
                .contains("(1001, 10, 'Cheese & Tomato Toasted Sandwich'")
                .contains("(1007, 10, 'Mince & Cheese Toasted Sandwich'")
                .contains("(1101, 11, 'Magnum'")
                .contains("(1103, 11, 'Paddle Pop'")
                .contains("(6, 'Choose included kiddies sauce', TRUE, 1, 1)")
                .contains("(8, 'Choose pasta type', TRUE, 1, 1)")
                .contains("(419, 9)")
                .contains("(420, 10)")
                .contains("CREATE TABLE IF NOT EXISTS pizza_base_option")
                .contains("CREATE TABLE IF NOT EXISTS branch_pizza_base_option_price")
                .contains("CREATE TABLE IF NOT EXISTS order_pizza_item_base_option")
                .contains("(1, 'Wheat and Gluten Free Base', TRUE)")
                .contains("(1, 1, 3, 36.00)");
    }

    @Test
    void keepsRibSidesAndOnlyTheRequestedSandwichAndKiddiesExtras() throws IOException {
        String sql = migrationSql();

        assertThat(sql)
                .contains("(7, 'Kiddies burger extras', FALSE, 0, 4)")
                .contains("(11, 'Toasted sandwich extras', FALSE, 0, 4)")
                .contains("(501, 2), (501, 3)")
                .contains("(502, 2), (502, 3)")
                .contains("(9, 2, 'Chips', 602, 0.00)")
                .contains("(10, 2, '5 x Onion Rings', 604, 0.00)")
                .contains("(11, 2, 'Salad', 605, 0.00)")
                .contains("(33, 7, 'Add Cheese', NULL, 14.00)")
                .contains("(47, 7, 'Add Bacon', NULL, 19.00)")
                .contains("(48, 7, 'Add Egg', NULL, 14.00)")
                .contains("(49, 7, 'Add Avo', NULL, 19.00)")
                .contains("(54, 11, 'Add Cheese', NULL, 14.00)")
                .contains("(57, 11, 'Add Avo', NULL, 19.00)")
                .contains("WHERE group_id = 7\n  AND id IN (50, 51, 52, 53)")
                .doesNotContain("(50, 7, 'Add 5 x Onion Rings'")
                .doesNotContain("(51, 7, 'Add Pepper Sauce'")
                .doesNotContain("(52, 7, 'Add Mushroom Sauce'")
                .doesNotContain("(53, 7, 'Add Cheese Sauce'");

        for (int sandwichId = 1001; sandwichId <= 1007; sandwichId++) {
            assertThat(sql).contains("(" + sandwichId + ", 11)");
        }
    }

    @Test
    void deactivatesOnionRingsAndPremiumSaucesOnlyForBurgerComponents() throws IOException {
        String sql = migrationSql();

        assertThat(sql)
                .contains("(3,   'BBQ Sauce',           'extra_topping',   TRUE, FALSE)")
                .contains("(206, '5 x Onion Rings',     'extra_topping', FALSE, FALSE)")
                .contains("(207, 'Pepper Sauce',        'extra_topping', FALSE, FALSE)")
                .contains("(208, 'Mushroom Sauce',      'extra_topping', FALSE, FALSE)")
                .contains("(209, 'Cheese Sauce',        'extra_topping', FALSE, FALSE)")
                .contains("(604, 5, '5 x Onion Rings'")
                .contains("(623, 5, 'Pepper Sauce'")
                .contains("(624, 5, 'Mushroom Sauce'")
                .contains("(625, 5, 'Cheese Sauce'")
                .contains("(25, 3, '5 x Onion Rings', 604, 28.00)");
    }

    @Test
    void movesSauceMenuItemsIntoSidesAndDeactivatesSaucesCategory() throws IOException {
        String sql = migrationSql();

        assertThat(sql)
                .contains("UPDATE menu_category\nSET active = FALSE\nWHERE id = 6;")
                .contains("(620, 5, 'BBQ Sauce 100ml'")
                .contains("(621, 5, 'Sweet Chilli Sauce 100ml'")
                .contains("(622, 5, 'Pink Sauce 100ml'")
                .contains("(623, 5, 'Pepper Sauce'")
                .contains("(624, 5, 'Mushroom Sauce'")
                .contains("(625, 5, 'Cheese Sauce'")
                .contains("(1, 620, 15.00)")
                .contains("(1, 621, 15.00)")
                .contains("(1, 622, 15.00)")
                .contains("(1, 623, 35.00)")
                .contains("(1, 624, 35.00)")
                .contains("(1, 625, 35.00)")
                .contains("(12, 3, 'BBQ Sauce 100ml', 620, 15.00)")
                .contains("(13, 3, 'Sweet Chilli Sauce 100ml', 621, 15.00)")
                .contains("(14, 3, 'Pink Sauce 100ml', 622, 15.00)");
    }

    private String migrationSql() throws IOException {
        ClassPathResource resource = new ClassPathResource("migration.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    private String section(String sql, String startMarker, String endMarker) {
        int start = sql.indexOf(startMarker);
        int end = sql.indexOf(endMarker, start);
        assertThat(start).isGreaterThanOrEqualTo(0);
        assertThat(end).isGreaterThan(start);
        return sql.substring(start, end);
    }

    private Map<Integer, BigDecimal> branchPrices(String sql, int branchId, int firstItemId, int lastItemId) {
        Map<Integer, BigDecimal> prices = new LinkedHashMap<>();
        Matcher matcher = BRANCH_MENU_PRICE.matcher(sql);
        while (matcher.find()) {
            int rowBranchId = Integer.parseInt(matcher.group(1));
            int menuItemId = Integer.parseInt(matcher.group(2));
            if (rowBranchId == branchId && menuItemId >= firstItemId && menuItemId <= lastItemId) {
                prices.put(menuItemId, new BigDecimal(matcher.group(3)));
            }
        }
        return prices;
    }
}
