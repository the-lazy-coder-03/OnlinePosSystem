package org.example.onlinepossystem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_menu_item_extra")
public class OrderMenuItemExtra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_menu_item_extra_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_menu_item_id", nullable = false)
    private OrderMenuItem orderMenuItem;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer qty = 1;

    @Column(name = "unit_price_at_time", nullable = false)
    private Double unitPriceAtTime = 0.0;

    public OrderMenuItemExtra() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public OrderMenuItem getOrderMenuItem() { return orderMenuItem; }
    public void setOrderMenuItem(OrderMenuItem orderMenuItem) { this.orderMenuItem = orderMenuItem; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getQty() { return qty; }
    public void setQty(Integer qty) { this.qty = qty; }
    public Double getUnitPriceAtTime() { return unitPriceAtTime; }
    public void setUnitPriceAtTime(Double unitPriceAtTime) { this.unitPriceAtTime = unitPriceAtTime; }
}
