package org.example.onlinepossystem.ordering.entity;

import jakarta.persistence.*;
import org.example.onlinepossystem.catalog.entity.Ingredient;

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

    @ManyToOne
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @Column(nullable = false)
    private Integer qty = 1;

    @Column(name = "unit_price_at_time", nullable = false)
    private Double unitPriceAtTime;

    public OrderPizzaItemExtra() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public OrderPizzaItem getOrderPizzaItem() { return orderPizzaItem; }
    public void setOrderPizzaItem(OrderPizzaItem orderPizzaItem) { this.orderPizzaItem = orderPizzaItem; }
    public Ingredient getIngredient() { return ingredient; }
    public void setIngredient(Ingredient ingredient) { this.ingredient = ingredient; }
    public Integer getQty() { return qty; }
    public void setQty(Integer qty) { this.qty = qty; }
    public Double getUnitPriceAtTime() { return unitPriceAtTime; }
    public void setUnitPriceAtTime(Double unitPriceAtTime) { this.unitPriceAtTime = unitPriceAtTime; }
}
