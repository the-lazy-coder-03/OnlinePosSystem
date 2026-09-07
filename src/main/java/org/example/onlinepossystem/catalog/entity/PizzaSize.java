package org.example.onlinepossystem.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "pizza_size")
public class PizzaSize {
    @Id
    @Column(name = "pizza_size_id")
    private Integer id;

    @Column(nullable = false, unique = true)
    private Integer cm;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private boolean active = true;

    public PizzaSize() {}

    public PizzaSize(Integer id, Integer cm, Integer sortOrder) {
        this.id = id;
        this.cm = cm;
        this.sortOrder = sortOrder;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getCm() { return cm; }
    public void setCm(Integer cm) { this.cm = cm; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
