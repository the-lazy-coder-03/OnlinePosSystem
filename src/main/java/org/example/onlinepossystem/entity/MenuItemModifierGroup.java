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

    protected MenuItemModifierGroup() {
    }
}
