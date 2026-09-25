package org.example.onlinepossystem.ordering.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "order_pizza_item_extra")
public class OrderPizzaItemExtra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_pizza_item_extra_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_pizza_item_id", nullable = false)
    private OrderPizzaItem orderPizzaItem;

    @Column(name = "ingredient_id", nullable = false)
    private Integer ingredientId;

    @Column(name = "ingredient_name_at_time")
    private String ingredientNameAtTime;

    @Column(nullable = false)
    private Integer qty = 1;

    @Column(name = "unit_price_at_time", nullable = false)
    private Double unitPriceAtTime;

    public OrderPizzaItemExtra() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public OrderPizzaItem getOrderPizzaItem() { return orderPizzaItem; }
    public void setOrderPizzaItem(OrderPizzaItem orderPizzaItem) { this.orderPizzaItem = orderPizzaItem; }
    public Integer getIngredientId() { return ingredientId; }
    public void setIngredientId(Integer ingredientId) { this.ingredientId = ingredientId; }
    public String getIngredientNameAtTime() { return ingredientNameAtTime; }
    public void setIngredientNameAtTime(String ingredientNameAtTime) { this.ingredientNameAtTime = ingredientNameAtTime; }
    public Integer getQty() { return qty; }
    public void setQty(Integer qty) { this.qty = qty; }
    public Double getUnitPriceAtTime() { return unitPriceAtTime; }
    public void setUnitPriceAtTime(Double unitPriceAtTime) { this.unitPriceAtTime = unitPriceAtTime; }
}
