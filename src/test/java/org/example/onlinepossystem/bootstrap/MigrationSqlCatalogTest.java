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
