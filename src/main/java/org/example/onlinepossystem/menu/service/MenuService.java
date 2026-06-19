package org.example.onlinepossystem.menu.service;

import org.example.onlinepossystem.entity.BurgerTopping;
import org.example.onlinepossystem.menu.dto.BurgerToppingItem;
import org.example.onlinepossystem.menu.dto.MenuItemDetail;
import org.example.onlinepossystem.menu.dto.MenuItemRow;
import org.example.onlinepossystem.menu.dto.ModifierGroupItem;
import org.example.onlinepossystem.menu.dto.ModifierGroupRow;
import org.example.onlinepossystem.menu.dto.ModifierOptionItem;
import org.example.onlinepossystem.menu.dto.ModifierOptionRow;
import org.example.onlinepossystem.menu.repository.MenuReadRepository;
import org.example.onlinepossystem.repository.BurgerToppingRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class MenuService {
    private final MenuReadRepository menuReadRepository;
    private final BurgerToppingRepository burgerToppingRepository;

    public MenuService(MenuReadRepository menuReadRepository, BurgerToppingRepository burgerToppingRepository) {
        this.menuReadRepository = menuReadRepository;
        this.burgerToppingRepository = burgerToppingRepository;
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

            List<ModifierOptionRow> optionRows = menuReadRepository.findModifierOptionsForGroups(branchId, groupIds);
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

        List<BurgerTopping> burgerToppings = burgerToppingRepository.findByBurgerIds(menuItemIds);
        Map<Integer, List<BurgerToppingItem>> toppingsByMenuItem = burgerToppings.stream()
                .filter(topping -> topping.getBurger() != null && topping.getBurger().getId() != null)
                .collect(Collectors.groupingBy(
                        topping -> topping.getBurger().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(
                                topping -> new BurgerToppingItem(
                                        topping.getId(),
                                        topping.getToppingName(),
                                        topping.isDefault(),
                                        toBigDecimal(topping.getPrice())
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
}
