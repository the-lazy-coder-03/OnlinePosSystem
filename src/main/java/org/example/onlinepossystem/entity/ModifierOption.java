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

    protected ModifierOption() {
    }
}
