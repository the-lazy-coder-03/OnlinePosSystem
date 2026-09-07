package org.example.onlinepossystem.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "ingredient")
public class Ingredient {
    @Id
    @Column(name = "ingredient_id")
    private Integer id;

    @Column(nullable = false, unique = true)
    private String name;

    @ManyToOne
    @JoinColumn(name = "price_category_id", nullable = false)
    private PriceCategory priceCategory;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean seasonal = false;

    public Ingredient() {}

    public Ingredient(Integer id, String name, PriceCategory priceCategory, boolean seasonal) {
        this.id = id;
        this.name = name;
        this.priceCategory = priceCategory;
        this.seasonal = seasonal;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public PriceCategory getPriceCategory() { return priceCategory; }
    public void setPriceCategory(PriceCategory priceCategory) { this.priceCategory = priceCategory; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isSeasonal() { return seasonal; }
    public void setSeasonal(boolean seasonal) { this.seasonal = seasonal; }
}
