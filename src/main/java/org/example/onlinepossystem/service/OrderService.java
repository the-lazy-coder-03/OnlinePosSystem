package org.example.onlinepossystem.service;

import org.example.onlinepossystem.dto.MenuDTO;
import org.example.onlinepossystem.dto.OrderRequestDTO;
import org.example.onlinepossystem.dto.OrderResponseDTO;
import org.example.onlinepossystem.entity.*;
import org.example.onlinepossystem.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private BranchMenuItemPriceRepository branchMenuItemPriceRepository;

    @Autowired
    private BurgerToppingRepository burgerToppingRepository;

    @Autowired
    private SaladIngredientRepository saladIngredientRepository;

    @Autowired
    private PizzaSizeRepository pizzaSizeRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private BranchPizzaPriceRepository branchPizzaPriceRepository;

    @Autowired
    private PizzaRepository pizzaRepository;

    @Autowired
    private PizzaAllowedSizeRepository pizzaAllowedSizeRepository;

    @Autowired
    private PizzaDefaultIngredientRepository pizzaDefaultIngredientRepository;

    @Autowired
    private BranchExtraPriceRepository branchExtraPriceRepository;

    @Autowired
    private ModifierOptionRepository modifierOptionRepository;

    public List<MenuDTO> getMenuForBranch(String branchName) {
        if (branchName == null || branchName.isBlank()) {
            throw new IllegalArgumentException("Branch name is required.");
        }
        Branch branch = branchRepository.findByName(branchName)
                .orElseThrow(() -> new java.util.NoSuchElementException("Branch not found: " + branchName));

        List<MenuDTO> menu = new ArrayList<>();

        // 1. Regular Menu Items
        List<BranchMenuItemPrice> menuItemPrices = branchMenuItemPriceRepository.findByBranchId(branch.getId());
        Map<Integer, Double> itemPriceMap = menuItemPrices.stream()
                .collect(Collectors.toMap(p -> p.getMenuItem().getId(), BranchMenuItemPrice::getPrice, (v1, v2) -> v1));

        List<MenuItem> items = menuItemRepository.findAllByActiveTrue();
        for (MenuItem i : items) {
            if (itemPriceMap.containsKey(i.getId())) {
                MenuDTO dto = new MenuDTO();
                dto.setCategoryName(i.getCategory() != null ? i.getCategory().getName() : "Uncategorized");
                dto.setCategoryId(i.getCategory() != null ? i.getCategory().getId() : null);
                dto.setMenuItemId(i.getId());
                dto.setMenuItemName(i.getName());
                dto.setDescription(i.getDescription());
                dto.setPrice(itemPriceMap.get(i.getId()));
                dto.setIs300ml(i.isIs300ml());
                dto.setIs2l(i.isIs2l());
                dto.setPizza(false);

                List<MenuDTO.CustomizationDTO> customizations = new ArrayList<>();
                List<BurgerTopping> toppings = burgerToppingRepository.findByBurgerId(i.getId());
                for (BurgerTopping t : toppings) {
                    MenuDTO.CustomizationDTO c = new MenuDTO.CustomizationDTO();
                    c.setId(t.getId());
                    c.setName(t.getToppingName());
                    c.setPrice(t.getPrice());
                    c.setDefault(t.isDefault());
                    customizations.add(c);
                }
                List<SaladIngredient> ingredients = saladIngredientRepository.findBySaladId(i.getId());
                for (SaladIngredient ing : ingredients) {
                    MenuDTO.CustomizationDTO c = new MenuDTO.CustomizationDTO();
                    c.setId(ing.getId());
                    c.setName(ing.getIngredientName());
                    c.setPrice(ing.getPrice());
                    c.setDefault(false);
                    customizations.add(c);
                }
                dto.setCustomizations(customizations);
                menu.add(dto);
            }
        }

        // 2. Pizzas
        List<BranchPizzaPrice> pizzaPrices = branchPizzaPriceRepository.findByBranchId(branch.getId());
        Map<Integer, List<BranchPizzaPrice>> pizzaPriceMap = pizzaPrices.stream()
                .collect(Collectors.groupingBy(p -> p.getPizza().getId()));

        List<Pizza> pizzas = pizzaRepository.findAllByActiveTrue();
        for (Pizza p : pizzas) {
            if (pizzaPriceMap.containsKey(p.getId())) {
                List<BranchPizzaPrice> bppList = pizzaPriceMap.get(p.getId());
                for (BranchPizzaPrice bpp : bppList) {
                    MenuDTO dto = new MenuDTO();
                    dto.setCategoryName(p.getCategory() != null ? p.getCategory().getName() : "Pizzas");
                    dto.setCategoryId(p.getCategory() != null ? p.getCategory().getId() : null);
                    dto.setMenuItemId(p.getId());
                    dto.setMenuItemName(p.getName() + " (" + bpp.getPizzaSize().getCm() + "cm)");
                    dto.setDescription(p.getDescription());
                    dto.setPrice(bpp.getPrice());
                    dto.setPizza(true);
                    dto.setPizzaSizeId(bpp.getPizzaSize().getId());

                    // Customizations for pizza (extra ingredients)
                    List<MenuDTO.CustomizationDTO> customizations = new ArrayList<>();
                    // Actually for pizza, we should show ALL ingredients and their branch-specific prices for this size
                    // But for simplicity in this step, let's just mark it as pizza.
                    dto.setCustomizations(customizations);
                    menu.add(dto);
                }
            }
        }

        return menu;
    }

    @Transactional
    public OrderResponseDTO placeOrder(OrderRequestDTO request) {
        validateOrderRequest(request);
        Branch branch = branchRepository.findByName(request.getBranchName())
                .orElseThrow(() -> new java.util.NoSuchElementException("Branch not found: " + request.getBranchName()));

        Order order = new Order();
        order.setBranch(branch);
        order.setCustomerName(request.getCustomerName());
        order.setPhone(request.getPhone());
        order.setHouseNumber(request.getHouseNumber());
        order.setStreet(request.getStreet());
        order.setArea(request.getArea());
        order.setCity(request.getCity());
        order.setPostalCode(request.getPostalCode());
        order.setComplexName(request.getComplexName());
        order.setOrderType(request.getOrderType() != null ? request.getOrderType() : "pickup");
        order.setCreatedAt(LocalDateTime.now());
        order.setStatus("Pending");

        for (OrderRequestDTO.OrderItemRequestDTO itemRequest : request.getItems()) {
            if (itemRequest.getPizzaId() != null) {
                Integer sizeId = itemRequest.getPizzaSizeId();
                if (sizeId == null && itemRequest.getSizeCm() != null) {
                    sizeId = pizzaSizeRepository.findByCm(itemRequest.getSizeCm())
                            .map(PizzaSize::getId)
                            .orElseThrow(() -> new java.util.NoSuchElementException("Pizza size not found for cm: " + itemRequest.getSizeCm()));
                }
                if (sizeId == null) {
                    throw new IllegalArgumentException("pizzaSizeId or sizeCm is required when pizzaId is provided.");
                }
                // Handle Pizza
                Pizza pizza = pizzaRepository.findById(itemRequest.getPizzaId())
                        .orElseThrow(() -> new java.util.NoSuchElementException("Pizza not found with ID: " + itemRequest.getPizzaId()));
                final Integer finalSizeId = sizeId;
                PizzaSize size = pizzaSizeRepository.findById(finalSizeId)
                        .orElseThrow(() -> new java.util.NoSuchElementException("Pizza size not found with ID: " + finalSizeId));
                BranchPizzaPrice bpp = branchPizzaPriceRepository
                        .findByBranchIdAndPizzaIdAndPizzaSizeId(branch.getId(), pizza.getId(), size.getId())
                        .orElseThrow(() -> new java.util.NoSuchElementException("Price not found for pizza: " + pizza.getName() + " size: " + size.getCm() + "cm in branch: " + branch.getName()));

                OrderPizzaItem pizzaItem = new OrderPizzaItem();
                pizzaItem.setPizza(pizza);
                pizzaItem.setPizzaSize(size);
                pizzaItem.setQty(itemRequest.getQuantity());
                pizzaItem.setBasePriceAtTime(bpp.getPrice());
                pizzaItem.setNotes(itemRequest.getNotes());

                if (itemRequest.getCustomizations() != null) {
                    for (OrderRequestDTO.CustomizationRequestDTO custReq : itemRequest.getCustomizations()) {
                        Ingredient ing = ingredientRepository.findById(custReq.getId())
                                .orElseThrow(() -> new java.util.NoSuchElementException("Ingredient not found with ID: " + custReq.getId()));
                        
                        BranchExtraPrice bep = branchExtraPriceRepository
                                .findById(new BranchExtraPrice.BranchExtraPriceId(branch.getId(), ing.getPriceCategory().getId(), size.getId()))
                                .orElseThrow(() -> new java.util.NoSuchElementException("Extra price not found for ingredient: " + ing.getName() + " size: " + size.getCm() + "cm in branch: " + branch.getName()));

                        OrderPizzaItemExtra extra = new OrderPizzaItemExtra();
                        extra.setIngredient(ing);
                        extra.setQty(custReq.getQuantity());
                        extra.setUnitPriceAtTime(bep.getPrice());
                        pizzaItem.addExtra(extra);
                    }
                }
                order.addPizzaItem(pizzaItem);

            } else {
                // Handle Menu Item
                if (itemRequest.getMenuItemId() == null) {
                    throw new IllegalArgumentException("Each item must have either menuItemId or pizzaId.");
                }
                MenuItem menuItem = menuItemRepository.findById(itemRequest.getMenuItemId())
                        .orElseThrow(() -> new java.util.NoSuchElementException("Menu item not found with ID: " + itemRequest.getMenuItemId()));

                BranchMenuItemPrice branchPrice = branchMenuItemPriceRepository
                        .findByBranchIdAndMenuItemId(branch.getId(), menuItem.getId())
                        .orElseThrow(() -> new java.util.NoSuchElementException("Price not found for menu item: " + menuItem.getName() + " in branch: " + branch.getName()));

                OrderMenuItem orderItem = new OrderMenuItem();
                orderItem.setMenuItem(menuItem);
                orderItem.setQty(itemRequest.getQuantity());
                orderItem.setUnitPriceAtTime(branchPrice.getPrice());
                orderItem.setNotes(itemRequest.getNotes());

                if (itemRequest.getCustomizations() != null) {
                    for (OrderRequestDTO.CustomizationRequestDTO custReq : itemRequest.getCustomizations()) {
                        OrderMenuItemExtra extra = new OrderMenuItemExtra();
                        // For non-pizza items, we'll use the name from BurgerTopping or SaladIngredient
                        String extraName = "";
                        Double extraPrice = 0.0;

                        ModifierOption modifierOption = modifierOptionRepository.findById(custReq.getId()).orElse(null);
                        if (modifierOption != null) {
                            extraName = modifierOption.name;
                            if (modifierOption.menuItemId != null) {
                                BranchMenuItemPrice extraPriceEntry = branchMenuItemPriceRepository
                                        .findByBranchIdAndMenuItemId(branch.getId(), modifierOption.menuItemId)
                                        .orElseThrow(() -> new java.util.NoSuchElementException("Price not found for modifier option menu item ID: " + modifierOption.menuItemId));
                                extraPrice = extraPriceEntry.getPrice();
                            }
                        } else {
                            BurgerTopping topping = burgerToppingRepository.findById(custReq.getId()).orElse(null);
                            if (topping != null) {
                                extraName = topping.getToppingName();
                                extraPrice = topping.getPrice();
                            } else {
                                SaladIngredient ingredient = saladIngredientRepository.findById(custReq.getId()).orElse(null);
                                if (ingredient != null) {
                                    extraName = ingredient.getIngredientName();
                                    extraPrice = ingredient.getPrice();
                                }
                            }
                        }
                        if (extraName.isEmpty()) {
                            throw new java.util.NoSuchElementException("Customization not found with ID: " + custReq.getId());
                        }
                        extra.setName(extraName);
                        extra.setQty(custReq.getQuantity());
                        extra.setUnitPriceAtTime(extraPrice);
                        orderItem.addExtra(extra);
                    }
                }
                order.addMenuItem(orderItem);
            }
        }

        return toDto(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getOrdersByBranch(String branchName) {
        if (branchName == null || branchName.isBlank()) {
            throw new IllegalArgumentException("Branch name is required.");
        }
        Branch branch = branchRepository.findByName(branchName)
                .orElseThrow(() -> new java.util.NoSuchElementException("Branch not found: " + branchName));
        return orderRepository.findByBranchId(branch.getId()).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getPendingOrdersByBranch(String branchName) {
        if (branchName == null || branchName.isBlank()) {
            throw new IllegalArgumentException("Branch name is required.");
        }
        Branch branch = branchRepository.findByName(branchName)
                .orElseThrow(() -> new java.util.NoSuchElementException("Branch not found: " + branchName));
        return orderRepository.findByBranchIdAndStatus(branch.getId(), "Pending").stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public OrderResponseDTO updateOrderStatus(Long orderId, String newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Order not found with ID: " + orderId));
        order.setStatus(newStatus);
        return toDto(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    private void validateOrderRequest(OrderRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Order request is required.");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must include at least one item.");
        }
        for (OrderRequestDTO.OrderItemRequestDTO item : request.getItems()) {
            if (item == null) {
                throw new IllegalArgumentException("Order items cannot be null.");
            }
            boolean hasMenuItem = item.getMenuItemId() != null;
            boolean hasPizza = item.getPizzaId() != null;
            if (hasMenuItem == hasPizza) {
                throw new IllegalArgumentException("Each item must include exactly one of menuItemId or pizzaId.");
            }
        }
    }

    private OrderResponseDTO toDto(Order order) {
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

        List<OrderResponseDTO.MenuItemDTO> menuItems = new ArrayList<>();
        if (order.getMenuItems() != null) {
            for (OrderMenuItem item : order.getMenuItems()) {
                OrderResponseDTO.MenuItemDTO itemDto = new OrderResponseDTO.MenuItemDTO();
                itemDto.setId(item.getId());
                if (item.getMenuItem() != null) {
                    itemDto.setMenuItemId(item.getMenuItem().getId());
                    itemDto.setMenuItemName(item.getMenuItem().getName());
                }
                itemDto.setQty(item.getQty());
                itemDto.setUnitPriceAtTime(item.getUnitPriceAtTime());
                itemDto.setNotes(item.getNotes());

                List<OrderResponseDTO.MenuItemExtraDTO> extraDtos = new ArrayList<>();
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
                itemDto.setExtras(extraDtos);
                menuItems.add(itemDto);
            }
        }
        dto.setMenuItems(menuItems);

        List<OrderResponseDTO.PizzaItemDTO> pizzaItems = new ArrayList<>();
        if (order.getPizzaItems() != null) {
            for (OrderPizzaItem item : order.getPizzaItems()) {
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

                List<OrderResponseDTO.PizzaItemExtraDTO> extraDtos = new ArrayList<>();
                if (item.getExtras() != null) {
                    for (OrderPizzaItemExtra extra : item.getExtras()) {
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
                }
                itemDto.setExtras(extraDtos);
                pizzaItems.add(itemDto);
            }
        }
        dto.setPizzaItems(pizzaItems);

        return dto;
    }
}
