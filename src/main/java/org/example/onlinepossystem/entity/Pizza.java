package org.example.onlinepossystem.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "pizza")
public class Pizza {
    @Id
    @Column(name = "pizza_id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "pizza_category_id", nullable = false)
    private PizzaCategory category;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private boolean active = true;

    public Pizza() {}

    public Pizza(Integer id, PizzaCategory category, String name, String description, Integer sortOrder) {
        this.id = id;
        this.category = category;
        this.name = name;
        this.description = description;
        this.sortOrder = sortOrder;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public PizzaCategory getCategory() { return category; }
    public void setCategory(PizzaCategory category) { this.category = category; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
