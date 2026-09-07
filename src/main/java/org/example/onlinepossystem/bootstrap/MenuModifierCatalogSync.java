package org.example.onlinepossystem.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class MenuModifierCatalogSync implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(MenuModifierCatalogSync.class);

    private final JdbcTemplate jdbcTemplate;

    public MenuModifierCatalogSync(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        try {
            upsertModifierGroup(4, "Chip extras", false, 0, 2);
            upsertModifierOption(13, 4, "Extra Chip Sauce", null, BigDecimal.ZERO);
            upsertModifierOption(14, 4, "Extra Rib Sauce", null, BigDecimal.ZERO);
            unlinkModifierGroup(601, 4);
            unlinkModifierGroup(602, 4);
            unlinkModifierGroup(603, 4);
            linkModifierGroupIfMenuItemExists(501, 4);
            linkModifierGroupIfMenuItemExists(502, 4);
        } catch (DataAccessException ex) {
            logger.warn("Could not sync chip modifier catalog rows. migration.sql may not have run yet.", ex);
        }
    }

    private void upsertModifierGroup(int id, String name, boolean required, int minSelect, int maxSelect) {
        int updated = jdbcTemplate.update("""
                UPDATE modifier_group
                SET name = ?, required = ?, min_select = ?, max_select = ?
                WHERE id = ?
                """, name, required, minSelect, maxSelect, id);
        if (updated == 0) {
            jdbcTemplate.update("""
                    INSERT INTO modifier_group (id, name, required, min_select, max_select)
                    VALUES (?, ?, ?, ?, ?)
                    """, id, name, required, minSelect, maxSelect);
        }
    }

    private void upsertModifierOption(int id, int groupId, String name, Integer menuItemId, BigDecimal additionalPrice) {
        int updated = jdbcTemplate.update("""
                UPDATE modifier_option
                SET group_id = ?, name = ?, menu_item_id = ?, additional_price = ?
                WHERE id = ?
                """, groupId, name, menuItemId, additionalPrice, id);
        if (updated == 0) {
            jdbcTemplate.update("""
                    INSERT INTO modifier_option (id, group_id, name, menu_item_id, additional_price)
                    VALUES (?, ?, ?, ?, ?)
                    """, id, groupId, name, menuItemId, additionalPrice);
        }
    }

    private void linkModifierGroupIfMenuItemExists(int menuItemId, int groupId) {
        Integer menuItemCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM menu_item WHERE id = ?",
                Integer.class,
                menuItemId
        );
        if (menuItemCount == null || menuItemCount == 0) {
            return;
        }

        Integer linkCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM menu_item_modifier_group WHERE menu_item_id = ? AND group_id = ?",
                Integer.class,
                menuItemId,
                groupId
        );
        if (linkCount != null && linkCount > 0) {
            return;
        }

        jdbcTemplate.update(
                "INSERT INTO menu_item_modifier_group (menu_item_id, group_id) VALUES (?, ?)",
                menuItemId,
                groupId
        );
    }

    private void unlinkModifierGroup(int menuItemId, int groupId) {
        jdbcTemplate.update(
                "DELETE FROM menu_item_modifier_group WHERE menu_item_id = ? AND group_id = ?",
                menuItemId,
                groupId
        );
    }
}
