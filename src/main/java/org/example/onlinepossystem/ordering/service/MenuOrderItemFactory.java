package org.example.onlinepossystem.ordering.service;

import org.example.onlinepossystem.catalog.api.OrderCatalogResolver;
import org.example.onlinepossystem.ordering.dto.OrderRequestDTO;
import org.example.onlinepossystem.ordering.entity.OrderBurgerExtraComponent;
import org.example.onlinepossystem.ordering.entity.OrderBurgerProtein;
import org.example.onlinepossystem.ordering.entity.OrderBurgerRemovedComponent;
import org.example.onlinepossystem.ordering.entity.OrderMenuItem;
import org.example.onlinepossystem.ordering.entity.OrderMenuItemExtra;
import org.springframework.stereotype.Component;

@Component
public class MenuOrderItemFactory {
    private final OrderCatalogResolver catalogResolver;
    private final OrderCustomizationMapper customizationMapper;

    public MenuOrderItemFactory(OrderCatalogResolver catalogResolver,
                                OrderCustomizationMapper customizationMapper) {
        this.catalogResolver = catalogResolver;
        this.customizationMapper = customizationMapper;
    }

    public OrderMenuItem create(Integer branchId, OrderRequestDTO.OrderItemRequestDTO request) {
        OrderCatalogResolver.ResolvedMenuItem resolvedItem = catalogResolver.resolveMenuItem(
                branchId,
                request.getMenuItemId(),
                customizationMapper.toCatalogRequests(request.getCustomizations())
        );

        OrderMenuItem orderItem = new OrderMenuItem();
        orderItem.setMenuItemId(resolvedItem.menuItemId());
        orderItem.setItemNameAtTime(resolvedItem.menuItemName());
        orderItem.setQty(request.getQuantity());
        orderItem.setUnitPriceAtTime(resolvedItem.unitPrice());
        orderItem.setNotes(request.getNotes());

        applyBurgerSelection(orderItem, resolvedItem.burgerSelection());
        for (OrderCatalogResolver.ResolvedGenericMenuExtra resolvedExtra : resolvedItem.extras()) {
            OrderMenuItemExtra extra = new OrderMenuItemExtra();
            extra.setName(resolvedExtra.name());
            extra.setQty(resolvedExtra.quantity());
            extra.setUnitPriceAtTime(resolvedExtra.unitPrice());
            orderItem.addExtra(extra);
        }
        return orderItem;
    }

    private void applyBurgerSelection(
            OrderMenuItem orderItem,
            OrderCatalogResolver.ResolvedBurgerSelection burgerSelection
    ) {
        if (burgerSelection == null) {
            return;
        }

        if (burgerSelection.protein() != null) {
            OrderCatalogResolver.ResolvedBurgerProtein resolvedProtein = burgerSelection.protein();
            OrderBurgerProtein protein = new OrderBurgerProtein();
            protein.setComponentId(resolvedProtein.componentId());
            protein.setProteinQtyPerBurger(resolvedProtein.quantity());
            protein.setUnitPriceAtTime(resolvedProtein.unitPrice());
            orderItem.setBurgerProtein(protein);
        }

        for (OrderCatalogResolver.ResolvedBurgerComponent resolvedComponent : burgerSelection.removedComponents()) {
            OrderBurgerRemovedComponent removedComponent = new OrderBurgerRemovedComponent();
            removedComponent.setComponentId(resolvedComponent.componentId());
            orderItem.addRemovedBurgerComponent(removedComponent);
        }

        for (OrderCatalogResolver.ResolvedBurgerComponent resolvedComponent : burgerSelection.extraComponents()) {
            OrderBurgerExtraComponent extraComponent = new OrderBurgerExtraComponent();
            extraComponent.setComponentId(resolvedComponent.componentId());
            extraComponent.setQty(resolvedComponent.quantity());
            extraComponent.setUnitPriceAtTime(resolvedComponent.unitPrice());
            orderItem.addExtraBurgerComponent(extraComponent);
        }
    }
}
