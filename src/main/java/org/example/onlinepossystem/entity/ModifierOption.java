package org.example.onlinepossystem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "modifier_option")
public class ModifierOption {
    @Id
    @Column(name = "id")
    public Integer id;

    @Column(name = "group_id")
    public Integer groupId;

    @Column(name = "name")
    public String name;

    @Column(name = "menu_item_id")
    public Integer menuItemId;

    public ModifierOption() {
    }

    public ModifierOption(Integer id, Integer groupId, String name, Integer menuItemId) {
        this.id = id;
        this.groupId = groupId;
        this.name = name;
        this.menuItemId = menuItemId;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getGroupId() { return groupId; }
    public void setGroupId(Integer groupId) { this.groupId = groupId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getMenuItemId() { return menuItemId; }
    public void setMenuItemId(Integer menuItemId) { this.menuItemId = menuItemId; }
}
