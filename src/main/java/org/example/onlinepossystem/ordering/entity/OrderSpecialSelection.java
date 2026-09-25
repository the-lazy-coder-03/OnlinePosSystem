package org.example.onlinepossystem.ordering.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_special_selection")
public class OrderSpecialSelection {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_special_selection_id") private Long id;
    @ManyToOne @JoinColumn(name = "order_special_item_id", nullable = false) private OrderSpecialItem orderSpecialItem;
    @Column(name = "selection_kind", nullable = false) private String selectionKind;
    @Column(name = "special_component_id") private Long specialComponentId;
    @Column(name = "special_addon_id") private Long specialAddonId;
    @Column(name = "selection_index", nullable = false) private Integer selectionIndex;
    @Column(name = "label_at_time", nullable = false) private String labelAtTime;
    @Column(name = "product_name_at_time", nullable = false) private String productNameAtTime;
    @Column(name = "pizza_size_cm_at_time") private Integer pizzaSizeCmAtTime;
    @Column(name = "selection_quantity", nullable = false) private Integer selectionQuantity = 1;
    @Column(name = "addon_price_at_time", nullable = false, precision = 10, scale = 2) private BigDecimal addonPriceAtTime = BigDecimal.ZERO;
    @Column(name = "customization_charge_at_time", nullable = false, precision = 10, scale = 2) private BigDecimal customizationChargeAtTime = BigDecimal.ZERO;
    @OneToOne @JoinColumn(name = "order_menu_item_id") private OrderMenuItem menuItem;
    @OneToOne @JoinColumn(name = "order_pizza_item_id") private OrderPizzaItem pizzaItem;

    public Long getId() { return id; }
    public OrderSpecialItem getOrderSpecialItem() { return orderSpecialItem; }
    public void setOrderSpecialItem(OrderSpecialItem value) { orderSpecialItem = value; }
    public String getSelectionKind() { return selectionKind; }
    public void setSelectionKind(String value) { selectionKind = value; }
    public Long getSpecialComponentId() { return specialComponentId; }
    public void setSpecialComponentId(Long value) { specialComponentId = value; }
    public Long getSpecialAddonId() { return specialAddonId; }
    public void setSpecialAddonId(Long value) { specialAddonId = value; }
    public Integer getSelectionIndex() { return selectionIndex; }
    public void setSelectionIndex(Integer value) { selectionIndex = value; }
    public String getLabelAtTime() { return labelAtTime; }
    public void setLabelAtTime(String value) { labelAtTime = value; }
    public String getProductNameAtTime() { return productNameAtTime; }
    public void setProductNameAtTime(String value) { productNameAtTime = value; }
    public Integer getPizzaSizeCmAtTime() { return pizzaSizeCmAtTime; }
    public void setPizzaSizeCmAtTime(Integer value) { pizzaSizeCmAtTime = value; }
    public Integer getSelectionQuantity() { return selectionQuantity; }
    public void setSelectionQuantity(Integer value) { selectionQuantity = value; }
    public BigDecimal getAddonPriceAtTime() { return addonPriceAtTime; }
    public void setAddonPriceAtTime(BigDecimal value) { addonPriceAtTime = value; }
    public BigDecimal getCustomizationChargeAtTime() { return customizationChargeAtTime; }
    public void setCustomizationChargeAtTime(BigDecimal value) { customizationChargeAtTime = value; }
    public OrderMenuItem getMenuItem() { return menuItem; }
    public void setMenuItem(OrderMenuItem value) { menuItem = value; }
    public OrderPizzaItem getPizzaItem() { return pizzaItem; }
    public void setPizzaItem(OrderPizzaItem value) { pizzaItem = value; }
}
