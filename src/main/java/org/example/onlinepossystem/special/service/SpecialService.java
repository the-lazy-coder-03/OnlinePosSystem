package org.example.onlinepossystem.special.service;

import org.example.onlinepossystem.catalog.entity.MenuItem;
import org.example.onlinepossystem.catalog.entity.Pizza;
import org.example.onlinepossystem.catalog.api.SpecialCatalogAccess;
import org.example.onlinepossystem.branch.api.BranchLookup;
import org.example.onlinepossystem.branch.entity.Branch;
import jakarta.persistence.EntityManager;
import org.example.onlinepossystem.ordering.dto.OrderRequestDTO;
import org.example.onlinepossystem.ordering.entity.*;
import org.example.onlinepossystem.ordering.service.MenuOrderItemFactory;
import org.example.onlinepossystem.ordering.service.PizzaOrderItemFactory;
import org.example.onlinepossystem.special.dto.*;
import org.example.onlinepossystem.special.entity.*;
import org.example.onlinepossystem.special.repository.SpecialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SpecialService {
    private final SpecialRepository specials;
    private final SpecialCatalogAccess catalog;
    private final BranchLookup branches;
    private final EntityManager entityManager;
    private final MenuOrderItemFactory menuFactory;
    private final PizzaOrderItemFactory pizzaFactory;
    private final Clock clock;

    public SpecialService(SpecialRepository specials, SpecialCatalogAccess catalog,
                          BranchLookup branches, EntityManager entityManager,
                          MenuOrderItemFactory menuFactory, PizzaOrderItemFactory pizzaFactory, Clock clock) {
        this.specials = specials;
        this.catalog = catalog;
        this.branches = branches;
        this.entityManager = entityManager;
        this.menuFactory = menuFactory;
        this.pizzaFactory = pizzaFactory;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<SpecialView> available(Integer branchId) {
        LocalDate today = LocalDate.now(clock);
        return specials.findAllByBranchIdOrderBySortOrderAscIdAsc(branchId).stream()
                .filter(value -> isAvailable(value, today))
                .map(value -> toView(value, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SpecialView> weeklySchedule(Integer branchId) {
        LocalDate today = LocalDate.now(clock);
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1L);
        return specials.findAllByBranchIdOrderBySortOrderAscIdAsc(branchId).stream()
                .filter(value -> value.isActive() && !value.isArchived())
                .filter(value -> scheduledInWeek(value, weekStart))
                .sorted(Comparator.comparingInt(this::firstScheduledDay)
                        .thenComparingInt(Special::getSortOrder)
                        .thenComparing(Special::getId, Comparator.nullsLast(Long::compareTo)))
                .map(value -> toView(value, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SpecialView> adminList() {
        return specials.findAllByOrderByBranchIdAscSortOrderAscIdAsc().stream().map(value -> toView(value, true)).toList();
    }

    @Transactional(readOnly = true)
    public AdminSpecialCatalog adminCatalog() {
        return new AdminSpecialCatalog(
                branches.findAll().stream().map(value -> new AdminSpecialCatalog.Reference(value.id(), value.name())).toList(),
                catalog.menuCategories().stream().map(value -> new AdminSpecialCatalog.Reference(value.id(), value.name())).toList(),
                catalog.pizzaCategories().stream().map(value -> new AdminSpecialCatalog.Reference(value.id(), value.name())).toList(),
                catalog.pizzaSizes().stream().map(value -> new AdminSpecialCatalog.Reference(value.id(), value.name())).toList(),
                catalog.menuItems().stream().map(value -> new AdminSpecialCatalog.Product(value.id(), value.name(), value.categoryId())).toList(),
                catalog.pizzas().stream().map(value -> new AdminSpecialCatalog.Product(value.id(), value.name(), value.categoryId())).toList());
    }

    @Transactional(readOnly = true)
    public SpecialView adminDetail(Long id) {
        return toView(requireSpecial(id), true);
    }

    @Transactional
    public SpecialView adminSave(Long id, AdminSpecialRequest request) {
        validateAdminRequest(request);
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if ((id == null && specials.existsByCodeIgnoreCase(code))
                || (id != null && specials.existsByCodeIgnoreCaseAndIdNot(code, id))) {
            throw new IllegalArgumentException("Special code is already in use.");
        }
        Special special = id == null ? new Special() : requireSpecial(id);
        special.setCode(code);
        branches.requireById(request.branchId());
        special.setBranch(entityManager.getReference(Branch.class, request.branchId()));
        special.setName(request.name().trim());
        special.setDescription(trimToNull(request.description()));
        special.setBundlePrice(money(request.bundlePrice()));
        special.setActive(request.active());
        special.setStartsOn(request.startsOn());
        special.setEndsOn(request.endsOn());
        special.setSortOrder(request.sortOrder());
        special.getDays().clear();
        special.getDays().addAll(request.days());
        special.getComponents().clear();
        special.getAddons().clear();
        if (id != null) specials.flush();

        for (AdminSpecialRequest.Component input : request.components()) {
            SpecialComponent component = new SpecialComponent();
            component.setCode(input.code().trim().toUpperCase(Locale.ROOT));
            component.setLabel(input.label().trim());
            component.setProductType(normalizeType(input.productType()));
            component.setQuantity(input.quantity());
            component.setSelectionMode(normalizeSelectionMode(input.selectionMode()));
            component.setAllowRepeats(input.allowRepeats());
            component.setAllowCustomization(input.allowCustomization());
            component.setSortOrder(input.sortOrder());
            if (input.menuCategoryId() != null) component.setMenuCategory(catalog.requireMenuCategory(input.menuCategoryId()));
            if (input.pizzaCategoryId() != null) component.setPizzaCategory(catalog.requirePizzaCategory(input.pizzaCategoryId()));
            if (input.pizzaSizeId() != null) component.setPizzaSize(catalog.requirePizzaSize(input.pizzaSizeId()));
            component.setMenuItems(new LinkedHashSet<>(catalog.findMenuItems(safeSet(input.menuItemIds()))));
            component.setPizzas(new LinkedHashSet<>(catalog.findPizzas(safeSet(input.pizzaIds()))));
            validateComponent(component);
            special.addComponent(component);
        }
        for (AdminSpecialRequest.Addon input : safe(request.addons())) {
            SpecialAddon addon = new SpecialAddon();
            addon.setCode(input.code().trim().toUpperCase(Locale.ROOT));
            addon.setLabel(input.label().trim());
            addon.setProductType(normalizeType(input.productType()));
            addon.setPrice(money(input.price()));
            addon.setMaxQuantity(input.maxQuantity());
            addon.setAllowCustomization(input.allowCustomization());
            addon.setActive(input.active());
            addon.setSortOrder(input.sortOrder());
            if (input.menuItemId() != null) addon.setMenuItem(catalog.findMenuItem(input.menuItemId())
                    .orElseThrow(() -> new IllegalArgumentException("Add-on menu item not found.")));
            if (input.pizzaId() != null) addon.setPizza(catalog.findPizza(input.pizzaId())
                    .orElseThrow(() -> new IllegalArgumentException("Add-on pizza not found.")));
            if (input.pizzaSizeId() != null) addon.setPizzaSize(catalog.requirePizzaSize(input.pizzaSizeId()));
            validateAddon(addon);
            special.addAddon(addon);
        }
        return toView(specials.saveAndFlush(special), true);
    }

    @Transactional
    public SpecialView archive(Long id) {
        Special special = requireSpecial(id);
        special.setActive(false);
        special.setArchived(true);
        return toView(specials.saveAndFlush(special), true);
    }

    @Transactional
    public SpecialView restore(Long id) {
        Special special = requireSpecial(id);
        special.setArchived(false);
        special.setActive(false);
        return toView(specials.saveAndFlush(special), true);
    }

    @Transactional(readOnly = true)
    public SpecialQuoteResponse quote(Integer branchId, Long specialId, SpecialQuoteRequest request) {
        OrderRequestDTO.SpecialItemRequestDTO converted = new OrderRequestDTO.SpecialItemRequestDTO();
        converted.setSpecialId(specialId);
        converted.setQuantity(request == null || request.quantity() == null ? 1 : request.quantity());
        converted.setSelections(request == null || request.selections() == null ? List.of() : request.selections().stream().map(value -> {
            OrderRequestDTO.SpecialSelectionRequestDTO selection = new OrderRequestDTO.SpecialSelectionRequestDTO();
            selection.setComponentId(value.componentId()); selection.setSelectionIndex(value.selectionIndex()); selection.setItem(value.item());
            return selection;
        }).toList());
        converted.setAddons(request == null || request.addons() == null ? List.of() : request.addons().stream().map(value -> {
            OrderRequestDTO.SpecialAddonRequestDTO addon = new OrderRequestDTO.SpecialAddonRequestDTO();
            addon.setAddonId(value.addonId()); addon.setQuantity(value.quantity()); addon.setItem(value.item());
            return addon;
        }).toList());
        return resolve(branchId, converted).response();
    }

    @Transactional
    public void addToOrder(Order order, Integer branchId, OrderRequestDTO.SpecialItemRequestDTO request) {
        ResolvedSpecial resolved = resolve(branchId, request);
        OrderSpecialItem specialItem = resolved.orderItem();
        order.addSpecialItem(specialItem);
        for (ResolvedSelection selection : resolved.selections()) {
            if (selection.menuItem() != null) order.addMenuItem(selection.menuItem());
            if (selection.pizzaItem() != null) order.addPizzaItem(selection.pizzaItem());
            specialItem.addSelection(selection.snapshot());
        }
    }

    private ResolvedSpecial resolve(Integer branchId, OrderRequestDTO.SpecialItemRequestDTO request) {
        if (request == null || request.getSpecialId() == null) throw new IllegalArgumentException("Special is required.");
        Special special = specials.findOneById(request.getSpecialId())
                .orElseThrow(() -> new NoSuchElementException("Special not found."));
        if (!Objects.equals(special.getBranch().getId(), branchId) || !isAvailable(special, LocalDate.now(clock))) {
            throw new IllegalArgumentException("Special is not available for this branch and day.");
        }
        int bundleQuantity = request.getQuantity() == null ? 1 : request.getQuantity();
        if (bundleQuantity < 1 || bundleQuantity > 99) throw new IllegalArgumentException("Special quantity must be between 1 and 99.");

        Map<Long, List<OrderRequestDTO.SpecialSelectionRequestDTO>> byComponent = safe(request.getSelections()).stream()
                .collect(Collectors.groupingBy(OrderRequestDTO.SpecialSelectionRequestDTO::getComponentId));
        Set<Long> knownComponents = special.getComponents().stream().map(SpecialComponent::getId).collect(Collectors.toSet());
        if (!knownComponents.containsAll(byComponent.keySet())) throw new IllegalArgumentException("A selection does not belong to this special.");

        List<ResolvedSelection> resolved = new ArrayList<>();
        BigDecimal customizationTotal = BigDecimal.ZERO;
        for (SpecialComponent component : special.getComponents()) {
            List<OrderRequestDTO.SpecialSelectionRequestDTO> supplied = new ArrayList<>(byComponent.getOrDefault(component.getId(), List.of()));
            if (supplied.isEmpty() && "INCLUDED".equals(component.getSelectionMode())) {
                supplied = autoIncluded(component, branchId);
            }
            if (supplied.size() != component.getQuantity()) {
                throw new IllegalArgumentException(component.getLabel() + " requires exactly " + component.getQuantity() + " selection(s).");
            }
            validateIndexes(supplied, component.getQuantity(), component.getLabel());
            List<String> selectedKeys = new ArrayList<>();
            for (OrderRequestDTO.SpecialSelectionRequestDTO selection : supplied) {
                ResolvedSelection item = resolveComponent(branchId, component, selection);
                BigDecimal selectionCustomization = customizationTotal(item);
                item.snapshot().setCustomizationChargeAtTime(selectionCustomization);
                resolved.add(item);
                selectedKeys.add(item.productKey());
                customizationTotal = customizationTotal.add(selectionCustomization);
            }
            if (!component.isAllowRepeats() && new HashSet<>(selectedKeys).size() != selectedKeys.size()) {
                throw new IllegalArgumentException(component.getLabel() + " does not allow duplicate selections.");
            }
        }

        Map<Long, SpecialAddon> addonRules = special.getAddons().stream()
                .filter(SpecialAddon::isActive).collect(Collectors.toMap(SpecialAddon::getId, Function.identity()));
        BigDecimal addonTotal = BigDecimal.ZERO;
        Set<Long> seenAddons = new HashSet<>();
        for (OrderRequestDTO.SpecialAddonRequestDTO requestAddon : safe(request.getAddons())) {
            SpecialAddon addon = addonRules.get(requestAddon.getAddonId());
            if (addon == null || !seenAddons.add(addon.getId())) throw new IllegalArgumentException("Invalid special add-on.");
            int quantity = requestAddon.getQuantity() == null ? 1 : requestAddon.getQuantity();
            if (quantity < 1 || quantity > addon.getMaxQuantity()) throw new IllegalArgumentException("Invalid add-on quantity.");
            ResolvedSelection item = resolveAddon(branchId, addon, requestAddon.getItem(), quantity);
            BigDecimal selectionCustomization = customizationTotal(item);
            item.snapshot().setSelectionQuantity(quantity);
            item.snapshot().setAddonPriceAtTime(money(addon.getPrice()).multiply(BigDecimal.valueOf(quantity)));
            item.snapshot().setCustomizationChargeAtTime(selectionCustomization);
            resolved.add(item);
            addonTotal = addonTotal.add(addon.getPrice().multiply(BigDecimal.valueOf(quantity)));
            customizationTotal = customizationTotal.add(selectionCustomization);
        }

        BigDecimal perBundle = money(special.getBundlePrice()).add(customizationTotal).add(addonTotal);
        BigDecimal lineTotal = perBundle.multiply(BigDecimal.valueOf(bundleQuantity)).setScale(2, RoundingMode.HALF_UP);
        OrderSpecialItem orderItem = new OrderSpecialItem();
        orderItem.setSpecialId(special.getId()); orderItem.setSpecialNameAtTime(special.getName());
        orderItem.setSpecialDescriptionAtTime(special.getDescription()); orderItem.setQuantity(bundleQuantity);
        orderItem.setBasePriceAtTime(money(special.getBundlePrice()));
        orderItem.setCustomizationTotalAtTime(money(customizationTotal)); orderItem.setAddonTotalAtTime(money(addonTotal));
        orderItem.setFinalLineTotalAtTime(lineTotal);

        List<SpecialQuoteResponse.Selection> quoteSelections = resolved.stream().map(this::quoteSelection).toList();
        return new ResolvedSpecial(orderItem, resolved, new SpecialQuoteResponse(special.getId(), special.getName(), bundleQuantity,
                money(special.getBundlePrice()), money(customizationTotal), money(addonTotal), lineTotal, quoteSelections));
    }

    private ResolvedSelection resolveComponent(Integer branchId, SpecialComponent component,
                                                OrderRequestDTO.SpecialSelectionRequestDTO selection) {
        OrderRequestDTO.OrderItemRequestDTO item = requireSingleItem(selection.getItem());
        validateCustomization(component.isAllowCustomization(), item);
        if ("MENU_ITEM".equals(component.getProductType())) {
            MenuItem product = requireEligibleMenu(component, item.getMenuItemId());
            OrderMenuItem line = component.isAllowCustomization() ? menuFactory.create(branchId, item) : baseMenuLine(branchId, product);
            return selection(component, selection.getSelectionIndex(), product.getName(), line, null);
        }
        Pizza product = requireEligiblePizza(component, item.getPizzaId());
        lockSize(component.getPizzaSize() == null ? null : component.getPizzaSize().getId(), item);
        OrderPizzaItem line = pizzaFactory.create(branchId, item);
        return selection(component, selection.getSelectionIndex(), product.getName(), null, line);
    }

    private ResolvedSelection resolveAddon(Integer branchId, SpecialAddon addon,
                                            OrderRequestDTO.OrderItemRequestDTO item, int quantity) {
        item = requireSingleItem(item);
        validateCustomization(addon.isAllowCustomization(), item);
        OrderSpecialSelection snapshot = new OrderSpecialSelection();
        snapshot.setSelectionKind("ADDON"); snapshot.setSpecialAddonId(addon.getId()); snapshot.setSelectionIndex(1);
        snapshot.setSelectionQuantity(quantity);
        snapshot.setLabelAtTime(addon.getLabel());
        if ("MENU_ITEM".equals(addon.getProductType())) {
            if (!Objects.equals(addon.getMenuItem().getId(), item.getMenuItemId())) throw new IllegalArgumentException("Invalid add-on product.");
            OrderMenuItem line = addon.isAllowCustomization() ? menuFactory.create(branchId, item) : baseMenuLine(branchId, addon.getMenuItem());
            line.setQty(quantity); snapshot.setMenuItem(line); snapshot.setProductNameAtTime(addon.getMenuItem().getName());
            return resolved(snapshot, line, null, "M:" + addon.getMenuItem().getId());
        }
        if (!Objects.equals(addon.getPizza().getId(), item.getPizzaId())) throw new IllegalArgumentException("Invalid add-on product.");
        lockSize(addon.getPizzaSize().getId(), item);
        OrderPizzaItem line = pizzaFactory.create(branchId, item); line.setQty(quantity);
        snapshot.setPizzaItem(line); snapshot.setProductNameAtTime(addon.getPizza().getName());
        snapshot.setPizzaSizeCmAtTime(addon.getPizzaSize().getCm());
        return resolved(snapshot, null, line, "P:" + addon.getPizza().getId());
    }

    private ResolvedSelection selection(SpecialComponent component, int index, String name,
                                        OrderMenuItem menuItem, OrderPizzaItem pizzaItem) {
        OrderSpecialSelection snapshot = new OrderSpecialSelection();
        snapshot.setSelectionKind("COMPONENT"); snapshot.setSpecialComponentId(component.getId());
        snapshot.setSelectionIndex(index); snapshot.setLabelAtTime(component.getLabel()); snapshot.setProductNameAtTime(name);
        snapshot.setSelectionQuantity(1);
        snapshot.setMenuItem(menuItem); snapshot.setPizzaItem(pizzaItem);
        if (component.getPizzaSize() != null) snapshot.setPizzaSizeCmAtTime(component.getPizzaSize().getCm());
        String key = menuItem != null ? "M:" + menuItem.getMenuItemId() : "P:" + pizzaItem.getPizzaId();
        return resolved(snapshot, menuItem, pizzaItem, key);
    }

    private ResolvedSelection resolved(OrderSpecialSelection snapshot, OrderMenuItem menuItem,
                                       OrderPizzaItem pizzaItem, String key) {
        Integer productId = menuItem != null ? menuItem.getMenuItemId() : pizzaItem.getPizzaId();
        String type = menuItem != null ? "MENU_ITEM" : "PIZZA";
        return new ResolvedSelection(snapshot, menuItem, pizzaItem, key,
                new SpecialQuoteResponse.Selection(snapshot.getSelectionKind(),
                        "COMPONENT".equals(snapshot.getSelectionKind()) ? snapshot.getSpecialComponentId() : snapshot.getSpecialAddonId(),
                        snapshot.getSelectionIndex(), snapshot.getLabelAtTime(), type, productId,
                        snapshot.getProductNameAtTime(), snapshot.getPizzaSizeCmAtTime(), snapshot.getSelectionQuantity(),
                        money(snapshot.getAddonPriceAtTime()), money(snapshot.getCustomizationChargeAtTime())));
    }

    private SpecialQuoteResponse.Selection quoteSelection(ResolvedSelection selection) {
        OrderSpecialSelection snapshot = selection.snapshot();
        OrderMenuItem menuItem = selection.menuItem();
        OrderPizzaItem pizzaItem = selection.pizzaItem();
        return new SpecialQuoteResponse.Selection(snapshot.getSelectionKind(),
                "COMPONENT".equals(snapshot.getSelectionKind()) ? snapshot.getSpecialComponentId() : snapshot.getSpecialAddonId(),
                snapshot.getSelectionIndex(), snapshot.getLabelAtTime(), menuItem != null ? "MENU_ITEM" : "PIZZA",
                menuItem != null ? menuItem.getMenuItemId() : pizzaItem.getPizzaId(), snapshot.getProductNameAtTime(),
                snapshot.getPizzaSizeCmAtTime(), snapshot.getSelectionQuantity(), money(snapshot.getAddonPriceAtTime()),
                money(snapshot.getCustomizationChargeAtTime()));
    }

    private OrderMenuItem baseMenuLine(Integer branchId, MenuItem item) {
        var price = catalog.menuPrice(branchId, item.getId())
                .orElseThrow(() -> new IllegalArgumentException(item.getName() + " is unavailable for this branch."));
        OrderMenuItem line = new OrderMenuItem();
        line.setMenuItemId(item.getId()); line.setItemNameAtTime(item.getName()); line.setQty(1);
        line.setUnitPriceAtTime(price.doubleValue());
        return line;
    }

    private List<OrderRequestDTO.SpecialSelectionRequestDTO> autoIncluded(SpecialComponent component, Integer branchId) {
        List<SpecialView.Option> options = componentOptions(component, branchId, true);
        if (options.size() != 1 || component.isAllowCustomization()) return List.of();
        List<OrderRequestDTO.SpecialSelectionRequestDTO> result = new ArrayList<>();
        for (int index = 1; index <= component.getQuantity(); index++) {
            OrderRequestDTO.OrderItemRequestDTO item = new OrderRequestDTO.OrderItemRequestDTO();
            if ("MENU_ITEM".equals(component.getProductType())) item.setMenuItemId(options.get(0).id());
            else { item.setPizzaId(options.get(0).id()); item.setPizzaSizeId(component.getPizzaSize().getId()); }
            item.setQuantity(1); item.setCustomizations(List.of());
            OrderRequestDTO.SpecialSelectionRequestDTO selection = new OrderRequestDTO.SpecialSelectionRequestDTO();
            selection.setComponentId(component.getId()); selection.setSelectionIndex(index); selection.setItem(item);
            result.add(selection);
        }
        return result;
    }

    private MenuItem requireEligibleMenu(SpecialComponent component, Integer id) {
        if (id == null) throw new IllegalArgumentException(component.getLabel() + " requires a menu item.");
        MenuItem item = catalog.findMenuItem(id).filter(MenuItem::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Selected menu item is unavailable."));
        boolean explicit = component.getMenuItems().stream().anyMatch(value -> Objects.equals(value.getId(), id));
        boolean category = component.getMenuItems().isEmpty() && component.getMenuCategory() != null
                && Objects.equals(component.getMenuCategory().getId(), item.getCategory().getId());
        if (!explicit && !category) throw new IllegalArgumentException("Menu item does not qualify for " + component.getLabel() + ".");
        return item;
    }

    private Pizza requireEligiblePizza(SpecialComponent component, Integer id) {
        if (id == null) throw new IllegalArgumentException(component.getLabel() + " requires a pizza.");
        Pizza pizza = catalog.findPizza(id).filter(Pizza::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Selected pizza is unavailable."));
        boolean explicit = component.getPizzas().stream().anyMatch(value -> Objects.equals(value.getId(), id));
        boolean category = component.getPizzas().isEmpty() && component.getPizzaCategory() != null
                && Objects.equals(component.getPizzaCategory().getId(), pizza.getCategory().getId());
        if (!explicit && !category) throw new IllegalArgumentException("Pizza does not qualify for " + component.getLabel() + ".");
        return pizza;
    }

    private void lockSize(Integer sizeId, OrderRequestDTO.OrderItemRequestDTO item) {
        if (sizeId == null) return;
        Integer selected = item.getPizzaSizeId();
        if (selected == null && item.getSizeCm() != null) {
            Integer requiredCm = catalog.requirePizzaSize(sizeId).getCm();
            if (Objects.equals(requiredCm, item.getSizeCm())) selected = sizeId;
        }
        if (!Objects.equals(sizeId, selected)) throw new IllegalArgumentException("The special requires the configured pizza size.");
        item.setPizzaSizeId(sizeId);
    }

    private void validateCustomization(boolean allowed, OrderRequestDTO.OrderItemRequestDTO item) {
        if (!allowed && ((item.getCustomizations() != null && !item.getCustomizations().isEmpty()) || item.getPizzaBaseOptionId() != null)) {
            throw new IllegalArgumentException("This included item cannot be customized.");
        }
    }

    private OrderRequestDTO.OrderItemRequestDTO requireSingleItem(OrderRequestDTO.OrderItemRequestDTO item) {
        if (item == null) throw new IllegalArgumentException("Special selection item is required.");
        if (item.getQuantity() == null) item.setQuantity(1);
        if (item.getQuantity() != 1) throw new IllegalArgumentException("Special child quantities are controlled by the bundle.");
        if ((item.getMenuItemId() == null) == (item.getPizzaId() == null)) throw new IllegalArgumentException("Select exactly one product.");
        return item;
    }

    private void validateIndexes(List<OrderRequestDTO.SpecialSelectionRequestDTO> values, int quantity, String label) {
        Set<Integer> indexes = values.stream().map(OrderRequestDTO.SpecialSelectionRequestDTO::getSelectionIndex).collect(Collectors.toSet());
        if (indexes.size() != quantity || indexes.stream().anyMatch(value -> value == null || value < 1 || value > quantity)) {
            throw new IllegalArgumentException("Invalid selection slots for " + label + ".");
        }
    }

    private BigDecimal customizationTotal(ResolvedSelection selection) {
        BigDecimal total = BigDecimal.ZERO;
        if (selection.menuItem() != null) {
            OrderMenuItem item = selection.menuItem();
            for (OrderMenuItemExtra extra : item.getExtras()) total = total.add(money(extra.getUnitPriceAtTime()).multiply(BigDecimal.valueOf(extra.getQty())));
            if (item.getBurgerProtein() != null) total = total.add(money(item.getBurgerProtein().getUnitPriceAtTime()).multiply(BigDecimal.valueOf(item.getBurgerProtein().getProteinQtyPerBurger())));
            for (OrderBurgerExtraComponent extra : item.getExtraBurgerComponents()) total = total.add(money(extra.getUnitPriceAtTime()).multiply(BigDecimal.valueOf(extra.getQty())));
            total = total.multiply(BigDecimal.valueOf(item.getQty()));
        } else {
            OrderPizzaItem item = selection.pizzaItem();
            if (item.getBaseOption() != null) total = total.add(money(item.getBaseOption().getUnitPriceAtTime()));
            for (OrderPizzaItemExtra extra : item.getExtras()) total = total.add(money(extra.getUnitPriceAtTime()).multiply(BigDecimal.valueOf(extra.getQty())));
            total = total.multiply(BigDecimal.valueOf(item.getQty()));
        }
        return money(total);
    }

    private boolean isAvailable(Special special, LocalDate date) {
        return special.isActive() && !special.isArchived() && special.getDays().contains(date.getDayOfWeek().getValue())
                && (special.getStartsOn() == null || !date.isBefore(special.getStartsOn()))
                && (special.getEndsOn() == null || !date.isAfter(special.getEndsOn()));
    }

    private boolean scheduledInWeek(Special special, LocalDate weekStart) {
        return special.getDays().stream()
                .filter(day -> day >= 1 && day <= 7)
                .map(day -> weekStart.plusDays(day - 1L))
                .anyMatch(day -> (special.getStartsOn() == null || !day.isBefore(special.getStartsOn()))
                        && (special.getEndsOn() == null || !day.isAfter(special.getEndsOn())));
    }

    private int firstScheduledDay(Special special) {
        return special.getDays().stream().min(Integer::compareTo).orElse(8);
    }

    private SpecialView toView(Special special, boolean includeInactiveAddons) {
        Integer branchId = special.getBranch().getId();
        boolean branchAvailableOnly = !includeInactiveAddons;
        List<SpecialView.Component> components = special.getComponents().stream().map(component ->
                new SpecialView.Component(component.getId(), component.getCode(), component.getLabel(), component.getProductType(),
                        component.getQuantity(), component.getSelectionMode(),
                        component.getMenuCategory() == null ? null : component.getMenuCategory().getId(),
                        component.getPizzaCategory() == null ? null : component.getPizzaCategory().getId(),
                        component.getPizzaSize() == null ? null : component.getPizzaSize().getId(),
                        component.getPizzaSize() == null ? null : component.getPizzaSize().getCm(),
                        component.isAllowRepeats(), component.isAllowCustomization(), component.getSortOrder(),
                        componentOptions(component, branchId, branchAvailableOnly))).toList();
        List<SpecialView.Addon> addons = special.getAddons().stream()
                .filter(addon -> includeInactiveAddons || addon.isActive())
                .filter(addon -> !branchAvailableOnly || addonAvailable(addon, branchId)).map(addon -> {
            boolean menu = "MENU_ITEM".equals(addon.getProductType());
            return new SpecialView.Addon(addon.getId(), addon.getCode(), addon.getLabel(), addon.getProductType(),
                    menu ? addon.getMenuItem().getId() : addon.getPizza().getId(),
                    menu ? addon.getMenuItem().getName() : addon.getPizza().getName(),
                    addon.getPizzaSize() == null ? null : addon.getPizzaSize().getId(),
                    addon.getPizzaSize() == null ? null : addon.getPizzaSize().getCm(), addon.getPrice(),
                    addon.getMaxQuantity(), addon.isAllowCustomization(), addon.isActive(), addon.getSortOrder());
        }).toList();
        return new SpecialView(special.getId(), special.getCode(), special.getName(), special.getDescription(), special.getBundlePrice(),
                special.getBranch().getId(), special.isActive(), special.isArchived(), special.getStartsOn(), special.getEndsOn(),
                special.getDays(), isAvailable(special, LocalDate.now(clock)), special.getSortOrder(), components, addons);
    }

    private List<SpecialView.Option> componentOptions(SpecialComponent component, Integer branchId,
                                                      boolean branchAvailableOnly) {
        if ("MENU_ITEM".equals(component.getProductType())) {
            Collection<MenuItem> choices = component.getMenuItems().isEmpty()
                    ? catalog.activeMenuItems().stream().filter(value -> component.getMenuCategory() != null
                    && Objects.equals(value.getCategory().getId(), component.getMenuCategory().getId())).toList()
                    : component.getMenuItems();
            return choices.stream().filter(MenuItem::isActive)
                    .filter(value -> !branchAvailableOnly || catalog.menuPrice(branchId, value.getId()).isPresent())
                    .sorted(Comparator.comparing(MenuItem::getName))
                    .map(value -> new SpecialView.Option(value.getId(), value.getName(), value.getCategory().getName())).toList();
        }
        Collection<Pizza> choices = component.getPizzas().isEmpty()
                ? catalog.activePizzas().stream().filter(value -> component.getPizzaCategory() != null
                && Objects.equals(value.getCategory().getId(), component.getPizzaCategory().getId())).toList()
                : component.getPizzas();
        Integer sizeId = component.getPizzaSize() == null ? null : component.getPizzaSize().getId();
        return choices.stream().filter(Pizza::isActive)
                .filter(value -> !branchAvailableOnly || sizeId == null
                        || catalog.pizzaPrice(branchId, value.getId(), sizeId).isPresent())
                .sorted(Comparator.comparing(Pizza::getName))
                .map(value -> new SpecialView.Option(value.getId(), value.getName(), value.getCategory().getName())).toList();
    }

    private boolean addonAvailable(SpecialAddon addon, Integer branchId) {
        if ("MENU_ITEM".equals(addon.getProductType())) {
            return catalog.menuPrice(branchId, addon.getMenuItem().getId()).isPresent();
        }
        return catalog.pizzaPrice(branchId, addon.getPizza().getId(), addon.getPizzaSize().getId()).isPresent();
    }

    private BigDecimal money(Number value) { return value == null ? BigDecimal.ZERO.setScale(2) : new BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP); }
    private <T> List<T> safe(List<T> values) { return values == null ? List.of() : values; }
    private <T> Set<T> safeSet(Set<T> values) { return values == null ? Set.of() : values; }

    private Special requireSpecial(Long id) {
        return specials.findOneById(id).orElseThrow(() -> new NoSuchElementException("Special not found."));
    }

    private void validateAdminRequest(AdminSpecialRequest request) {
        if (request.startsOn() != null && request.endsOn() != null && request.endsOn().isBefore(request.startsOn())) {
            throw new IllegalArgumentException("The end date cannot be before the start date.");
        }
        Set<String> componentCodes = new HashSet<>();
        for (AdminSpecialRequest.Component component : request.components()) {
            if (!componentCodes.add(component.code().trim().toUpperCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Component codes must be unique within a special.");
            }
        }
        Set<String> addonCodes = new HashSet<>();
        for (AdminSpecialRequest.Addon addon : safe(request.addons())) {
            if (!addonCodes.add(addon.code().trim().toUpperCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Add-on codes must be unique within a special.");
            }
        }
    }

    private void validateComponent(SpecialComponent component) {
        boolean menu = "MENU_ITEM".equals(component.getProductType());
        if (menu && (component.getPizzaCategory() != null || component.getPizzaSize() != null || !component.getPizzas().isEmpty())) {
            throw new IllegalArgumentException(component.getLabel() + " mixes menu-item and pizza rules.");
        }
        if (!menu && (component.getMenuCategory() != null || !component.getMenuItems().isEmpty() || component.getPizzaSize() == null)) {
            throw new IllegalArgumentException(component.getLabel() + " requires a pizza size and pizza-only rules.");
        }
        boolean hasOptions = menu
                ? component.getMenuCategory() != null || !component.getMenuItems().isEmpty()
                : component.getPizzaCategory() != null || !component.getPizzas().isEmpty();
        if (!hasOptions) throw new IllegalArgumentException(component.getLabel() + " requires a category or explicit products.");
    }

    private void validateAddon(SpecialAddon addon) {
        boolean menu = "MENU_ITEM".equals(addon.getProductType());
        if (menu && (addon.getMenuItem() == null || addon.getPizza() != null || addon.getPizzaSize() != null)) {
            throw new IllegalArgumentException(addon.getLabel() + " requires exactly one menu item.");
        }
        if (!menu && (addon.getPizza() == null || addon.getPizzaSize() == null || addon.getMenuItem() != null)) {
            throw new IllegalArgumentException(addon.getLabel() + " requires a pizza and size.");
        }
    }

    private String normalizeType(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("MENU_ITEM", "PIZZA").contains(normalized)) throw new IllegalArgumentException("Invalid product type.");
        return normalized;
    }

    private String normalizeSelectionMode(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if ("SELECT".equals(normalized)) normalized = "CUSTOMER_CHOICE";
        if (!Set.of("CUSTOMER_CHOICE", "INCLUDED").contains(normalized)) throw new IllegalArgumentException("Invalid selection mode.");
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private record ResolvedSelection(OrderSpecialSelection snapshot, OrderMenuItem menuItem, OrderPizzaItem pizzaItem,
                                     String productKey, SpecialQuoteResponse.Selection quote) {}
    private record ResolvedSpecial(OrderSpecialItem orderItem, List<ResolvedSelection> selections, SpecialQuoteResponse response) {}
}
