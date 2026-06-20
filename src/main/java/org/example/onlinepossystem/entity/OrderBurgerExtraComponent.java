package org.example.onlinepossystem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "order_burger_extra_component")
public class OrderBurgerExtraComponent {
    @EmbeddedId
    private OrderBurgerExtraComponentId id = new OrderBurgerExtraComponentId();

    @ManyToOne
    @MapsId("orderMenuItemId")
    @JoinColumn(name = "order_menu_item_id")
    private OrderMenuItem orderMenuItem;

    @ManyToOne
    @MapsId("componentId")
    @JoinColumn(name = "component_id")
    private BurgerComponent component;

    @Column(nullable = false)
    private Integer qty = 1;

    @Column(name = "unit_price_at_time", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPriceAtTime = BigDecimal.ZERO;

    public OrderBurgerExtraComponent() {
    }

    public OrderBurgerExtraComponentId getId() {
        return id;
    }

    public void setId(OrderBurgerExtraComponentId id) {
        this.id = id;
    }

    public OrderMenuItem getOrderMenuItem() {
        return orderMenuItem;
    }

    public void setOrderMenuItem(OrderMenuItem orderMenuItem) {
        this.orderMenuItem = orderMenuItem;
        if (orderMenuItem != null) {
            this.id.setOrderMenuItemId(orderMenuItem.getId());
        }
    }

    public BurgerComponent getComponent() {
        return component;
    }

    public void setComponent(BurgerComponent component) {
        this.component = component;
        if (component != null) {
            this.id.setComponentId(component.getId());
        }
    }

    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }

    public BigDecimal getUnitPriceAtTime() {
        return unitPriceAtTime;
    }

    public void setUnitPriceAtTime(BigDecimal unitPriceAtTime) {
        this.unitPriceAtTime = unitPriceAtTime;
    }
}
