package org.example.onlinepossystem.catalog.service;

import org.example.onlinepossystem.branch.api.BranchLookup;
import org.example.onlinepossystem.branch.api.BranchView;
import org.example.onlinepossystem.catalog.api.OrderCatalogResolver;
import org.example.onlinepossystem.catalog.dto.MenuDTO;
import org.example.onlinepossystem.catalog.entity.BranchExtraPrice;
import org.example.onlinepossystem.catalog.entity.BranchMenuItemPrice;
import org.example.onlinepossystem.catalog.entity.BranchPizzaPrice;
import org.example.onlinepossystem.catalog.entity.Ingredient;
import org.example.onlinepossystem.catalog.entity.MenuItemModifierGroup;
import org.example.onlinepossystem.catalog.entity.MenuItem;
import org.example.onlinepossystem.catalog.entity.ModifierGroup;
import org.example.onlinepossystem.catalog.entity.ModifierOption;
import org.example.onlinepossystem.catalog.entity.Pizza;
import org.example.onlinepossystem.catalog.entity.PizzaSize;
import org.example.onlinepossystem.catalog.entity.SaladIngredient;
import org.example.onlinepossystem.catalog.menu.dto.BurgerComponentRow;
import org.example.onlinepossystem.catalog.menu.repository.BurgerComponentReadRepository;
import org.example.onlinepossystem.catalog.repository.BranchExtraPriceRepository;
import org.example.onlinepossystem.catalog.repository.BranchMenuItemPriceRepository;
import org.example.onlinepossystem.catalog.repository.BranchPizzaPriceRepository;
import org.example.onlinepossystem.catalog.repository.IngredientRepository;
import org.example.onlinepossystem.catalog.repository.MenuItemModifierGroupRepository;
import org.example.onlinepossystem.catalog.repository.MenuItemRepository;
import org.example.onlinepossystem.catalog.repository.ModifierGroupRepository;
import org.example.onlinepossystem.catalog.repository.ModifierOptionRepository;
import org.example.onlinepossystem.catalog.repository.PizzaRepository;
import org.example.onlinepossystem.catalog.repository.PizzaSizeRepository;
import org.example.onlinepossystem.catalog.repository.SaladIngredientRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
public class CatalogOrderResolver implements OrderCatalogResolver {
    private final BranchLookup branchLookup;
    private final MenuItemRepository menuItemRepository;
    private final BranchMenuItemPriceRepository branchMenuItemPriceRepository;
    private final BurgerComponentReadRepository burgerComponentReadRepository;
    private final SaladIngredientRepository saladIngredientRepository;
    private final PizzaSizeRepository pizzaSizeRepository;
    private final IngredientRepository ingredientRepository;
    private final BranchPizzaPriceRepository branchPizzaPriceRepository;
    private final PizzaRepository pizzaRepository;
    private final BranchExtraPriceRepository branchExtraPriceRepository;
    private final ModifierOptionRepository modifierOptionRepository;
    private final MenuItemModifierGroupRepository menuItemModifierGroupRepository;
    private final ModifierGroupRepository modifierGroupRepository;

    public CatalogOrderResolver(BranchLookup branchLookup,
                                MenuItemRepository menuItemRepository,
                                BranchMenuItemPriceRepository branchMenuItemPriceRepository,
                                BurgerComponentReadRepository burgerComponentReadRepository,
                                SaladIngredientRepository saladIngredientRepository,
                                PizzaSizeRepository pizzaSizeRepository,
                                IngredientRepository ingredientRepository,
                                BranchPizzaPriceRepository branchPizzaPriceRepository,
                                PizzaRepository pizzaRepository,
                                BranchExtraPriceRepository branchExtraPriceRepository,
                                ModifierOptionRepository modifierOptionRepository,
                                MenuItemModifierGroupRepository menuItemModifierGroupRepository,
                                ModifierGroupRepository modifierGroupRepository) {
        this.branchLookup = branchLookup;
        this.menuItemRepository = menuItemRepository;
        this.branchMenuItemPriceRepository = branchMenuItemPriceRepository;
        this.burgerComponentReadRepository = burgerComponentReadRepository;
        this.saladIngredientRepository = saladIngredientRepository;
        this.pizzaSizeRepository = pizzaSizeRepository;
        this.ingredientRepository = ingredientRepository;
        this.branchPizzaPriceRepository = branchPizzaPriceRepository;
        this.pizzaRepository = pizzaRepository;
        this.branchExtraPriceRepository = branchExtraPriceRepository;
        this.modifierOptionRepository = modifierOptionRepository;
        this.menuItemModifierGroupRepository = menuItemModifierGroupRepository;
        this.modifierGroupRepository = modifierGroupRepository;
    }

    @Override
    public List<MenuDTO> getMenuForBranch(String branchName) {
        if (branchName == null || branchName.isBlank()) {
            throw new IllegalArgumentException("Branch name is required.");
        }
        BranchView branch = branchLookup.requireByName(branchName);

        List<MenuDTO> menu = new ArrayList<>();
        List<BranchMenuItemPrice> menuItemPrices = branchMenuItemPriceRepository.findByBranchId(branch.id());
        Map<Integer, Double> itemPriceMap = menuItemPrices.stream()
                .collect(Collectors.toMap(p -> p.getMenuItem().getId(), BranchMenuItemPrice::getPrice, (v1, v2) -> v1));

        List<MenuItem> items = menuItemRepository.findAllByActiveTrue();
        List<Integer> menuItemIds = items.stream().map(MenuItem::getId).toList();
        Map<Integer, List<BurgerComponentRow>> burgerComponentsByMenuItem = burgerComponentReadRepository
                .findComponentsForMenuItems(branch.id(), menuItemIds)
                .stream()
                .collect(Collectors.groupingBy(
                        BurgerComponentRow::burgerId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        for (MenuItem item : items) {
            if (!itemPriceMap.containsKey(item.getId())) {
                continue;
            }
            MenuDTO dto = new MenuDTO();
            dto.setCategoryName(item.getCategory() != null ? item.getCategory().getName() : "Uncategorized");
            dto.setCategoryId(item.getCategory() != null ? item.getCategory().getId() : null);
            dto.setMenuItemId(item.getId());
            dto.setMenuItemName(item.getName());
            dto.setDescription(item.getDescription());
            dto.setPrice(itemPriceMap.get(item.getId()));
            dto.setIs300ml(item.isIs300ml());
            dto.setIs2l(item.isIs2l());
            dto.setPizza(false);

            List<MenuDTO.CustomizationDTO> customizations = new ArrayList<>();
            for (BurgerComponentRow component : burgerComponentsByMenuItem.getOrDefault(item.getId(), List.of())) {
                MenuDTO.CustomizationDTO customization = new MenuDTO.CustomizationDTO();
                customization.setId(component.componentId());
                customization.setName(component.name());
                customization.setPrice(toDouble(component.price()));
                customization.setDefault(Boolean.TRUE.equals(component.defaultSelected()));
                customization.setType(component.componentType());
                customization.setProteinQuantityRequired(component.proteinQuantityRequired());
                customizations.add(customization);
            }
            for (SaladIngredient ingredient : saladIngredientRepository.findBySaladId(item.getId())) {
                MenuDTO.CustomizationDTO customization = new MenuDTO.CustomizationDTO();
                customization.setId(ingredient.getId());
                customization.setName(ingredient.getIngredientName());
                customization.setPrice(ingredient.getPrice());
                customization.setDefault(false);
                customizations.add(customization);
            }
            dto.setCustomizations(customizations);
            menu.add(dto);
        }

        List<BranchPizzaPrice> pizzaPrices = branchPizzaPriceRepository.findByBranchId(branch.id());
        Map<Integer, List<BranchPizzaPrice>> pizzaPriceMap = pizzaPrices.stream()
                .collect(Collectors.groupingBy(p -> p.getPizza().getId()));

        for (Pizza pizza : pizzaRepository.findAllByActiveTrue()) {
            if (!pizzaPriceMap.containsKey(pizza.getId())) {
                continue;
            }
            for (BranchPizzaPrice branchPizzaPrice : pizzaPriceMap.get(pizza.getId())) {
                MenuDTO dto = new MenuDTO();
                dto.setCategoryName(pizza.getCategory() != null ? pizza.getCategory().getName() : "Pizzas");
                dto.setCategoryId(pizza.getCategory() != null ? pizza.getCategory().getId() : null);
                dto.setMenuItemId(pizza.getId());
                dto.setMenuItemName(pizza.getName() + " (" + branchPizzaPrice.getPizzaSize().getCm() + "cm)");
                dto.setDescription(pizza.getDescription());
                dto.setPrice(branchPizzaPrice.getPrice());
                dto.setPizza(true);
                dto.setPizzaSizeId(branchPizzaPrice.getPizzaSize().getId());
                dto.setCustomizations(List.of());
                menu.add(dto);
            }
        }

        return menu;
    }

    @Override
    public ResolvedPizzaItem resolvePizzaItem(
            Integer branchId,
            Integer pizzaId,
            Integer pizzaSizeId,
            Integer sizeCm,
            List<CatalogCustomizationRequest> customizations
    ) {
        Integer resolvedSizeId = pizzaSizeId;
        if (resolvedSizeId == null && sizeCm != null) {
            resolvedSizeId = pizzaSizeRepository.findByCm(sizeCm)
                    .map(PizzaSize::getId)
                    .orElseThrow(() -> new java.util.NoSuchElementException("Pizza size not found for cm: " + sizeCm));
        }
        if (resolvedSizeId == null) {
            throw new IllegalArgumentException("pizzaSizeId or sizeCm is required when pizzaId is provided.");
        }
        Integer finalSizeId = resolvedSizeId;

        Pizza pizza = pizzaRepository.findById(pizzaId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Pizza not found with ID: " + pizzaId));
        PizzaSize size = pizzaSizeRepository.findById(finalSizeId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Pizza size not found with ID: " + finalSizeId));
        BranchPizzaPrice price = branchPizzaPriceRepository
                .findByBranchIdAndPizzaIdAndPizzaSizeId(branchId, pizza.getId(), size.getId())
                .orElseThrow(() -> new java.util.NoSuchElementException("Price not found for pizza: "
                        + pizza.getName() + " size: " + size.getCm() + "cm in branch: " + branchId));

        List<ResolvedPizzaExtra> extras = new ArrayList<>();
        for (CatalogCustomizationRequest customization : safeCustomizations(customizations)) {
            Ingredient ingredient = ingredientRepository.findById(customization.id())
                    .orElseThrow(() -> new java.util.NoSuchElementException(
                            "Ingredient not found with ID: " + customization.id()
                    ));

            BranchExtraPrice extraPrice = branchExtraPriceRepository
                    .findById(new BranchExtraPrice.BranchExtraPriceId(
                            branchId,
                            ingredient.getPriceCategory().getId(),
                            size.getId()
                    ))
                    .orElseThrow(() -> new java.util.NoSuchElementException("Extra price not found for ingredient: "
                            + ingredient.getName() + " size: " + size.getCm() + "cm in branch: " + branchId));

            extras.add(new ResolvedPizzaExtra(
                    ingredient.getId(),
                    ingredient.getName(),
                    safeQuantity(customization.quantity()),
                    extraPrice.getPrice()
            ));
        }

        return new ResolvedPizzaItem(
                pizza.getId(),
                pizza.getName(),
                size.getId(),
                size.getCm(),
                price.getPrice(),
                extras
        );
    }

    @Override
    public ResolvedMenuItem resolveMenuItem(
            Integer branchId,
            Integer menuItemId,
            List<CatalogCustomizationRequest> customizations
    ) {
        MenuItem menuItem = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Menu item not found with ID: " + menuItemId));

        BranchMenuItemPrice branchPrice = branchMenuItemPriceRepository
                .findByBranchIdAndMenuItemId(branchId, menuItem.getId())
                .orElseThrow(() -> new java.util.NoSuchElementException("Price not found for menu item: "
                        + menuItem.getName() + " in branch: " + branchId));

        MenuCustomizations resolvedCustomizations = resolveMenuItemCustomizations(
                branchId,
                menuItem,
                safeCustomizations(customizations)
        );

        return new ResolvedMenuItem(
                menuItem.getId(),
                menuItem.getName(),
                branchPrice.getPrice(),
                resolvedCustomizations.burgerSelection(),
                resolvedCustomizations.genericExtras()
        );
    }

    private MenuCustomizations resolveMenuItemCustomizations(
            Integer branchId,
            MenuItem menuItem,
            List<CatalogCustomizationRequest> customizations
    ) {
        Optional<BurgerComponentReadRepository.BurgerConfig> burgerConfig =
                burgerComponentReadRepository.findBurgerConfig(menuItem.getId());

        Set<Integer> burgerComponentIds = Set.of();
        ResolvedBurgerSelection burgerSelection = ResolvedBurgerSelection.empty();
        if (burgerConfig.isPresent()) {
            BurgerResolution burgerResolution = resolveBurgerComponents(
                    branchId,
                    menuItem,
                    customizations,
                    burgerConfig.get()
            );
            burgerComponentIds = burgerResolution.componentIds();
            burgerSelection = burgerResolution.selection();
        }

        validateRequiredModifierGroups(menuItem, customizations, burgerComponentIds);

        return new MenuCustomizations(
                burgerSelection,
                resolveGenericMenuItemExtras(customizations, burgerComponentIds)
        );
    }

    private BurgerResolution resolveBurgerComponents(
            Integer branchId,
            MenuItem menuItem,
            List<CatalogCustomizationRequest> customizations,
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
        for (CatalogCustomizationRequest customization : customizations) {
            if (customization == null) {
                continue;
            }
            if (isBurgerExtraComponentCustomization(customization, componentsById.keySet())) {
                selectedExtraQuantities.merge(customization.id(), safeQuantity(customization.quantity()), Integer::sum);
                continue;
            }
            if (isBurgerComponentCustomization(customization, componentsById.keySet())) {
                selectedComponentIds.add(customization.id());
                selectedQuantities.merge(customization.id(), safeQuantity(customization.quantity()), Integer::sum);
            }
        }

        List<BurgerComponentRow> selectedProteins = selectedComponentIds.stream()
                .map(componentId -> findBurgerComponent(componentsById, componentId, "protein").orElse(null))
                .filter(component -> component != null)
                .toList();
        boolean proteinRequired = Boolean.TRUE.equals(burgerConfig.proteinRequired());
        if (proteinRequired && selectedProteins.size() != 1) {
            throw new IllegalArgumentException(menuItem.getName() + " requires exactly one protein choice.");
        }
        if (!proteinRequired && !selectedProteins.isEmpty()) {
            throw new IllegalArgumentException(menuItem.getName() + " does not accept a protein choice.");
        }

        ResolvedBurgerProtein resolvedProtein = null;
        if (proteinRequired) {
            BurgerComponentRow protein = selectedProteins.get(0);
            resolvedProtein = new ResolvedBurgerProtein(
                    protein.componentId(),
                    protein.name(),
                    burgerConfig.proteinQuantityRequired(),
                    toMoney(protein.price())
            );
        }

        List<ResolvedBurgerComponent> removedComponents = new ArrayList<>();
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
            removedComponents.add(new ResolvedBurgerComponent(
                    component.componentId(),
                    component.name(),
                    1,
                    BigDecimal.ZERO
            ));
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

        List<ResolvedBurgerComponent> extraComponents = new ArrayList<>();
        for (Map.Entry<Integer, Integer> selectedExtra : selectedExtraQuantities.entrySet()) {
            BurgerComponentRow component = findBurgerExtraComponent(componentsById, selectedExtra.getKey())
                    .orElseThrow(() -> new java.util.NoSuchElementException(
                            "Burger extra component not found with ID: " + selectedExtra.getKey()
                    ));

            extraComponents.add(new ResolvedBurgerComponent(
                    component.componentId(),
                    component.name(),
                    selectedExtra.getValue(),
                    toMoney(component.price())
            ));
        }

        return new BurgerResolution(
                componentsById.keySet(),
                new ResolvedBurgerSelection(resolvedProtein, removedComponents, extraComponents)
        );
    }

    private void validateRequiredModifierGroups(
            MenuItem menuItem,
            List<CatalogCustomizationRequest> customizations,
            Set<Integer> burgerComponentIds
    ) {
        List<MenuItemModifierGroup> linkedGroups = menuItemModifierGroupRepository.findByMenuItemId(menuItem.getId());
        if (linkedGroups.isEmpty()) {
            return;
        }

        Set<Integer> linkedGroupIds = linkedGroups.stream()
                .map(MenuItemModifierGroup::getGroupId)
                .collect(Collectors.toSet());
        Map<Integer, Integer> selectedCountsByGroup = selectedModifierOptionCounts(customizations, burgerComponentIds, linkedGroupIds);

        for (Integer groupId : linkedGroupIds) {
            ModifierGroup group = modifierGroupRepository.findById(groupId).orElse(null);
            if (group == null) {
                continue;
            }
            int minimum = Math.max(nullToZero(group.getMinSelect()), Boolean.TRUE.equals(group.getRequired()) ? 1 : 0);
            int maximum = nullToZero(group.getMaxSelect());
            int selectedCount = selectedCountsByGroup.getOrDefault(groupId, 0);

            if (minimum > 0 && selectedCount < minimum) {
                throw new IllegalArgumentException(menuItem.getName() + " requires at least "
                        + minimum + " option(s) for " + group.getName() + ".");
            }
            if (maximum > 0 && selectedCount > maximum) {
                throw new IllegalArgumentException(menuItem.getName() + " allows no more than "
                        + maximum + " option(s) for " + group.getName() + ".");
            }
        }
    }

    private Map<Integer, Integer> selectedModifierOptionCounts(
            List<CatalogCustomizationRequest> customizations,
            Set<Integer> burgerComponentIds,
            Set<Integer> linkedGroupIds
    ) {
        Map<Integer, Integer> selectedCountsByGroup = new HashMap<>();
        for (CatalogCustomizationRequest customization : safeCustomizations(customizations)) {
            if (customization == null || isBurgerComponentCustomization(customization, burgerComponentIds)) {
                continue;
            }
            Optional<ModifierOption> option = modifierOptionRepository.findById(customization.id());
            if (option.isEmpty() || !linkedGroupIds.contains(option.get().getGroupId())) {
                continue;
            }
            selectedCountsByGroup.merge(option.get().getGroupId(), 1, Integer::sum);
        }
        return selectedCountsByGroup;
    }

    private List<ResolvedGenericMenuExtra> resolveGenericMenuItemExtras(
            List<CatalogCustomizationRequest> customizations,
            Set<Integer> burgerComponentIds
    ) {
        List<ResolvedGenericMenuExtra> extras = new ArrayList<>();
        for (CatalogCustomizationRequest customization : customizations) {
            if (customization == null) {
                continue;
            }
            if (isBurgerComponentCustomization(customization, burgerComponentIds)) {
                continue;
            }
            extras.add(resolveGenericMenuItemExtra(customization));
        }
        return extras;
    }

    private ResolvedGenericMenuExtra resolveGenericMenuItemExtra(CatalogCustomizationRequest customization) {
        String customizationType = normalizedType(customization);
        if (customizationType.isBlank() || "modifieroption".equals(customizationType)) {
            Optional<ModifierOption> modifierOption = modifierOptionRepository.findById(customization.id());
            if (modifierOption.isPresent()) {
                ModifierOption option = modifierOption.get();
                double extraPrice = option.getAdditionalPrice() == null ? 0.0 : option.getAdditionalPrice().doubleValue();
                return new ResolvedGenericMenuExtra(
                        option.getName(),
                        safeQuantity(customization.quantity()),
                        extraPrice
                );
            }
            if ("modifieroption".equals(customizationType)) {
                throw new java.util.NoSuchElementException("Modifier option not found with ID: " + customization.id());
            }
        }

        if (customizationType.isBlank() || "saladingredient".equals(customizationType)) {
            Optional<SaladIngredient> ingredient = saladIngredientRepository.findById(customization.id());
            if (ingredient.isPresent()) {
                SaladIngredient saladIngredient = ingredient.get();
                return new ResolvedGenericMenuExtra(
                        saladIngredient.getIngredientName(),
                        safeQuantity(customization.quantity()),
                        saladIngredient.getPrice()
                );
            }
            if ("saladingredient".equals(customizationType)) {
                throw new java.util.NoSuchElementException("Salad ingredient not found with ID: " + customization.id());
            }
        }

        throw new java.util.NoSuchElementException("Customization not found with ID: " + customization.id());
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
                .filter(component -> !"protein".equals(component.componentType()))
                .filter(component -> !Boolean.TRUE.equals(component.defaultSelected()))
                .findFirst();
    }

    private boolean isBurgerComponentCustomization(
            CatalogCustomizationRequest customization,
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
                && knownComponentIds.contains(customization.id()));
    }

    private boolean isBurgerExtraComponentCustomization(
            CatalogCustomizationRequest customization,
            Set<Integer> knownComponentIds
    ) {
        String customizationType = normalizedType(customization);
        return "burgerextra".equals(customizationType)
                || "burgerextratopping".equals(customizationType)
                || "burgerextracomponent".equals(customizationType)
                || ("burgercomponentextra".equals(customizationType)
                && customization != null
                && knownComponentIds.contains(customization.id()));
    }

    private String normalizedType(CatalogCustomizationRequest customization) {
        if (customization == null || customization.type() == null) {
            return "";
        }
        return customization.type().trim().replace("_", "").replace("-", "").toLowerCase();
    }

    private List<CatalogCustomizationRequest> safeCustomizations(List<CatalogCustomizationRequest> customizations) {
        return customizations == null ? List.of() : customizations;
    }

    private Integer safeQuantity(Integer quantity) {
        return quantity == null || quantity < 1 ? 1 : quantity;
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
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

    @Override
    public Optional<NamedReference> findMenuItem(Integer id) {
        return menuItemRepository.findById(id)
                .map(item -> new NamedReference(item.getId(), item.getName()));
    }

    @Override
    public Optional<NamedReference> findPizza(Integer id) {
        return pizzaRepository.findById(id)
                .map(pizza -> new NamedReference(pizza.getId(), pizza.getName()));
    }

    @Override
    public Optional<SizeReference> findPizzaSize(Integer id) {
        return pizzaSizeRepository.findById(id)
                .map(size -> new SizeReference(size.getId(), size.getCm()));
    }

    @Override
    public Optional<NamedReference> findIngredient(Integer id) {
        return ingredientRepository.findById(id)
                .map(ingredient -> new NamedReference(ingredient.getId(), ingredient.getName()));
    }

    @Override
    public Optional<NamedReference> findBurgerComponent(Integer id) {
        return burgerComponentReadRepository.findNameById(id)
                .map(name -> new NamedReference(id, name));
    }

    private record MenuCustomizations(
            ResolvedBurgerSelection burgerSelection,
            List<ResolvedGenericMenuExtra> genericExtras
    ) {
    }

    private record BurgerResolution(Set<Integer> componentIds, ResolvedBurgerSelection selection) {
    }
}
