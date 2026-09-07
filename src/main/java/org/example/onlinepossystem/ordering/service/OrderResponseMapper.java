package org.example.onlinepossystem.ordering.service;

import org.example.onlinepossystem.ordering.dto.OrderResponseDTO;
import org.example.onlinepossystem.ordering.entity.Order;
import org.example.onlinepossystem.ordering.entity.OrderBurgerExtraComponent;
import org.example.onlinepossystem.ordering.entity.OrderBurgerProtein;
import org.example.onlinepossystem.ordering.entity.OrderBurgerRemovedComponent;
import org.example.onlinepossystem.ordering.entity.OrderMenuItem;
import org.example.onlinepossystem.ordering.entity.OrderMenuItemExtra;
import org.example.onlinepossystem.ordering.entity.OrderPizzaItem;
import org.example.onlinepossystem.ordering.entity.OrderPizzaItemExtra;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class OrderResponseMapper {
    public OrderResponseDTO toDto(Order order) {
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(order.getId());
        if (order.getBranch() != null) {
            dto.setBranchId(order.getBranch().getId());
            dto.setBranchName(order.getBranch().getName());
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
        dto.setNotes(order.getNotes());
        dto.setMenuItems(toMenuItemDtos(order.getMenuItems()));
        dto.setPizzaItems(toPizzaItemDtos(order.getPizzaItems()));
        return dto;
    }

    private List<OrderResponseDTO.MenuItemDTO> toMenuItemDtos(List<OrderMenuItem> items) {
        List<OrderResponseDTO.MenuItemDTO> menuItems = new ArrayList<>();
        if (items == null) {
            return menuItems;
        }

        for (OrderMenuItem item : items) {
            OrderResponseDTO.MenuItemDTO itemDto = new OrderResponseDTO.MenuItemDTO();
            itemDto.setId(item.getId());
            if (item.getMenuItem() != null) {
                itemDto.setMenuItemId(item.getMenuItem().getId());
                itemDto.setMenuItemName(item.getMenuItem().getName());
            }
            itemDto.setQty(item.getQty());
            itemDto.setUnitPriceAtTime(item.getUnitPriceAtTime());
            itemDto.setNotes(item.getNotes());
            itemDto.setExtras(toMenuItemExtraDtos(item));
            menuItems.add(itemDto);
        }
        return menuItems;
    }

    private List<OrderResponseDTO.MenuItemExtraDTO> toMenuItemExtraDtos(OrderMenuItem item) {
        List<OrderResponseDTO.MenuItemExtraDTO> extraDtos = new ArrayList<>();
        if (item.getBurgerProtein() != null && item.getBurgerProtein().getComponent() != null) {
            OrderBurgerProtein protein = item.getBurgerProtein();
            OrderResponseDTO.MenuItemExtraDTO proteinDto = new OrderResponseDTO.MenuItemExtraDTO();
            proteinDto.setName("Protein: " + protein.getComponent().getName());
            proteinDto.setQty(protein.getProteinQtyPerBurger());
            proteinDto.setUnitPriceAtTime(toDouble(protein.getUnitPriceAtTime()));
            extraDtos.add(proteinDto);
        }
        if (item.getRemovedBurgerComponents() != null) {
            for (OrderBurgerRemovedComponent removedComponent : item.getRemovedBurgerComponents()) {
                if (removedComponent.getComponent() == null) {
                    continue;
                }
                OrderResponseDTO.MenuItemExtraDTO removedDto = new OrderResponseDTO.MenuItemExtraDTO();
                removedDto.setName("No " + removedComponent.getComponent().getName());
                removedDto.setQty(1);
                removedDto.setUnitPriceAtTime(0.0);
                extraDtos.add(removedDto);
            }
        }
        if (item.getExtraBurgerComponents() != null) {
            for (OrderBurgerExtraComponent extraComponent : item.getExtraBurgerComponents()) {
                if (extraComponent.getComponent() == null) {
                    continue;
                }
                OrderResponseDTO.MenuItemExtraDTO extraDto = new OrderResponseDTO.MenuItemExtraDTO();
                extraDto.setName("Extra " + extraComponent.getComponent().getName());
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

        for (OrderPizzaItem item : items) {
            OrderResponseDTO.PizzaItemDTO itemDto = new OrderResponseDTO.PizzaItemDTO();
            itemDto.setId(item.getId());
            if (item.getPizza() != null) {
                itemDto.setPizzaId(item.getPizza().getId());
                itemDto.setPizzaName(item.getPizza().getName());
            }
            if (item.getPizzaSize() != null) {
                itemDto.setPizzaSizeId(item.getPizzaSize().getId());
                itemDto.setPizzaSizeCm(item.getPizzaSize().getCm());
            }
            itemDto.setQty(item.getQty());
            itemDto.setBasePriceAtTime(item.getBasePriceAtTime());
            itemDto.setNotes(item.getNotes());
            itemDto.setExtras(toPizzaItemExtraDtos(item.getExtras()));
            pizzaItems.add(itemDto);
        }
        return pizzaItems;
    }

    private List<OrderResponseDTO.PizzaItemExtraDTO> toPizzaItemExtraDtos(List<OrderPizzaItemExtra> extras) {
        List<OrderResponseDTO.PizzaItemExtraDTO> extraDtos = new ArrayList<>();
        if (extras == null) {
            return extraDtos;
        }

        for (OrderPizzaItemExtra extra : extras) {
            OrderResponseDTO.PizzaItemExtraDTO extraDto = new OrderResponseDTO.PizzaItemExtraDTO();
            extraDto.setId(extra.getId());
            if (extra.getIngredient() != null) {
                extraDto.setIngredientId(extra.getIngredient().getId());
                extraDto.setIngredientName(extra.getIngredient().getName());
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
}
