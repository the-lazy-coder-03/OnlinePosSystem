package org.example.onlinepossystem.ordering.service;

import org.example.onlinepossystem.catalog.api.OrderCatalogResolver;
import org.example.onlinepossystem.ordering.dto.OrderRequestDTO;
import org.example.onlinepossystem.ordering.entity.OrderPizzaItem;
import org.example.onlinepossystem.ordering.entity.OrderPizzaItemExtra;
import org.springframework.stereotype.Component;

@Component
public class PizzaOrderItemFactory {
    private final OrderCatalogResolver catalogResolver;
    private final OrderCustomizationMapper customizationMapper;

    public PizzaOrderItemFactory(OrderCatalogResolver catalogResolver,
                                 OrderCustomizationMapper customizationMapper) {
        this.catalogResolver = catalogResolver;
        this.customizationMapper = customizationMapper;
    }

    public OrderPizzaItem create(Integer branchId, OrderRequestDTO.OrderItemRequestDTO request) {
        OrderCatalogResolver.ResolvedPizzaItem resolvedItem = catalogResolver.resolvePizzaItem(
                branchId,
                request.getPizzaId(),
                request.getPizzaSizeId(),
                request.getSizeCm(),
                customizationMapper.toCatalogRequests(request.getCustomizations())
        );

        OrderPizzaItem pizzaItem = new OrderPizzaItem();
        pizzaItem.setPizzaId(resolvedItem.pizzaId());
        pizzaItem.setPizzaSizeId(resolvedItem.pizzaSizeId());
        pizzaItem.setQty(request.getQuantity());
        pizzaItem.setBasePriceAtTime(resolvedItem.basePrice());
        pizzaItem.setNotes(request.getNotes());

        for (OrderCatalogResolver.ResolvedPizzaExtra resolvedExtra : resolvedItem.extras()) {
            OrderPizzaItemExtra extra = new OrderPizzaItemExtra();
            extra.setIngredientId(resolvedExtra.ingredientId());
            extra.setQty(resolvedExtra.quantity());
            extra.setUnitPriceAtTime(resolvedExtra.unitPrice());
            pizzaItem.addExtra(extra);
        }
        return pizzaItem;
    }
}
