package org.example.onlinepossystem.service;

import org.example.onlinepossystem.dto.MenuDTO;
import org.example.onlinepossystem.dto.OrderRequestDTO;
import org.example.onlinepossystem.entity.*;
import org.example.onlinepossystem.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private PizzaSizeRepository pizzaSizeRepository;

    @Autowired
    private PizzaAllowedSizeRepository pizzaAllowedSizeRepository;

    @Autowired
    private PriceCategoryRepository priceCategoryRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private PizzaDefaultIngredientRepository pizzaDefaultIngredientRepository;

    @Autowired
    private BranchExtraPriceRepository branchExtraPriceRepository;

    public List<MenuDTO> getMenuForBranch(String branchName) {
        Optional<Branch> branchOpt = branchRepository.findByName(branchName);
        if (branchOpt.isEmpty()) {
            return new ArrayList<>();
        }
        Branch branch = branchOpt.get();

        // 1. Get pizza prices for this branch
        List<BranchMenuItemPrice> prices = branchMenuItemPriceRepository.findByBranchId(branch.getId());
        Map<Integer, List<MenuDTO.SizePriceDTO>> pizzaPrices = prices.stream()
                .collect(Collectors.groupingBy(p -> p.getMenuItem().getId(),
                        Collectors.mapping(p -> {
                            MenuDTO.SizePriceDTO s = new MenuDTO.SizePriceDTO();
                            s.setSizeId(p.getPizzaSize().getId());
                            s.setCm(p.getPizzaSize().getCm());
                            s.setPrice(p.getPrice());
                            return s;
                        }, Collectors.toList())));

        // 2. Get extra prices for this branch
        List<BranchExtraPrice> extraPrices = branchExtraPriceRepository.findByBranchId(branch.getId());
        Map<Integer, List<MenuDTO.SizePriceDTO>> categoryExtraPrices = extraPrices.stream()
                .collect(Collectors.groupingBy(p -> p.getPriceCategory().getId(),
                        Collectors.mapping(p -> {
                            MenuDTO.SizePriceDTO s = new MenuDTO.SizePriceDTO();
                            s.setSizeId(p.getPizzaSize().getId());
                            s.setCm(p.getPizzaSize().getCm());
                            s.setPrice(p.getPrice());
                            return s;
                        }, Collectors.toList())));

        // 3. Get all active pizzas
        List<MenuItem> pizzas = menuItemRepository.findAllByActiveTrue();
        List<Ingredient> ingredients = ingredientRepository.findAllByActiveTrue();

        return pizzas.stream()
                .filter(p -> pizzaPrices.containsKey(p.getId()))
                .map(p -> {
                    MenuDTO dto = new MenuDTO();
                    dto.setCategoryName(p.getCategory() != null ? p.getCategory().getName() : "Uncategorized");
                    dto.setMenuItemId(Long.valueOf(p.getId()));
                    dto.setMenuItemName(p.getName());
                    dto.setDescription(p.getDescription());
                    dto.setAvailableSizes(pizzaPrices.get(p.getId()));
                    
                    dto.setAvailableToppings(ingredients.stream().map(i -> {
                        MenuDTO.ToppingDTO t = new MenuDTO.ToppingDTO();
                        t.setId(i.getId());
                        t.setName(i.getName());
                        List<MenuDTO.SizePriceDTO> extras = categoryExtraPrices.get(i.getPriceCategory().getId());
                        t.setExtraPrices(extras != null ? extras : new ArrayList<>());
                        return t;
                    }).collect(Collectors.toList()));

                    return dto;
                }).collect(Collectors.toList());
    }

    @Transactional
    public Order placeOrder(OrderRequestDTO request) {
        Branch branch = branchRepository.findByName(request.getBranchName())
                .orElseThrow(() -> new RuntimeException("Branch not found"));

        Order order = new Order();
        order.setBranch(branch);
        order.setCustomerName(request.getCustomerName());
        order.setPhone(request.getPhone());
        order.setOrderType(request.getOrderType() != null ? request.getOrderType() : "pickup");
        order.setStatus("created");
        order.setCreatedAt(LocalDateTime.now());

        for (OrderRequestDTO.OrderItemRequestDTO itemRequest : request.getItems()) {
            MenuItem pizza = menuItemRepository.findById(itemRequest.getPizzaId())
                    .orElseThrow(() -> new RuntimeException("Pizza not found"));
            PizzaSize size = pizzaSizeRepository.findById(itemRequest.getPizzaSizeId())
                    .orElseThrow(() -> new RuntimeException("Size not found"));

            BranchMenuItemPrice branchPrice = branchMenuItemPriceRepository
                    .findByBranchIdAndMenuItemIdAndPizzaSizeId(branch.getId(), pizza.getId(), size.getId())
                    .orElseThrow(() -> new RuntimeException("Price not found for branch"));

            OrderItem orderItem = new OrderItem();
            orderItem.setMenuItem(pizza);
            orderItem.setPizzaSize(size);
            orderItem.setQty(itemRequest.getQuantity());
            orderItem.setPrice(branchPrice.getPrice());
            orderItem.setNotes(itemRequest.getNotes());

            if (itemRequest.getExtras() != null) {
                for (OrderRequestDTO.ExtraRequestDTO extraReq : itemRequest.getExtras()) {
                    Ingredient ing = ingredientRepository.findById(extraReq.getIngredientId())
                            .orElseThrow(() -> new RuntimeException("Ingredient not found"));
                    
                    BranchExtraPrice extraPrice = branchExtraPriceRepository.findAll().stream()
                            .filter(ep -> ep.getBranch().getId().equals(branch.getId()) &&
                                          ep.getPriceCategory().getId().equals(ing.getPriceCategory().getId()) &&
                                          ep.getPizzaSize().getId().equals(size.getId()))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("Extra price not found"));

                    OrderItemTopping extra = new OrderItemTopping();
                    extra.setIngredient(ing);
                    extra.setQty(extraReq.getQuantity());
                    extra.setPrice(extraPrice.getPrice());
                    orderItem.addTopping(extra);
                }
            }

            order.addOrderItem(orderItem);
        }

        return orderRepository.save(order);
    }

    public List<Order> getOrdersByBranch(String branchName) {
        Branch branch = branchRepository.findByName(branchName)
                .orElseThrow(() -> new RuntimeException("Branch not found"));
        return orderRepository.findByBranchIdOrderByCreatedAtAsc(branch.getId());
    }

    public List<Order> getPendingOrdersByBranch(String branchName) {
        Branch branch = branchRepository.findByName(branchName)
                .orElseThrow(() -> new RuntimeException("Branch not found"));
        return orderRepository.findByBranchIdAndStatusOrderByCreatedAtAsc(branch.getId(), "Pending");
    }

    public Order updateOrderStatus(Long orderId, String newStatus) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            order.setStatus(newStatus);
            return orderRepository.save(order);
        }
        return null;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
}
