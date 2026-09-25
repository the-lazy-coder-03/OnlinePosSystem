package org.example.onlinepossystem.special.entity;

import jakarta.persistence.*;
import org.example.onlinepossystem.catalog.entity.MenuItem;
import org.example.onlinepossystem.catalog.entity.Pizza;
import org.example.onlinepossystem.catalog.entity.PizzaSize;

import java.math.BigDecimal;

@Entity
@Table(name = "special_addon")
public class SpecialAddon {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "special_addon_id") private Long id;
    @ManyToOne @JoinColumn(name = "special_id", nullable = false) private Special special;
    @Column(name = "addon_code", nullable = false) private String code;
    @Column(nullable = false) private String label;
    @Column(name = "product_type", nullable = false) private String productType;
    @ManyToOne @JoinColumn(name = "menu_item_id") private MenuItem menuItem;
    @ManyToOne @JoinColumn(name = "pizza_id") private Pizza pizza;
    @ManyToOne @JoinColumn(name = "pizza_size_id") private PizzaSize pizzaSize;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal price;
    @Column(name = "max_quantity", nullable = false) private int maxQuantity = 1;
    @Column(name = "allow_customization", nullable = false) private boolean allowCustomization = true;
    @Column(nullable = false) private boolean active = true;
    @Column(name = "sort_order", nullable = false) private int sortOrder;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Special getSpecial() { return special; }
    public void setSpecial(Special special) { this.special = special; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }
    public MenuItem getMenuItem() { return menuItem; }
    public void setMenuItem(MenuItem menuItem) { this.menuItem = menuItem; }
    public Pizza getPizza() { return pizza; }
    public void setPizza(Pizza pizza) { this.pizza = pizza; }
    public PizzaSize getPizzaSize() { return pizzaSize; }
    public void setPizzaSize(PizzaSize pizzaSize) { this.pizzaSize = pizzaSize; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public int getMaxQuantity() { return maxQuantity; }
    public void setMaxQuantity(int maxQuantity) { this.maxQuantity = maxQuantity; }
    public boolean isAllowCustomization() { return allowCustomization; }
    public void setAllowCustomization(boolean allowCustomization) { this.allowCustomization = allowCustomization; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
