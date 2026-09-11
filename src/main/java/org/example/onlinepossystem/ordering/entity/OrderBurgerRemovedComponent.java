package org.example.onlinepossystem.ordering.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_burger_removed_component")
public class OrderBurgerRemovedComponent {
    @EmbeddedId
    private OrderBurgerRemovedComponentId id = new OrderBurgerRemovedComponentId();

    @ManyToOne
    @MapsId("orderMenuItemId")
    @JoinColumn(name = "order_menu_item_id")
    private OrderMenuItem orderMenuItem;

    public OrderBurgerRemovedComponent() {
    }

    public OrderBurgerRemovedComponentId getId() {
        return id;
    }

    public void setId(OrderBurgerRemovedComponentId id) {
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

    public Integer getComponentId() {
        return id == null ? null : id.getComponentId();
    }

    public void setComponentId(Integer componentId) {
        this.id.setComponentId(componentId);
    }
}
