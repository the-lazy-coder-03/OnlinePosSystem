package org.example.onlinepossystem.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "order_menu_item")
public class OrderMenuItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_menu_item_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonBackReference
    private Order order;

    @ManyToOne
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;

    @Column(nullable = false)
    private Integer qty = 1;

    @Column(name = "unit_price_at_time", nullable = false)
    private Double unitPriceAtTime;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "orderMenuItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderMenuItemExtra> extras = new ArrayList<>();

    public OrderMenuItem() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    public MenuItem getMenuItem() { return menuItem; }
    public void setMenuItem(MenuItem menuItem) { this.menuItem = menuItem; }
    public Integer getQty() { return qty; }
    public void setQty(Integer qty) { this.qty = qty; }
    public Double getUnitPriceAtTime() { return unitPriceAtTime; }
    public void setUnitPriceAtTime(Double unitPriceAtTime) { this.unitPriceAtTime = unitPriceAtTime; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public List<OrderMenuItemExtra> getExtras() { return extras; }
    public void setExtras(List<OrderMenuItemExtra> extras) { this.extras = extras; }
    public void addExtra(OrderMenuItemExtra extra) {
        extras.add(extra);
        extra.setOrderMenuItem(this);
    }
}
