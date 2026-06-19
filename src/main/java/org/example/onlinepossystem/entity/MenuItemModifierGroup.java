package org.example.onlinepossystem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "menu_item_modifier_group")
@IdClass(MenuItemModifierGroupId.class)
public class MenuItemModifierGroup {
    @Id
    @Column(name = "menu_item_id")
    public Integer menuItemId;

    @Id
    @Column(name = "group_id")
    public Integer groupId;

    public MenuItemModifierGroup() {
    }

    public MenuItemModifierGroup(Integer menuItemId, Integer groupId) {
        this.menuItemId = menuItemId;
        this.groupId = groupId;
    }

    public Integer getMenuItemId() { return menuItemId; }
    public void setMenuItemId(Integer menuItemId) { this.menuItemId = menuItemId; }
    public Integer getGroupId() { return groupId; }
    public void setGroupId(Integer groupId) { this.groupId = groupId; }
}
