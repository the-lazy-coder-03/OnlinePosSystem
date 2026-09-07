package org.example.onlinepossystem.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "menu_item", uniqueConstraints = {@UniqueConstraint(name = "uq_menu_item_name", columnNames = {"category_id", "name"})})
public class MenuItem {
    @Id
    @Column(name = "id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private MenuCategory category;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "is_300ml", nullable = false)
    private boolean is300ml = false;

    @Column(name = "is_2l", nullable = false)
    private boolean is2l = false;

    public MenuItem() {}

    public MenuItem(Integer id, MenuCategory category, String name, String description, Integer sortOrder, boolean is300ml, boolean is2l) {
        this.id = id;
        this.category = category;
        this.name = name;
        this.description = description;
        this.sortOrder = sortOrder;
        this.is300ml = is300ml;
        this.is2l = is2l;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public MenuCategory getCategory() { return category; }
    public void setCategory(MenuCategory category) { this.category = category; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isIs300ml() { return is300ml; }
    public void setIs300ml(boolean is300ml) { this.is300ml = is300ml; }
    public boolean isIs2l() { return is2l; }
    public void setIs2l(boolean is2l) { this.is2l = is2l; }
}
