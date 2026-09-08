package org.example.onlinepossystem.ordering.service;

import org.example.onlinepossystem.branch.api.BranchLookup;
import org.example.onlinepossystem.branch.entity.Branch;
import org.example.onlinepossystem.catalog.api.OrderCatalogResolver;
import org.example.onlinepossystem.catalog.dto.MenuDTO;
import org.example.onlinepossystem.customer.api.CustomerAccountReader;
import org.example.onlinepossystem.customer.entity.Customer;
import org.example.onlinepossystem.ordering.api.CustomerOrderHistoryReader;
import org.example.onlinepossystem.ordering.api.OrderOperations;
import org.example.onlinepossystem.ordering.dto.OrderRequestDTO;
import org.example.onlinepossystem.ordering.dto.OrderResponseDTO;
import org.example.onlinepossystem.ordering.entity.Order;
import org.example.onlinepossystem.ordering.entity.OrderBurgerExtraComponent;
import org.example.onlinepossystem.ordering.entity.OrderBurgerProtein;
import org.example.onlinepossystem.ordering.entity.OrderBurgerRemovedComponent;
import org.example.onlinepossystem.ordering.entity.OrderMenuItem;
import org.example.onlinepossystem.ordering.entity.OrderMenuItemExtra;
import org.example.onlinepossystem.ordering.entity.OrderPizzaItem;
import org.example.onlinepossystem.ordering.entity.OrderPizzaItemExtra;
import org.example.onlinepossystem.ordering.event.OrderCreatedEvent;
import org.example.onlinepossystem.ordering.repository.OrderRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService implements OrderOperations, CustomerOrderHistoryReader {
    private final OrderRepository orderRepository;
    private final BranchLookup branchLookup;
    private final CustomerAccountReader customerAccountReader;
    private final OrderCatalogResolver catalogResolver;
    private final OrderRequestValidator orderRequestValidator;
    private final OrderResponseMapper orderResponseMapper;
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(OrderRepository orderRepository,
                        BranchLookup branchLookup,
                        CustomerAccountReader customerAccountReader,
                        OrderCatalogResolver catalogResolver,
                        OrderRequestValidator orderRequestValidator,
                        OrderResponseMapper orderResponseMapper,
                        ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.branchLookup = branchLookup;
        this.customerAccountReader = customerAccountReader;
        this.catalogResolver = catalogResolver;
        this.orderRequestValidator = orderRequestValidator;
        this.orderResponseMapper = orderResponseMapper;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<MenuDTO> getMenuForBranch(String branchName) {
        return catalogResolver.getMenuForBranch(branchName);
    }

    @Override
    @Transactional
    public OrderResponseDTO placeOrder(OrderRequestDTO request) {
        return placeOrderForCustomer(request, null);
    }

    @Override
    @Transactional
    public OrderResponseDTO placeOrderForCustomer(OrderRequestDTO request, String customerEmail) {
        orderRequestValidator.validate(request);
        Branch branch = branchLookup.requireByName(request.getBranchName());
        Customer customer = resolveCustomer(customerEmail);
        LocalDateTime createdAt = LocalDateTime.now();

        Order order = new Order();
        order.setBranch(branch);
        order.setCustomer(customer);
        order.setCustomerName(request.getCustomerName());
        order.setPhone(request.getPhone());
        order.setHouseNumber(request.getHouseNumber());
        order.setStreet(request.getStreet());
        order.setArea(request.getArea());
        order.setCity(request.getCity());
        order.setPostalCode(request.getPostalCode());
        order.setComplexName(request.getComplexName());
        order.setOrderType(request.getOrderType() != null ? request.getOrderType() : "pickup");
        order.setCreatedAt(createdAt);
        order.setStatus("Pending");
        if (customer != null) {
            customer.setLastOrderedAt(createdAt);
        }

        for (OrderRequestDTO.OrderItemRequestDTO itemRequest : request.getItems()) {
            if (itemRequest.getPizzaId() != null) {
                order.addPizzaItem(buildPizzaItem(branch.getId(), itemRequest));
            } else {
                order.addMenuItem(buildMenuItem(branch.getId(), itemRequest));
            }
        }

        Order savedOrder = orderRepository.save(order);
        OrderResponseDTO response = orderResponseMapper.toDto(savedOrder);
        eventPublisher.publishEvent(new OrderCreatedEvent(response));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getRecentOrdersForCustomer(String customerEmail, int limit) {
        if (customerEmail == null || customerEmail.isBlank() || limit < 1) {
            return List.of();
        }

        return customerAccountReader.findByEmail(customerEmail)
                .map(customer -> orderRepository
                        .findByCustomerIdOrderByCreatedAtDesc(customer.getId(), PageRequest.of(0, limit))
                        .stream()
                        .map(orderResponseMapper::toDto)
                        .toList())
                .orElseGet(List::of);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getOrdersByBranch(String branchName) {
        if (branchName == null || branchName.isBlank()) {
            throw new IllegalArgumentException("Branch name is required.");
        }
        Branch branch = branchLookup.requireByName(branchName);
        return orderRepository.findByBranchIdOrderByCreatedAtDesc(branch.getId()).stream()
                .map(orderResponseMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getPendingOrdersByBranch(String branchName) {
        if (branchName == null || branchName.isBlank()) {
            throw new IllegalArgumentException("Branch name is required.");
        }
        Branch branch = branchLookup.requireByName(branchName);
        return orderRepository.findByBranchIdAndStatusOrderByCreatedAtDesc(branch.getId(), "Pending").stream()
                .map(orderResponseMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public OrderResponseDTO updateOrderStatus(Long orderId, String newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Order not found with ID: " + orderId));
        order.setStatus(newStatus);
        return orderResponseMapper.toDto(orderRepository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(orderResponseMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Order not found with ID: " + orderId));
        return orderResponseMapper.toDto(order);
    }

    private OrderPizzaItem buildPizzaItem(Integer branchId, OrderRequestDTO.OrderItemRequestDTO itemRequest) {
        OrderCatalogResolver.ResolvedPizzaItem resolvedItem = catalogResolver.resolvePizzaItem(
                branchId,
                itemRequest.getPizzaId(),
                itemRequest.getPizzaSizeId(),
                itemRequest.getSizeCm(),
                toCatalogCustomizations(itemRequest.getCustomizations())
        );

        OrderPizzaItem pizzaItem = new OrderPizzaItem();
        pizzaItem.setPizza(resolvedItem.pizza());
        pizzaItem.setPizzaSize(resolvedItem.pizzaSize());
        pizzaItem.setQty(itemRequest.getQuantity());
        pizzaItem.setBasePriceAtTime(resolvedItem.basePrice());
        pizzaItem.setNotes(itemRequest.getNotes());

        for (OrderCatalogResolver.ResolvedPizzaExtra resolvedExtra : resolvedItem.extras()) {
            OrderPizzaItemExtra extra = new OrderPizzaItemExtra();
            extra.setIngredient(resolvedExtra.ingredient());
            extra.setQty(resolvedExtra.quantity());
            extra.setUnitPriceAtTime(resolvedExtra.unitPrice());
            pizzaItem.addExtra(extra);
        }

        return pizzaItem;
    }

    private Customer resolveCustomer(String customerEmail) {
        if (customerEmail == null || customerEmail.isBlank()) {
            return null;
        }
        return customerAccountReader.findByEmail(customerEmail).orElse(null);
    }

    private OrderMenuItem buildMenuItem(Integer branchId, OrderRequestDTO.OrderItemRequestDTO itemRequest) {
        OrderCatalogResolver.ResolvedMenuItem resolvedItem = catalogResolver.resolveMenuItem(
                branchId,
                itemRequest.getMenuItemId(),
                toCatalogCustomizations(itemRequest.getCustomizations())
        );

        OrderMenuItem orderItem = new OrderMenuItem();
        orderItem.setMenuItem(resolvedItem.menuItem());
        orderItem.setQty(itemRequest.getQuantity());
        orderItem.setUnitPriceAtTime(resolvedItem.unitPrice());
        orderItem.setNotes(itemRequest.getNotes());

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
        if (burgerSelection == null || burgerSelection.protein() == null) {
            return;
        }

        OrderCatalogResolver.ResolvedBurgerProtein resolvedProtein = burgerSelection.protein();
        OrderBurgerProtein protein = new OrderBurgerProtein();
        protein.setComponent(resolvedProtein.component());
        protein.setProteinQtyPerBurger(resolvedProtein.quantity());
        protein.setUnitPriceAtTime(resolvedProtein.unitPrice());
        orderItem.setBurgerProtein(protein);

        for (OrderCatalogResolver.ResolvedBurgerComponent resolvedComponent : burgerSelection.removedComponents()) {
            OrderBurgerRemovedComponent removedComponent = new OrderBurgerRemovedComponent();
            removedComponent.setComponent(resolvedComponent.component());
            orderItem.addRemovedBurgerComponent(removedComponent);
        }

        for (OrderCatalogResolver.ResolvedBurgerComponent resolvedComponent : burgerSelection.extraComponents()) {
            OrderBurgerExtraComponent extraComponent = new OrderBurgerExtraComponent();
            extraComponent.setComponent(resolvedComponent.component());
            extraComponent.setQty(resolvedComponent.quantity());
            extraComponent.setUnitPriceAtTime(resolvedComponent.unitPrice());
            orderItem.addExtraBurgerComponent(extraComponent);
        }
    }

    private List<OrderCatalogResolver.CatalogCustomizationRequest> toCatalogCustomizations(
            List<OrderRequestDTO.CustomizationRequestDTO> customizations
    ) {
        if (customizations == null) {
            return List.of();
        }
        return customizations.stream()
                .filter(customization -> customization != null)
                .map(customization -> new OrderCatalogResolver.CatalogCustomizationRequest(
                        customization.getId(),
                        customization.getQuantity(),
                        customization.getType()
                ))
                .toList();
    }
}
