package org.example.onlinepossystem.ordering.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_pizza_item_base_option")
public class OrderPizzaItemBaseOption {
    @Id
    @Column(name = "order_pizza_item_id")
    private Long orderPizzaItemId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "order_pizza_item_id")
    private OrderPizzaItem orderPizzaItem;

    @Column(name = "pizza_base_option_id", nullable = false)
    private Integer pizzaBaseOptionId;

    @Column(name = "base_option_name_at_time")
    private String baseOptionNameAtTime;

    @Column(name = "unit_price_at_time", nullable = false)
    private Double unitPriceAtTime;

    public OrderPizzaItemBaseOption() {}

    public Long getOrderPizzaItemId() { return orderPizzaItemId; }
    public OrderPizzaItem getOrderPizzaItem() { return orderPizzaItem; }
    public void setOrderPizzaItem(OrderPizzaItem orderPizzaItem) { this.orderPizzaItem = orderPizzaItem; }
    public Integer getPizzaBaseOptionId() { return pizzaBaseOptionId; }
    public void setPizzaBaseOptionId(Integer pizzaBaseOptionId) { this.pizzaBaseOptionId = pizzaBaseOptionId; }
    public String getBaseOptionNameAtTime() { return baseOptionNameAtTime; }
    public void setBaseOptionNameAtTime(String baseOptionNameAtTime) { this.baseOptionNameAtTime = baseOptionNameAtTime; }
    public Double getUnitPriceAtTime() { return unitPriceAtTime; }
    public void setUnitPriceAtTime(Double unitPriceAtTime) { this.unitPriceAtTime = unitPriceAtTime; }
}
