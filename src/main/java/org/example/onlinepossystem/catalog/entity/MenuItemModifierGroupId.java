package org.example.onlinepossystem.catalog.entity;

import java.io.Serializable;
import java.util.Objects;

public class MenuItemModifierGroupId implements Serializable {
    public Integer menuItemId;
    public Integer groupId;

    public MenuItemModifierGroupId() {
    }

    public MenuItemModifierGroupId(Integer menuItemId, Integer groupId) {
        this.menuItemId = menuItemId;
        this.groupId = groupId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MenuItemModifierGroupId that = (MenuItemModifierGroupId) o;
        return Objects.equals(menuItemId, that.menuItemId) && Objects.equals(groupId, that.groupId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(menuItemId, groupId);
    }
}
