package org.example.onlinepossystem.ordering.entity;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class OrderBurgerExtraComponentId implements Serializable {
    private Long orderMenuItemId;
    private Integer componentId;

    public OrderBurgerExtraComponentId() {
    }

    public OrderBurgerExtraComponentId(Long orderMenuItemId, Integer componentId) {
        this.orderMenuItemId = orderMenuItemId;
        this.componentId = componentId;
    }

    public Long getOrderMenuItemId() {
        return orderMenuItemId;
    }

    public void setOrderMenuItemId(Long orderMenuItemId) {
        this.orderMenuItemId = orderMenuItemId;
    }

    public Integer getComponentId() {
        return componentId;
    }

    public void setComponentId(Integer componentId) {
        this.componentId = componentId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OrderBurgerExtraComponentId that = (OrderBurgerExtraComponentId) o;
        return Objects.equals(orderMenuItemId, that.orderMenuItemId)
                && Objects.equals(componentId, that.componentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderMenuItemId, componentId);
    }
}
