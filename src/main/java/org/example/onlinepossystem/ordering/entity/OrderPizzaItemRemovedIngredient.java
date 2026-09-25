package org.example.onlinepossystem.ordering.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "order_pizza_item_removed_ingredient")
public class OrderPizzaItemRemovedIngredient {
    @EmbeddedId private Id id = new Id();
    @ManyToOne @MapsId("orderPizzaItemId") @JoinColumn(name = "order_pizza_item_id")
    private OrderPizzaItem orderPizzaItem;
    @Column(name = "ingredient_name_at_time", nullable = false)
    private String ingredientNameAtTime;

    public Id getId() { return id; }
    public OrderPizzaItem getOrderPizzaItem() { return orderPizzaItem; }
    public void setOrderPizzaItem(OrderPizzaItem item) { this.orderPizzaItem = item; }
    public Integer getIngredientId() { return id.ingredientId; }
    public void setIngredientId(Integer ingredientId) { id.ingredientId = ingredientId; }
    public String getIngredientNameAtTime() { return ingredientNameAtTime; }
    public void setIngredientNameAtTime(String value) { ingredientNameAtTime = value; }

    @Embeddable
    public static class Id implements Serializable {
        @Column(name = "order_pizza_item_id") private Long orderPizzaItemId;
        @Column(name = "ingredient_id") private Integer ingredientId;
        public Id() {}
        @Override public boolean equals(Object o) { return o instanceof Id other && Objects.equals(orderPizzaItemId, other.orderPizzaItemId) && Objects.equals(ingredientId, other.ingredientId); }
        @Override public int hashCode() { return Objects.hash(orderPizzaItemId, ingredientId); }
    }
}
