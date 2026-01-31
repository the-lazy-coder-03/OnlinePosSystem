package org.example.onlinepossystem.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "pizza", uniqueConstraints = {@UniqueConstraint(name = "uq_pizza_name", columnNames = {"pizza_category_id", "name"})})
public class MenuItem {
    @Id
    @Column(name = "pizza_id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "pizza_category_id", nullable = false)
    private MenuCategory category;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private boolean active = true;

    public MenuItem() {}

    public MenuItem(Integer id, MenuCategory category, String name, String description, Integer sortOrder) {
        this.id = id;
        this.category = category;
        this.name = name;
        this.description = description;
        this.sortOrder = sortOrder;
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
}
