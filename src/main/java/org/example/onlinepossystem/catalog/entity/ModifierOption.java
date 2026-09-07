package org.example.onlinepossystem.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

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

    @Column(name = "additional_price")
    public BigDecimal additionalPrice = BigDecimal.ZERO;

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
    public BigDecimal getAdditionalPrice() { return additionalPrice; }
    public void setAdditionalPrice(BigDecimal additionalPrice) {
        this.additionalPrice = additionalPrice == null ? BigDecimal.ZERO : additionalPrice;
    }
}
