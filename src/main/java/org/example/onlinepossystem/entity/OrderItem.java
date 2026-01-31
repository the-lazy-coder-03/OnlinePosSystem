package org.example.onlinepossystem.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "order_item")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonBackReference
    private Order order;

    @ManyToOne
    @JoinColumn(name = "pizza_id", nullable = false)
    private MenuItem menuItem;

    @ManyToOne
    @JoinColumn(name = "pizza_size_id", nullable = false)
    private PizzaSize pizzaSize;

    @Column(nullable = false)
    private Integer qty = 1;

    @Column(name = "base_price_at_time", nullable = false)
    private Double price;

    private String notes;

    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemTopping> toppings = new ArrayList<>();

    public OrderItem() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    public MenuItem getMenuItem() { return menuItem; }
    public void setMenuItem(MenuItem menuItem) { this.menuItem = menuItem; }
    public PizzaSize getPizzaSize() { return pizzaSize; }
    public void setPizzaSize(PizzaSize pizzaSize) { this.pizzaSize = pizzaSize; }
    public Integer getQty() { return qty; }
    public void setQty(Integer qty) { this.qty = qty; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public List<OrderItemTopping> getToppings() { return toppings; }
    public void setToppings(List<OrderItemTopping> toppings) { this.toppings = toppings; }

    public void addTopping(OrderItemTopping topping) {
        toppings.add(topping);
        topping.setOrderItem(this);
    }
}
