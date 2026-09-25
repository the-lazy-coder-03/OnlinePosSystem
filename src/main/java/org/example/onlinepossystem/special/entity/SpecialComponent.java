package org.example.onlinepossystem.special.entity;

import jakarta.persistence.*;
import org.example.onlinepossystem.catalog.entity.*;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "special_component")
public class SpecialComponent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "special_component_id") private Long id;
    @ManyToOne @JoinColumn(name = "special_id", nullable = false) private Special special;
    @Column(name = "component_code", nullable = false) private String code;
    @Column(nullable = false) private String label;
    @Column(name = "product_type", nullable = false) private String productType;
    @Column(nullable = false) private int quantity;
    @Column(name = "selection_mode", nullable = false) private String selectionMode;
    @ManyToOne @JoinColumn(name = "menu_category_id") private MenuCategory menuCategory;
    @ManyToOne @JoinColumn(name = "pizza_category_id") private PizzaCategory pizzaCategory;
    @ManyToOne @JoinColumn(name = "pizza_size_id") private PizzaSize pizzaSize;
    @Column(name = "allow_repeats", nullable = false) private boolean allowRepeats;
    @Column(name = "allow_customization", nullable = false) private boolean allowCustomization = true;
    @Column(name = "sort_order", nullable = false) private int sortOrder;

    @ManyToMany
    @JoinTable(name = "special_component_menu_item", joinColumns = @JoinColumn(name = "special_component_id"),
            inverseJoinColumns = @JoinColumn(name = "menu_item_id"))
    private Set<MenuItem> menuItems = new LinkedHashSet<>();
    @ManyToMany
    @JoinTable(name = "special_component_pizza", joinColumns = @JoinColumn(name = "special_component_id"),
            inverseJoinColumns = @JoinColumn(name = "pizza_id"))
    private Set<Pizza> pizzas = new LinkedHashSet<>();

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
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getSelectionMode() { return selectionMode; }
    public void setSelectionMode(String selectionMode) { this.selectionMode = selectionMode; }
    public MenuCategory getMenuCategory() { return menuCategory; }
    public void setMenuCategory(MenuCategory menuCategory) { this.menuCategory = menuCategory; }
    public PizzaCategory getPizzaCategory() { return pizzaCategory; }
    public void setPizzaCategory(PizzaCategory pizzaCategory) { this.pizzaCategory = pizzaCategory; }
    public PizzaSize getPizzaSize() { return pizzaSize; }
    public void setPizzaSize(PizzaSize pizzaSize) { this.pizzaSize = pizzaSize; }
    public boolean isAllowRepeats() { return allowRepeats; }
    public void setAllowRepeats(boolean allowRepeats) { this.allowRepeats = allowRepeats; }
    public boolean isAllowCustomization() { return allowCustomization; }
    public void setAllowCustomization(boolean allowCustomization) { this.allowCustomization = allowCustomization; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public Set<MenuItem> getMenuItems() { return menuItems; }
    public void setMenuItems(Set<MenuItem> menuItems) { this.menuItems = menuItems; }
    public Set<Pizza> getPizzas() { return pizzas; }
    public void setPizzas(Set<Pizza> pizzas) { this.pizzas = pizzas; }
}
