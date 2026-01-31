package org.example.onlinepossystem.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "order_item_extra")
public class OrderItemTopping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_extra_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @ManyToOne
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @Column(nullable = false)
    private Integer qty = 1;

    @Column(name = "unit_price_at_time", nullable = false)
    private Double price;

    public OrderItemTopping() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public OrderItem getOrderItem() { return orderItem; }
    public void setOrderItem(OrderItem orderItem) { this.orderItem = orderItem; }
    public Ingredient getIngredient() { return ingredient; }
    public void setIngredient(Ingredient ingredient) { this.ingredient = ingredient; }
    public Integer getQty() { return qty; }
    public void setQty(Integer qty) { this.qty = qty; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
}
