package org.example.onlinepossystem.ordering.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "order_burger_protein")
public class OrderBurgerProtein {
    @Id
    @Column(name = "order_menu_item_id")
    private Long orderMenuItemId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "order_menu_item_id")
    private OrderMenuItem orderMenuItem;

    @Column(name = "component_id", nullable = false)
    private Integer componentId;

    @Column(name = "component_name_at_time")
    private String componentNameAtTime;

    @Column(name = "protein_qty_per_burger", nullable = false)
    private Integer proteinQtyPerBurger;

    @Column(name = "unit_price_at_time", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPriceAtTime = BigDecimal.ZERO;

    public OrderBurgerProtein() {
    }

    public Long getOrderMenuItemId() {
        return orderMenuItemId;
    }

    public void setOrderMenuItemId(Long orderMenuItemId) {
        this.orderMenuItemId = orderMenuItemId;
    }

    public OrderMenuItem getOrderMenuItem() {
        return orderMenuItem;
    }

    public void setOrderMenuItem(OrderMenuItem orderMenuItem) {
        this.orderMenuItem = orderMenuItem;
        if (orderMenuItem != null) {
            this.orderMenuItemId = orderMenuItem.getId();
        }
    }

    public Integer getComponentId() {
        return componentId;
    }

    public void setComponentId(Integer componentId) {
        this.componentId = componentId;
    }

    public String getComponentNameAtTime() { return componentNameAtTime; }
    public void setComponentNameAtTime(String componentNameAtTime) { this.componentNameAtTime = componentNameAtTime; }

    public Integer getProteinQtyPerBurger() {
        return proteinQtyPerBurger;
    }

    public void setProteinQtyPerBurger(Integer proteinQtyPerBurger) {
        this.proteinQtyPerBurger = proteinQtyPerBurger;
    }

    public BigDecimal getUnitPriceAtTime() {
        return unitPriceAtTime;
    }

    public void setUnitPriceAtTime(BigDecimal unitPriceAtTime) {
        this.unitPriceAtTime = unitPriceAtTime;
    }
}
