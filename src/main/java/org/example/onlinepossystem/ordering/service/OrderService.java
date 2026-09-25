package org.example.onlinepossystem.ordering.service;

import org.example.onlinepossystem.branch.api.BranchLookup;
import org.example.onlinepossystem.branch.api.BranchView;
import org.example.onlinepossystem.customer.api.CustomerAccount;
import org.example.onlinepossystem.customer.api.CustomerAccountReader;
import org.example.onlinepossystem.customer.api.CustomerOrderRecorder;
import org.example.onlinepossystem.ordering.api.CustomerOrderHistoryReader;
import org.example.onlinepossystem.ordering.api.CustomerOrderSummary;
import org.example.onlinepossystem.ordering.api.OrderEventPublisher;
import org.example.onlinepossystem.ordering.api.OrderOperations;
import org.example.onlinepossystem.ordering.dto.OrderRequestDTO;
import org.example.onlinepossystem.ordering.dto.OrderResponseDTO;
import org.example.onlinepossystem.ordering.entity.Order;
import org.example.onlinepossystem.ordering.repository.OrderRepository;
import org.example.onlinepossystem.special.service.SpecialService;
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
    private final CustomerOrderRecorder customerOrderRecorder;
    private final OrderRequestValidator orderRequestValidator;
    private final MenuOrderItemFactory menuOrderItemFactory;
    private final PizzaOrderItemFactory pizzaOrderItemFactory;
    private final SpecialService specialService;
    private final OrderStatusPolicy orderStatusPolicy;
    private final OrderResponseMapper orderResponseMapper;
    private final CustomerOrderSummaryMapper customerOrderSummaryMapper;
    private final OrderEventPublisher eventPublisher;

    public OrderService(OrderRepository orderRepository,
                        BranchLookup branchLookup,
                        CustomerAccountReader customerAccountReader,
                        CustomerOrderRecorder customerOrderRecorder,
                        OrderRequestValidator orderRequestValidator,
                        MenuOrderItemFactory menuOrderItemFactory,
                        PizzaOrderItemFactory pizzaOrderItemFactory,
                        SpecialService specialService,
                        OrderStatusPolicy orderStatusPolicy,
                        OrderResponseMapper orderResponseMapper,
                        CustomerOrderSummaryMapper customerOrderSummaryMapper,
                        OrderEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.branchLookup = branchLookup;
        this.customerAccountReader = customerAccountReader;
        this.customerOrderRecorder = customerOrderRecorder;
        this.orderRequestValidator = orderRequestValidator;
        this.menuOrderItemFactory = menuOrderItemFactory;
        this.pizzaOrderItemFactory = pizzaOrderItemFactory;
        this.specialService = specialService;
        this.orderStatusPolicy = orderStatusPolicy;
        this.orderResponseMapper = orderResponseMapper;
        this.customerOrderSummaryMapper = customerOrderSummaryMapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public OrderResponseDTO placeOrder(OrderRequestDTO request) {
        return placeOrderForCustomer(request, null);
    }

    @Override
    @Transactional
    public OrderResponseDTO placeOrderForCustomer(OrderRequestDTO request, String customerEmail) {
        CustomerAccount customer = resolveCustomer(customerEmail);
        return saveOrder(request, customer);
    }

    @Override
    @Transactional
    public OrderResponseDTO placeOrderForCustomerId(OrderRequestDTO request, Long customerId) {
        CustomerAccount customer = customerAccountReader.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer account is missing."));
        return saveOrder(request, customer);
    }

    private OrderResponseDTO saveOrder(OrderRequestDTO request, CustomerAccount customer) {
        orderRequestValidator.validate(request);
        BranchView branch = branchLookup.requireByName(request.getBranchName());
        LocalDateTime createdAt = LocalDateTime.now();

        Order order = new Order();
        order.setBranchId(branch.id());
        order.setCustomerId(customer == null ? null : customer.id());
        order.setCustomerName(request.getCustomerName());
        order.setPhone(request.getPhone());
        order.setHouseNumber(request.getHouseNumber());
        order.setStreet(request.getStreet());
        order.setArea(request.getArea());
        order.setCity(request.getCity());
        order.setPostalCode(request.getPostalCode());
        order.setComplexName(request.getComplexName());
        String orderType = request.getOrderType() != null ? request.getOrderType() : "pickup";
        order.setOrderType(orderType);
        order.setGateAccessCode("delivery".equalsIgnoreCase(orderType)
                ? trimToNull(request.getGateAccessCode()) : null);
        order.setCreatedAt(createdAt);
        order.setStatus("Pending");
        if (customer != null) {
            customerOrderRecorder.recordOrderPlaced(customer.id(), createdAt);
        }

        for (OrderRequestDTO.OrderItemRequestDTO itemRequest : safe(request.getItems())) {
            if (itemRequest.getPizzaId() != null) {
                order.addPizzaItem(pizzaOrderItemFactory.create(branch.id(), itemRequest));
            } else {
                order.addMenuItem(menuOrderItemFactory.create(branch.id(), itemRequest));
            }
        }
        for (OrderRequestDTO.SpecialItemRequestDTO specialRequest : safe(request.getSpecialItems())) {
            specialService.addToOrder(order, branch.id(), specialRequest);
        }

        Order savedOrder = orderRepository.save(order);
        OrderResponseDTO response = orderResponseMapper.toDto(savedOrder);
        eventPublisher.orderCreated(response, customerUsername(savedOrder));
        return response;
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerOrderSummary> getRecentOrdersForCustomer(String customerEmail, int limit) {
        if (customerEmail == null || customerEmail.isBlank() || limit < 1) {
            return List.of();
        }

        return customerAccountReader.findByEmail(customerEmail)
                .map(customer -> recentOrders(customer.id(), limit))
                .orElseGet(List::of);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerOrderSummary> getRecentOrdersForCustomerId(Long customerId, int limit) {
        if (customerId == null || customerId < 1 || limit < 1) return List.of();
        return recentOrders(customerId, limit);
    }

    private List<CustomerOrderSummary> recentOrders(Long customerId, int limit) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, PageRequest.of(0, limit))
                .stream().map(orderResponseMapper::toDto).map(customerOrderSummaryMapper::toSummary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getOrdersByBranch(String branchName) {
        if (branchName == null || branchName.isBlank()) {
            throw new IllegalArgumentException("Branch name is required.");
        }
        BranchView branch = branchLookup.requireByName(branchName);
        return orderRepository.findByBranchIdOrderByCreatedAtDesc(branch.id()).stream()
                .map(orderResponseMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getPendingOrdersByBranch(String branchName) {
        if (branchName == null || branchName.isBlank()) {
            throw new IllegalArgumentException("Branch name is required.");
        }
        BranchView branch = branchLookup.requireByName(branchName);
        return orderRepository.findByBranchIdAndStatusOrderByCreatedAtDesc(branch.id(), "Pending").stream()
                .map(orderResponseMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public OrderResponseDTO updateOrderStatus(Long orderId, String newStatus) {
        String validatedStatus = orderStatusPolicy.requireValid(newStatus);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Order not found with ID: " + orderId));
        order.setStatus(validatedStatus);
        Order savedOrder = orderRepository.save(order);
        OrderResponseDTO response = orderResponseMapper.toDto(savedOrder);
        eventPublisher.orderStatusChanged(response, customerUsername(savedOrder));
        return response;
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

    private CustomerAccount resolveCustomer(String customerEmail) {
        if (customerEmail == null || customerEmail.isBlank()) {
            return null;
        }
        return customerAccountReader.findByEmail(customerEmail).orElse(null);
    }

    private String customerUsername(Order order) {
        if (order == null || order.getCustomerId() == null) {
            return null;
        }
        return customerAccountReader.findById(order.getCustomerId())
                .map(CustomerAccount::email)
                .orElse(null);
    }

}
