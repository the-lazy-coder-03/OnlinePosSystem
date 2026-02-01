package org.example.onlinepossystem.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "order_pizza_item")
public class OrderPizzaItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_pizza_item_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonBackReference
    private Order order;

    @ManyToOne
    @JoinColumn(name = "pizza_id", nullable = false)
    private Pizza pizza;

    @ManyToOne
    @JoinColumn(name = "pizza_size_id", nullable = false)
    private PizzaSize pizzaSize;

    @Column(nullable = false)
    private Integer qty = 1;

    @Column(name = "base_price_at_time", nullable = false)
    private Double basePriceAtTime;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "orderPizzaItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderPizzaItemExtra> extras = new ArrayList<>();

    public OrderPizzaItem() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    public Pizza getPizza() { return pizza; }
    public void setPizza(Pizza pizza) { this.pizza = pizza; }
    public PizzaSize getPizzaSize() { return pizzaSize; }
    public void setPizzaSize(PizzaSize pizzaSize) { this.pizzaSize = pizzaSize; }
    public Integer getQty() { return qty; }
    public void setQty(Integer qty) { this.qty = qty; }
    public Double getBasePriceAtTime() { return basePriceAtTime; }
    public void setBasePriceAtTime(Double basePriceAtTime) { this.basePriceAtTime = basePriceAtTime; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public List<OrderPizzaItemExtra> getExtras() { return extras; }
    public void setExtras(List<OrderPizzaItemExtra> extras) { this.extras = extras; }
    public void addExtra(OrderPizzaItemExtra extra) {
        extras.add(extra);
        extra.setOrderPizzaItem(this);
    }
}
