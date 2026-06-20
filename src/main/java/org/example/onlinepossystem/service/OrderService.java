package org.example.onlinepossystem.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.onlinepossystem.dto.MenuDTO;
import org.example.onlinepossystem.dto.OrderRequestDTO;
import org.example.onlinepossystem.dto.OrderResponseDTO;
import org.example.onlinepossystem.entity.*;
import org.example.onlinepossystem.event.OrderCreatedEvent;
import org.example.onlinepossystem.menu.dto.BurgerComponentRow;
import org.example.onlinepossystem.menu.repository.BurgerComponentReadRepository;
import org.example.onlinepossystem.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    private BurgerComponentReadRepository burgerComponentReadRepository;

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

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @PersistenceContext
    private EntityManager entityManager;

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
        List<Integer> menuItemIds = items.stream().map(MenuItem::getId).toList();
        Map<Integer, List<BurgerComponentRow>> burgerComponentsByMenuItem = burgerComponentReadRepository
                .findComponentsForMenuItems(branch.getId(), menuItemIds)
                .stream()
                .collect(Collectors.groupingBy(
                        BurgerComponentRow::burgerId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

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
                for (BurgerComponentRow component : burgerComponentsByMenuItem.getOrDefault(i.getId(), List.of())) {
                    MenuDTO.CustomizationDTO c = new MenuDTO.CustomizationDTO();
                    c.setId(component.componentId());
                    c.setName(component.name());
                    c.setPrice(toDouble(component.price()));
                    c.setDefault(Boolean.TRUE.equals(component.defaultSelected()));
                    c.setType(component.componentType());
                    c.setProteinQuantityRequired(component.proteinQuantityRequired());
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

                applyMenuItemCustomizations(branch, menuItem, orderItem, itemRequest.getCustomizations());
                order.addMenuItem(orderItem);
            }
        }

        Order savedOrder = orderRepository.save(order);
        OrderResponseDTO response = toDto(savedOrder);
        eventPublisher.publishEvent(new OrderCreatedEvent(response));
        return response;
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getOrdersByBranch(String branchName) {
        if (branchName == null || branchName.isBlank()) {
            throw new IllegalArgumentException("Branch name is required.");
        }
        Branch branch = branchRepository.findByName(branchName)
                .orElseThrow(() -> new java.util.NoSuchElementException("Branch not found: " + branchName));
        return orderRepository.findByBranchIdOrderByCreatedAtDesc(branch.getId()).stream()
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
        return orderRepository.findByBranchIdAndStatusOrderByCreatedAtDesc(branch.getId(), "Pending").stream()
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
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Order not found with ID: " + orderId));
        return toDto(order);
    }

    private void applyMenuItemCustomizations(
            Branch branch,
            MenuItem menuItem,
            OrderMenuItem orderItem,
            List<OrderRequestDTO.CustomizationRequestDTO> customizations
    ) {
        List<OrderRequestDTO.CustomizationRequestDTO> safeCustomizations =
                customizations == null ? List.of() : customizations;
        Optional<BurgerComponentReadRepository.BurgerConfig> burgerConfig =
                burgerComponentReadRepository.findBurgerConfig(menuItem.getId());

        Set<Integer> burgerComponentIds = Set.of();
        if (burgerConfig.isPresent()) {
            burgerComponentIds = applyBurgerComponents(
                    branch.getId(),
                    menuItem,
                    orderItem,
                    safeCustomizations,
                    burgerConfig.get()
            );
        }

        applyGenericMenuItemExtras(branch, orderItem, safeCustomizations, burgerComponentIds);
    }

    private Set<Integer> applyBurgerComponents(
            Integer branchId,
            MenuItem menuItem,
            OrderMenuItem orderItem,
            List<OrderRequestDTO.CustomizationRequestDTO> customizations,
            BurgerComponentReadRepository.BurgerConfig burgerConfig
    ) {
        List<BurgerComponentRow> components = burgerComponentReadRepository
                .findComponentsForMenuItems(branchId, List.of(menuItem.getId()));
        if (components.isEmpty()) {
            throw new java.util.NoSuchElementException("Burger components not found for menu item: " + menuItem.getName());
        }

        Map<Integer, List<BurgerComponentRow>> componentsById = components.stream()
                .collect(Collectors.groupingBy(
                        BurgerComponentRow::componentId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        Set<Integer> selectedComponentIds = new LinkedHashSet<>();
        Map<Integer, Integer> selectedQuantities = new HashMap<>();
        Map<Integer, Integer> selectedExtraQuantities = new LinkedHashMap<>();
        for (OrderRequestDTO.CustomizationRequestDTO customization : customizations) {
            if (customization == null) {
                continue;
            }
            if (isBurgerExtraComponentCustomization(customization, componentsById.keySet())) {
                Integer componentId = customization.getId();
                selectedExtraQuantities.merge(componentId, safeQuantity(customization.getQuantity()), Integer::sum);
                continue;
            }
            if (isBurgerComponentCustomization(customization, componentsById.keySet())) {
                Integer componentId = customization.getId();
                selectedComponentIds.add(componentId);
                selectedQuantities.merge(componentId, safeQuantity(customization.getQuantity()), Integer::sum);
            }
        }

        List<BurgerComponentRow> selectedProteins = selectedComponentIds.stream()
                .map(componentId -> findBurgerComponent(componentsById, componentId, "protein").orElse(null))
                .filter(component -> component != null)
                .toList();
        if (selectedProteins.size() != 1) {
            throw new IllegalArgumentException(menuItem.getName() + " requires exactly one protein choice.");
        }

        BurgerComponentRow protein = selectedProteins.get(0);
        OrderBurgerProtein burgerProtein = new OrderBurgerProtein();
        burgerProtein.setComponent(burgerComponentReference(protein.componentId()));
        burgerProtein.setProteinQtyPerBurger(burgerConfig.proteinQuantityRequired());
        burgerProtein.setUnitPriceAtTime(toMoney(protein.price()));
        orderItem.setBurgerProtein(burgerProtein);

        for (BurgerComponentRow component : components) {
            if (!Boolean.TRUE.equals(component.defaultSelected())) {
                continue;
            }
            if (selectedComponentIds.contains(component.componentId())) {
                continue;
            }
            if (!Boolean.TRUE.equals(component.removable())) {
                throw new IllegalArgumentException(component.name() + " cannot be removed from " + menuItem.getName() + ".");
            }

            OrderBurgerRemovedComponent removedComponent = new OrderBurgerRemovedComponent();
            removedComponent.setComponent(burgerComponentReference(component.componentId()));
            orderItem.addRemovedBurgerComponent(removedComponent);
        }

        for (Integer selectedComponentId : selectedComponentIds) {
            if (!componentsById.containsKey(selectedComponentId)) {
                throw new java.util.NoSuchElementException("Burger component not found with ID: " + selectedComponentId);
            }
            boolean defaultForBurger = componentsById.getOrDefault(selectedComponentId, List.of()).stream()
                    .anyMatch(component -> Boolean.TRUE.equals(component.defaultSelected()));
            if (defaultForBurger) {
                continue;
            }
            findBurgerExtraComponent(componentsById, selectedComponentId).ifPresent(component ->
                    selectedExtraQuantities.merge(
                            selectedComponentId,
                            selectedQuantities.getOrDefault(selectedComponentId, 1),
                            Integer::sum
                    )
            );
        }

        for (Map.Entry<Integer, Integer> selectedExtra : selectedExtraQuantities.entrySet()) {
            BurgerComponentRow component = findBurgerExtraComponent(componentsById, selectedExtra.getKey())
                    .orElseThrow(() -> new java.util.NoSuchElementException(
                            "Burger extra component not found with ID: " + selectedExtra.getKey()
                    ));

            OrderBurgerExtraComponent extraComponent = new OrderBurgerExtraComponent();
            extraComponent.setComponent(burgerComponentReference(component.componentId()));
            extraComponent.setQty(selectedExtra.getValue());
            extraComponent.setUnitPriceAtTime(toMoney(component.price()));
            orderItem.addExtraBurgerComponent(extraComponent);
        }

        return componentsById.keySet();
    }

    private Optional<BurgerComponentRow> findBurgerComponent(
            Map<Integer, List<BurgerComponentRow>> componentsById,
            Integer componentId,
            String componentType
    ) {
        return componentsById.getOrDefault(componentId, List.of()).stream()
                .filter(component -> componentType.equals(component.componentType()))
                .findFirst();
    }

    private Optional<BurgerComponentRow> findBurgerExtraComponent(
            Map<Integer, List<BurgerComponentRow>> componentsById,
            Integer componentId
    ) {
        return componentsById.getOrDefault(componentId, List.of()).stream()
                .filter(component -> "extra_topping".equals(component.componentType()))
                .filter(component -> !Boolean.TRUE.equals(component.defaultSelected()))
                .findFirst();
    }

    private void applyGenericMenuItemExtras(
            Branch branch,
            OrderMenuItem orderItem,
            List<OrderRequestDTO.CustomizationRequestDTO> customizations,
            Set<Integer> burgerComponentIds
    ) {
        for (OrderRequestDTO.CustomizationRequestDTO customization : customizations) {
            if (customization == null) {
                continue;
            }
            if (isBurgerComponentCustomization(customization, burgerComponentIds)) {
                continue;
            }

            OrderMenuItemExtra extra = resolveGenericMenuItemExtra(branch, customization);
            orderItem.addExtra(extra);
        }
    }

    private OrderMenuItemExtra resolveGenericMenuItemExtra(
            Branch branch,
            OrderRequestDTO.CustomizationRequestDTO customization
    ) {
        String customizationType = normalizedType(customization);
        if (customizationType.isBlank() || "modifieroption".equals(customizationType)) {
            Optional<ModifierOption> modifierOption = modifierOptionRepository.findById(customization.getId());
            if (modifierOption.isPresent()) {
                ModifierOption option = modifierOption.get();
                double extraPrice = 0.0;
                if (option.menuItemId != null) {
                    BranchMenuItemPrice extraPriceEntry = branchMenuItemPriceRepository
                            .findByBranchIdAndMenuItemId(branch.getId(), option.menuItemId)
                            .orElseThrow(() -> new java.util.NoSuchElementException(
                                    "Price not found for modifier option menu item ID: " + option.menuItemId
                            ));
                    extraPrice = extraPriceEntry.getPrice();
                }
                return genericMenuItemExtra(option.name, safeQuantity(customization.getQuantity()), extraPrice);
            }
            if ("modifieroption".equals(customizationType)) {
                throw new java.util.NoSuchElementException("Modifier option not found with ID: " + customization.getId());
            }
        }

        if (customizationType.isBlank() || "saladingredient".equals(customizationType)) {
            Optional<SaladIngredient> ingredient = saladIngredientRepository.findById(customization.getId());
            if (ingredient.isPresent()) {
                SaladIngredient saladIngredient = ingredient.get();
                return genericMenuItemExtra(
                        saladIngredient.getIngredientName(),
                        safeQuantity(customization.getQuantity()),
                        saladIngredient.getPrice()
                );
            }
            if ("saladingredient".equals(customizationType)) {
                throw new java.util.NoSuchElementException("Salad ingredient not found with ID: " + customization.getId());
            }
        }

        throw new java.util.NoSuchElementException("Customization not found with ID: " + customization.getId());
    }

    private OrderMenuItemExtra genericMenuItemExtra(String name, Integer quantity, Double price) {
        OrderMenuItemExtra extra = new OrderMenuItemExtra();
        extra.setName(name);
        extra.setQty(quantity);
        extra.setUnitPriceAtTime(price == null ? 0.0 : price);
        return extra;
    }

    private boolean isBurgerComponentCustomization(
            OrderRequestDTO.CustomizationRequestDTO customization,
            Set<Integer> knownComponentIds
    ) {
        if (isBurgerExtraComponentCustomization(customization, knownComponentIds)) {
            return true;
        }
        String customizationType = normalizedType(customization);
        return "burgertopping".equals(customizationType)
                || "burgercomponent".equals(customizationType)
                || (customizationType.isBlank()
                && customization != null
                && knownComponentIds.contains(customization.getId()));
    }

    private boolean isBurgerExtraComponentCustomization(
            OrderRequestDTO.CustomizationRequestDTO customization,
            Set<Integer> knownComponentIds
    ) {
        String customizationType = normalizedType(customization);
        return "burgerextra".equals(customizationType)
                || "burgerextratopping".equals(customizationType)
                || "burgerextracomponent".equals(customizationType)
                || ("burgercomponentextra".equals(customizationType)
                && customization != null
                && knownComponentIds.contains(customization.getId()));
    }

    private String normalizedType(OrderRequestDTO.CustomizationRequestDTO customization) {
        if (customization == null || customization.getType() == null) {
            return "";
        }
        return customization.getType().trim().replace("_", "").replace("-", "").toLowerCase();
    }

    private Integer safeQuantity(Integer quantity) {
        return quantity == null || quantity < 1 ? 1 : quantity;
    }

    private Double toDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    private Double toDouble(BigDecimal value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    private BigDecimal toMoney(Double value) {
        return value == null ? BigDecimal.ZERO : BigDecimal.valueOf(value);
    }

    private BurgerComponent burgerComponentReference(Integer componentId) {
        return entityManager.getReference(BurgerComponent.class, componentId);
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
