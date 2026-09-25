package org.example.onlinepossystem.ordering.service;

import org.example.onlinepossystem.branch.api.BranchLookup;
import org.example.onlinepossystem.catalog.api.OrderCatalogResolver;
import org.example.onlinepossystem.ordering.dto.OrderResponseDTO;
import org.example.onlinepossystem.ordering.entity.Order;
import org.example.onlinepossystem.ordering.entity.OrderBurgerExtraComponent;
import org.example.onlinepossystem.ordering.entity.OrderBurgerProtein;
import org.example.onlinepossystem.ordering.entity.OrderBurgerRemovedComponent;
import org.example.onlinepossystem.ordering.entity.OrderMenuItem;
import org.example.onlinepossystem.ordering.entity.OrderMenuItemExtra;
import org.example.onlinepossystem.ordering.entity.OrderPizzaItem;
import org.example.onlinepossystem.ordering.entity.OrderPizzaItemExtra;
import org.example.onlinepossystem.ordering.entity.OrderSpecialItem;
import org.example.onlinepossystem.ordering.entity.OrderSpecialSelection;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

@Component
public class OrderResponseMapper {
    private final BranchLookup branchLookup;
    private final OrderCatalogResolver catalogResolver;

    public OrderResponseMapper(BranchLookup branchLookup, OrderCatalogResolver catalogResolver) {
        this.branchLookup = branchLookup;
        this.catalogResolver = catalogResolver;
    }

    public OrderResponseDTO toDto(Order order) {
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(order.getId());
        if (order.getBranchId() != null) {
            dto.setBranchId(order.getBranchId());
            dto.setBranchName(branchLookup.requireById(order.getBranchId()).name());
        }
        dto.setStatus(order.getStatus());
        dto.setOrderType(order.getOrderType());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setCustomerName(order.getCustomerName());
        dto.setPhone(order.getPhone());
        dto.setHouseNumber(order.getHouseNumber());
        dto.setStreet(order.getStreet());
        dto.setArea(order.getArea());
        dto.setCity(order.getCity());
        dto.setPostalCode(order.getPostalCode());
        dto.setComplexName(order.getComplexName());
        dto.setGateAccessCode(order.getGateAccessCode());
        dto.setNotes(order.getNotes());
        Set<OrderMenuItem> bundledMenuItems = new HashSet<>();
        Set<OrderPizzaItem> bundledPizzaItems = new HashSet<>();
        for (OrderSpecialItem special : safe(order.getSpecialItems())) {
            for (OrderSpecialSelection selection : safe(special.getSelections())) {
                if (selection.getMenuItem() != null) bundledMenuItems.add(selection.getMenuItem());
                if (selection.getPizzaItem() != null) bundledPizzaItems.add(selection.getPizzaItem());
            }
        }
        dto.setMenuItems(toMenuItemDtos(safe(order.getMenuItems()).stream()
                .filter(item -> !bundledMenuItems.contains(item)).toList()));
        dto.setPizzaItems(toPizzaItemDtos(safe(order.getPizzaItems()).stream()
                .filter(item -> !bundledPizzaItems.contains(item)).toList()));
        dto.setSpecialItems(toSpecialItemDtos(order.getSpecialItems()));
        return dto;
    }

    private List<OrderResponseDTO.SpecialItemDTO> toSpecialItemDtos(List<OrderSpecialItem> items) {
        List<OrderResponseDTO.SpecialItemDTO> result = new ArrayList<>();
        for (OrderSpecialItem item : safe(items)) {
            OrderResponseDTO.SpecialItemDTO dto = new OrderResponseDTO.SpecialItemDTO();
            dto.setId(item.getId());
            dto.setSpecialId(item.getSpecialId());
            dto.setName(item.getSpecialNameAtTime());
            dto.setDescription(item.getSpecialDescriptionAtTime());
            dto.setQuantity(item.getQuantity());
            dto.setBasePriceAtTime(toDouble(item.getBasePriceAtTime()));
            dto.setCustomizationTotalAtTime(toDouble(item.getCustomizationTotalAtTime()));
            dto.setAddonTotalAtTime(toDouble(item.getAddonTotalAtTime()));
            dto.setFinalLineTotalAtTime(toDouble(item.getFinalLineTotalAtTime()));
            List<OrderResponseDTO.SpecialSelectionDTO> selections = new ArrayList<>();
            for (OrderSpecialSelection selection : safe(item.getSelections())) {
                OrderResponseDTO.SpecialSelectionDTO selectionDto = new OrderResponseDTO.SpecialSelectionDTO();
                selectionDto.setKind(selection.getSelectionKind());
                selectionDto.setSelectionIndex(selection.getSelectionIndex());
                selectionDto.setLabel(selection.getLabelAtTime());
                selectionDto.setProductName(selection.getProductNameAtTime());
                selectionDto.setPizzaSizeCm(selection.getPizzaSizeCmAtTime());
                selectionDto.setQuantity(selection.getSelectionQuantity());
                selectionDto.setAddonPriceAtTime(toDouble(selection.getAddonPriceAtTime()));
                selectionDto.setCustomizationChargeAtTime(toDouble(selection.getCustomizationChargeAtTime()));
                if (selection.getMenuItem() != null) selectionDto.setMenuItem(toMenuItemDto(selection.getMenuItem()));
                if (selection.getPizzaItem() != null) selectionDto.setPizzaItem(toPizzaItemDto(selection.getPizzaItem()));
                selections.add(selectionDto);
            }
            dto.setSelections(selections);
            result.add(dto);
        }
        return result;
    }

    private List<OrderResponseDTO.MenuItemDTO> toMenuItemDtos(List<OrderMenuItem> items) {
        List<OrderResponseDTO.MenuItemDTO> menuItems = new ArrayList<>();
        if (items == null) {
            return menuItems;
        }

        for (OrderMenuItem item : items) menuItems.add(toMenuItemDto(item));
        return menuItems;
    }

    private OrderResponseDTO.MenuItemDTO toMenuItemDto(OrderMenuItem item) {
        OrderResponseDTO.MenuItemDTO itemDto = new OrderResponseDTO.MenuItemDTO();
        itemDto.setId(item.getId());
        if (item.getMenuItemId() != null) {
            itemDto.setMenuItemId(item.getMenuItemId());
            itemDto.setMenuItemName(item.getItemNameAtTime());
            if (itemDto.getMenuItemName() == null || itemDto.getMenuItemName().isBlank()) {
                catalogResolver.findMenuItem(item.getMenuItemId())
                        .ifPresent(reference -> itemDto.setMenuItemName(reference.name()));
            }
        }
        itemDto.setQty(item.getQty());
        itemDto.setUnitPriceAtTime(item.getUnitPriceAtTime());
        itemDto.setNotes(item.getNotes());
        itemDto.setExtras(toMenuItemExtraDtos(item));
        return itemDto;
    }

    private List<OrderResponseDTO.MenuItemExtraDTO> toMenuItemExtraDtos(OrderMenuItem item) {
        List<OrderResponseDTO.MenuItemExtraDTO> extraDtos = new ArrayList<>();
        if (item.getBurgerProtein() != null && item.getBurgerProtein().getComponentId() != null) {
            OrderBurgerProtein protein = item.getBurgerProtein();
            OrderResponseDTO.MenuItemExtraDTO proteinDto = new OrderResponseDTO.MenuItemExtraDTO();
            proteinDto.setName("Protein: " + snapshotOr(protein.getComponentNameAtTime(), protein.getComponentId()));
            proteinDto.setQty(protein.getProteinQtyPerBurger());
            proteinDto.setUnitPriceAtTime(toDouble(protein.getUnitPriceAtTime()));
            extraDtos.add(proteinDto);
        }
        if (item.getRemovedBurgerComponents() != null) {
            for (OrderBurgerRemovedComponent removedComponent : item.getRemovedBurgerComponents()) {
                if (removedComponent.getComponentId() == null) {
                    continue;
                }
                OrderResponseDTO.MenuItemExtraDTO removedDto = new OrderResponseDTO.MenuItemExtraDTO();
                removedDto.setName("No " + snapshotOr(removedComponent.getComponentNameAtTime(), removedComponent.getComponentId()));
                removedDto.setQty(1);
                removedDto.setUnitPriceAtTime(0.0);
                extraDtos.add(removedDto);
            }
        }
        if (item.getExtraBurgerComponents() != null) {
            for (OrderBurgerExtraComponent extraComponent : item.getExtraBurgerComponents()) {
                if (extraComponent.getComponentId() == null) {
                    continue;
                }
                OrderResponseDTO.MenuItemExtraDTO extraDto = new OrderResponseDTO.MenuItemExtraDTO();
                extraDto.setName("Extra " + snapshotOr(extraComponent.getComponentNameAtTime(), extraComponent.getComponentId()));
                extraDto.setQty(extraComponent.getQty());
                extraDto.setUnitPriceAtTime(toDouble(extraComponent.getUnitPriceAtTime()));
                extraDtos.add(extraDto);
            }
        }
        if (item.getExtras() != null) {
            for (OrderMenuItemExtra extra : item.getExtras()) {
                OrderResponseDTO.MenuItemExtraDTO extraDto = new OrderResponseDTO.MenuItemExtraDTO();
                extraDto.setId(extra.getId());
                extraDto.setName(extra.getName());
                extraDto.setQty(extra.getQty());
                extraDto.setUnitPriceAtTime(extra.getUnitPriceAtTime());
                extraDtos.add(extraDto);
            }
        }
        return extraDtos;
    }

    private List<OrderResponseDTO.PizzaItemDTO> toPizzaItemDtos(List<OrderPizzaItem> items) {
        List<OrderResponseDTO.PizzaItemDTO> pizzaItems = new ArrayList<>();
        if (items == null) {
            return pizzaItems;
        }

        for (OrderPizzaItem item : items) pizzaItems.add(toPizzaItemDto(item));
        return pizzaItems;
    }

    private OrderResponseDTO.PizzaItemDTO toPizzaItemDto(OrderPizzaItem item) {
        OrderResponseDTO.PizzaItemDTO itemDto = new OrderResponseDTO.PizzaItemDTO();
        itemDto.setId(item.getId());
        itemDto.setPizzaId(item.getPizzaId());
        itemDto.setPizzaName(item.getPizzaNameAtTime());
        if ((itemDto.getPizzaName() == null || itemDto.getPizzaName().isBlank()) && item.getPizzaId() != null) {
            catalogResolver.findPizza(item.getPizzaId()).ifPresent(reference -> itemDto.setPizzaName(reference.name()));
        }
        itemDto.setPizzaSizeId(item.getPizzaSizeId());
        itemDto.setPizzaSizeCm(item.getPizzaSizeCmAtTime());
        if (itemDto.getPizzaSizeCm() == null && item.getPizzaSizeId() != null) {
            catalogResolver.findPizzaSize(item.getPizzaSizeId()).ifPresent(reference -> itemDto.setPizzaSizeCm(reference.cm()));
        }
        itemDto.setQty(item.getQty());
        itemDto.setBasePriceAtTime(item.getBasePriceAtTime());
        if (item.getBaseOption() != null) {
            itemDto.setPizzaBaseOptionId(item.getBaseOption().getPizzaBaseOptionId());
            itemDto.setPizzaBaseOptionPriceAtTime(item.getBaseOption().getUnitPriceAtTime());
            itemDto.setPizzaBaseOptionName(item.getBaseOption().getBaseOptionNameAtTime());
            if (itemDto.getPizzaBaseOptionName() == null || itemDto.getPizzaBaseOptionName().isBlank()) {
                catalogResolver.findPizzaBaseOption(item.getBaseOption().getPizzaBaseOptionId())
                        .ifPresent(reference -> itemDto.setPizzaBaseOptionName(reference.name()));
            }
        }
        itemDto.setNotes(item.getNotes());
        itemDto.setExtras(toPizzaItemExtraDtos(item.getExtras()));
        itemDto.setRemovedIngredients(safe(item.getRemovedIngredients()).stream()
                .map(value -> value.getIngredientNameAtTime() == null || value.getIngredientNameAtTime().isBlank()
                        ? catalogResolver.findIngredient(value.getIngredientId()).map(OrderCatalogResolver.NamedReference::name)
                        .orElse("Ingredient " + value.getIngredientId())
                        : value.getIngredientNameAtTime()).toList());
        return itemDto;
    }

    private List<OrderResponseDTO.PizzaItemExtraDTO> toPizzaItemExtraDtos(List<OrderPizzaItemExtra> extras) {
        List<OrderResponseDTO.PizzaItemExtraDTO> extraDtos = new ArrayList<>();
        if (extras == null) {
            return extraDtos;
        }

        for (OrderPizzaItemExtra extra : extras) {
            OrderResponseDTO.PizzaItemExtraDTO extraDto = new OrderResponseDTO.PizzaItemExtraDTO();
            extraDto.setId(extra.getId());
            if (extra.getIngredientId() != null) {
                extraDto.setIngredientId(extra.getIngredientId());
                extraDto.setIngredientName(extra.getIngredientNameAtTime());
                if (extraDto.getIngredientName() == null || extraDto.getIngredientName().isBlank()) {
                    catalogResolver.findIngredient(extra.getIngredientId())
                            .ifPresent(reference -> extraDto.setIngredientName(reference.name()));
                }
            }
            extraDto.setQty(extra.getQty());
            extraDto.setUnitPriceAtTime(extra.getUnitPriceAtTime());
            extraDtos.add(extraDto);
        }
        return extraDtos;
    }

    private Double toDouble(BigDecimal value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    private String componentName(Integer componentId) {
        return catalogResolver.findBurgerComponent(componentId)
                .map(OrderCatalogResolver.NamedReference::name)
                .orElse("Component " + componentId);
    }

    private String snapshotOr(String snapshot, Integer componentId) {
        return snapshot == null || snapshot.isBlank() ? componentName(componentId) : snapshot;
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }
}
