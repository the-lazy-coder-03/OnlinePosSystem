package org.example.onlinepossystem.ordering.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

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

    @Column(name = "pizza_id", nullable = false)
    private Integer pizzaId;

    @Column(name = "pizza_size_id", nullable = false)
    private Integer pizzaSizeId;

    @Column(nullable = false)
    private Integer qty = 1;

    @Column(name = "base_price_at_time", nullable = false)
    private Double basePriceAtTime;

    @Column(name = "pizza_name_at_time")
    private String pizzaNameAtTime;

    @Column(name = "pizza_size_cm_at_time")
    private Integer pizzaSizeCmAtTime;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "orderPizzaItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderPizzaItemExtra> extras = new ArrayList<>();

    @OneToMany(mappedBy = "orderPizzaItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderPizzaItemRemovedIngredient> removedIngredients = new ArrayList<>();

    @OneToOne(mappedBy = "orderPizzaItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private OrderPizzaItemBaseOption baseOption;

    public OrderPizzaItem() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    public Integer getPizzaId() { return pizzaId; }
    public void setPizzaId(Integer pizzaId) { this.pizzaId = pizzaId; }
    public Integer getPizzaSizeId() { return pizzaSizeId; }
    public void setPizzaSizeId(Integer pizzaSizeId) { this.pizzaSizeId = pizzaSizeId; }
    public Integer getQty() { return qty; }
    public void setQty(Integer qty) { this.qty = qty; }
    public Double getBasePriceAtTime() { return basePriceAtTime; }
    public void setBasePriceAtTime(Double basePriceAtTime) { this.basePriceAtTime = basePriceAtTime; }
    public String getPizzaNameAtTime() { return pizzaNameAtTime; }
    public void setPizzaNameAtTime(String pizzaNameAtTime) { this.pizzaNameAtTime = pizzaNameAtTime; }
    public Integer getPizzaSizeCmAtTime() { return pizzaSizeCmAtTime; }
    public void setPizzaSizeCmAtTime(Integer pizzaSizeCmAtTime) { this.pizzaSizeCmAtTime = pizzaSizeCmAtTime; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public List<OrderPizzaItemExtra> getExtras() { return extras; }
    public void setExtras(List<OrderPizzaItemExtra> extras) { this.extras = extras; }
    public void addExtra(OrderPizzaItemExtra extra) {
        extras.add(extra);
        extra.setOrderPizzaItem(this);
    }
    public List<OrderPizzaItemRemovedIngredient> getRemovedIngredients() { return removedIngredients; }
    public void addRemovedIngredient(OrderPizzaItemRemovedIngredient removed) {
        removedIngredients.add(removed);
        removed.setOrderPizzaItem(this);
    }
    public OrderPizzaItemBaseOption getBaseOption() { return baseOption; }
    public void setBaseOption(OrderPizzaItemBaseOption baseOption) {
        this.baseOption = baseOption;
        if (baseOption != null) {
            baseOption.setOrderPizzaItem(this);
        }
    }
}
