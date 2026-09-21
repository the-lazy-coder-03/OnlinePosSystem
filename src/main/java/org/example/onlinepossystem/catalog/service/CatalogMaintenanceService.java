package org.example.onlinepossystem.catalog.service;

import org.example.onlinepossystem.catalog.api.CatalogMaintenance;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CatalogMaintenanceService implements CatalogMaintenance {
    private final JdbcTemplate jdbcTemplate;

    public CatalogMaintenanceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void syncMenuModifierCatalog() {
        upsertModifierGroup(4, "Chip extras", false, 0, 2);
        upsertModifierOption(13, 4, "Extra Chip Sauce", null, BigDecimal.ZERO);
        upsertModifierOption(14, 4, "Extra Rib Sauce", null, BigDecimal.ZERO);
        unlinkBurgerComboSideChoices();
        unlinkModifierGroup(601, 4);
        unlinkModifierGroup(602, 4);
        unlinkModifierGroup(603, 4);
        linkModifierGroupIfMenuItemExists(501, 4);
        linkModifierGroupIfMenuItemExists(502, 4);
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

    private void unlinkBurgerComboSideChoices() {
        jdbcTemplate.update("""
                DELETE FROM menu_item_modifier_group
                WHERE group_id = 2
                  AND menu_item_id IN (301, 302, 303, 304, 305, 306, 307, 308)
                """);
    }
}
