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

    public List<MenuDTO> getMenuForBranch(String branchName) {
        Optional<Branch> branchOpt = branchRepository.findByName(branchName);
        if (branchOpt.isEmpty()) {
            return new ArrayList<>();
        }
        Branch branch = branchOpt.get();

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
    public Order placeOrder(OrderRequestDTO request) {
        Branch branch = branchRepository.findByName(request.getBranchName())
                .orElseThrow(() -> new RuntimeException("Branch not found"));

        Order order = new Order();
        order.setBranch(branch);
        order.setCustomerName(request.getCustomerName());
        order.setPhone(request.getPhone());
        order.setOrderType(request.getOrderType() != null ? request.getOrderType() : "pickup");
        order.setCreatedAt(LocalDateTime.now());
        order.setStatus("created");

        for (OrderRequestDTO.OrderItemRequestDTO itemRequest : request.getItems()) {
            if (itemRequest.getPizzaId() != null) {
                // Handle Pizza
                Pizza pizza = pizzaRepository.findById(itemRequest.getPizzaId())
                        .orElseThrow(() -> new RuntimeException("Pizza not found"));
                PizzaSize size = pizzaSizeRepository.findById(itemRequest.getPizzaSizeId())
                        .orElseThrow(() -> new RuntimeException("Pizza size not found"));
                BranchPizzaPrice bpp = branchPizzaPriceRepository
                        .findByBranchIdAndPizzaIdAndPizzaSizeId(branch.getId(), pizza.getId(), size.getId())
                        .orElseThrow(() -> new RuntimeException("Pizza price not found for branch"));

                OrderPizzaItem pizzaItem = new OrderPizzaItem();
                pizzaItem.setPizza(pizza);
                pizzaItem.setPizzaSize(size);
                pizzaItem.setQty(itemRequest.getQuantity());
                pizzaItem.setBasePriceAtTime(bpp.getPrice());
                pizzaItem.setNotes(itemRequest.getNotes());

                if (itemRequest.getCustomizations() != null) {
                    for (OrderRequestDTO.CustomizationRequestDTO custReq : itemRequest.getCustomizations()) {
                        Ingredient ing = ingredientRepository.findById(custReq.getId())
                                .orElseThrow(() -> new RuntimeException("Ingredient not found"));
                        
                        BranchExtraPrice bep = branchExtraPriceRepository
                                .findById(new BranchExtraPrice.BranchExtraPriceId(branch.getId(), ing.getPriceCategory().getId(), size.getId()))
                                .orElseThrow(() -> new RuntimeException("Extra price not found"));

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
                MenuItem menuItem = menuItemRepository.findById(itemRequest.getMenuItemId())
                        .orElseThrow(() -> new RuntimeException("Menu item not found"));

                BranchMenuItemPrice branchPrice = branchMenuItemPriceRepository
                        .findByBranchIdAndMenuItemId(branch.getId(), menuItem.getId())
                        .orElseThrow(() -> new RuntimeException("Price not found for branch"));

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

                        Optional<BurgerTopping> bt = burgerToppingRepository.findById(custReq.getId());
                        if (bt.isPresent()) {
                            extraName = bt.get().getToppingName();
                            extraPrice = bt.get().getPrice();
                        } else {
                            Optional<SaladIngredient> si = saladIngredientRepository.findById(custReq.getId());
                            if (si.isPresent()) {
                                extraName = si.get().getIngredientName();
                                extraPrice = si.get().getPrice();
                            }
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

        return orderRepository.save(order);
    }

    public List<Order> getOrdersByBranch(String branchName) {
        Optional<Branch> branchOpt = branchRepository.findByName(branchName);
        if (branchOpt.isEmpty()) return new ArrayList<>();
        return orderRepository.findByBranchId(branchOpt.get().getId());
    }

    public List<Order> getPendingOrdersByBranch(String branchName) {
        Optional<Branch> branchOpt = branchRepository.findByName(branchName);
        if (branchOpt.isEmpty()) return new ArrayList<>();
        return orderRepository.findByBranchIdAndStatus(branchOpt.get().getId(), "created");
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
