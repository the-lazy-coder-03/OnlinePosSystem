package org.example.onlinepossystem.catalog.menu.service;

import org.example.onlinepossystem.catalog.menu.dto.BurgerComponentRow;
import org.example.onlinepossystem.catalog.menu.dto.BurgerToppingItem;
import org.example.onlinepossystem.catalog.menu.dto.MenuItemDetail;
import org.example.onlinepossystem.catalog.menu.dto.MenuItemRow;
import org.example.onlinepossystem.catalog.menu.dto.ModifierGroupItem;
import org.example.onlinepossystem.catalog.menu.dto.ModifierGroupRow;
import org.example.onlinepossystem.catalog.menu.dto.ModifierOptionItem;
import org.example.onlinepossystem.catalog.menu.dto.ModifierOptionRow;
import org.example.onlinepossystem.catalog.menu.repository.BurgerComponentReadRepository;
import org.example.onlinepossystem.catalog.menu.repository.MenuReadRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MenuService {
    private final MenuReadRepository menuReadRepository;
    private final BurgerComponentReadRepository burgerComponentReadRepository;

    public MenuService(MenuReadRepository menuReadRepository, BurgerComponentReadRepository burgerComponentReadRepository) {
        this.menuReadRepository = menuReadRepository;
        this.burgerComponentReadRepository = burgerComponentReadRepository;
    }

    public List<MenuItemDetail> getMenuForBranch(Integer branchId) {
        List<MenuItemRow> menuRows = menuReadRepository.findMenuItemsForBranch(branchId);
        if (menuRows.isEmpty()) {
            return List.of();
        }

        List<Integer> menuItemIds = menuRows.stream()
                .map(MenuItemRow::menuItemId)
                .distinct()
                .toList();

        List<ModifierGroupRow> groupRows = menuReadRepository.findModifierGroupsForMenuItems(menuItemIds);
        Map<Integer, List<ModifierGroupItem>> groupsByMenuItem = new LinkedHashMap<>();

        if (!groupRows.isEmpty()) {
            List<Integer> groupIds = groupRows.stream()
                    .map(ModifierGroupRow::groupId)
                    .distinct()
                    .toList();

            List<ModifierOptionRow> optionRows = menuReadRepository.findModifierOptionsForGroups(groupIds);
            Map<Integer, List<ModifierOptionItem>> optionsByGroup = optionRows.stream()
                    .collect(Collectors.groupingBy(
                            ModifierOptionRow::groupId,
                            LinkedHashMap::new,
                            Collectors.mapping(
                                    row -> new ModifierOptionItem(
                                            row.optionId(),
                                            row.name(),
                                            row.menuItemId(),
                                            toBigDecimal(row.price())
                                    ),
                                    Collectors.toList()
                            )
                    ));

            for (ModifierGroupRow row : groupRows) {
                List<ModifierOptionItem> options = optionsByGroup.getOrDefault(row.groupId(), List.of());
                ModifierGroupItem groupItem = new ModifierGroupItem(
                        row.groupId(),
                        row.name(),
                        Boolean.TRUE.equals(row.required()),
                        row.minSelect(),
                        row.maxSelect(),
                        options
                );
                groupsByMenuItem.computeIfAbsent(row.menuItemId(), key -> new ArrayList<>()).add(groupItem);
            }
        }

        List<BurgerComponentRow> burgerComponents = burgerComponentReadRepository.findComponentsForMenuItems(branchId, menuItemIds);
        Map<Integer, List<BurgerToppingItem>> toppingsByMenuItem = burgerComponents.stream()
                .collect(Collectors.groupingBy(
                        BurgerComponentRow::burgerId,
                        LinkedHashMap::new,
                        Collectors.mapping(
                                component -> new BurgerToppingItem(
                                        component.componentId(),
                                        component.name(),
                                        Boolean.TRUE.equals(component.defaultSelected()),
                                        toBigDecimal(component.price()),
                                        component.componentType(),
                                        Boolean.TRUE.equals(component.removable()),
                                        component.proteinQuantityRequired()
                                ),
                                Collectors.toList()
                        )
                ));

        List<MenuItemDetail> results = new ArrayList<>();
        for (MenuItemRow row : menuRows) {
            List<ModifierGroupItem> groups = groupsByMenuItem.getOrDefault(row.menuItemId(), List.of());
            List<BurgerToppingItem> burgerToppingItems = toppingsByMenuItem.getOrDefault(row.menuItemId(), List.of());
            results.add(new MenuItemDetail(
                    row.menuItemId(),
                    row.name(),
                    row.description(),
                    row.categoryId(),
                    row.categoryName(),
                    toBigDecimal(row.price()),
                    row.is300ml(),
                    row.is2l(),
                    groups,
                    burgerToppingItems
            ));
        }
        return results;
    }

    private BigDecimal toBigDecimal(Double value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(value);
    }

    private BigDecimal toBigDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
