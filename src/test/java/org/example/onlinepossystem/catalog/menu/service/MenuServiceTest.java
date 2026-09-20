package org.example.onlinepossystem.catalog.menu.service;

import org.example.onlinepossystem.catalog.menu.dto.BurgerComponentRow;
import org.example.onlinepossystem.catalog.menu.dto.MenuItemDetail;
import org.example.onlinepossystem.catalog.menu.dto.MenuItemRow;
import org.example.onlinepossystem.catalog.menu.dto.ModifierGroupRow;
import org.example.onlinepossystem.catalog.menu.dto.ModifierOptionRow;
import org.example.onlinepossystem.catalog.menu.repository.BurgerComponentReadRepository;
import org.example.onlinepossystem.catalog.menu.repository.MenuReadRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MenuServiceTest {

    private final MenuReadRepository menuReadRepository = mock(MenuReadRepository.class);
    private final BurgerComponentReadRepository burgerComponentReadRepository = mock(BurgerComponentReadRepository.class);
    private final MenuService menuService = new MenuService(menuReadRepository, burgerComponentReadRepository);

    @Test
    void keepsRepositoryMenuOrderAndIncludesZeroPricedItems() {
        when(menuReadRepository.findMenuItemsForBranch(1)).thenReturn(List.of(
                new MenuItemRow(201, "Cheese Burger", "Burger", 1, "Burgers", 82.00),
                new MenuItemRow(401, "Chicken Alfredo Medium", "Medium portion", 3, "Pastas", 0.00),
                new MenuItemRow(101, "Coke 300ml", "Coke can", 4, "Cool Drinks", 20.00),
                new MenuItemRow(601, "Chips Small", "Small chips", 5, "Sides", 48.00),
                new MenuItemRow(501, "Ribs 400g", "Ribs with one side choice", 8, "Ribs", 159.00)
        ));
        when(menuReadRepository.findModifierGroupsForMenuItems(List.of(201, 401, 101, 601, 501)))
                .thenReturn(List.of());
        when(burgerComponentReadRepository.findComponentsForMenuItems(1, List.of(201, 401, 101, 601, 501)))
                .thenReturn(List.<BurgerComponentRow>of());

        List<MenuItemDetail> menu = menuService.getMenuForBranch(1);

        assertThat(menu).extracting(MenuItemDetail::name)
                .containsExactly(
                        "Cheese Burger",
                        "Chicken Alfredo Medium",
                        "Coke 300ml",
                        "Chips Small",
                        "Ribs 400g"
                );
        MenuItemDetail pasta = menu.stream()
                .filter(item -> item.menuItemId().equals(401))
                .findFirst()
                .orElseThrow();
        MenuItemDetail chips = menu.stream()
                .filter(item -> item.menuItemId().equals(601))
                .findFirst()
                .orElseThrow();
        assertThat(pasta.price()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(chips.price()).isEqualByComparingTo("48.00");
    }

    @Test
    void returnsRibSideChoiceBeforeRibExtrasAndChipExtras() {
        when(menuReadRepository.findMenuItemsForBranch(1)).thenReturn(List.of(
                new MenuItemRow(501, "Ribs 400g", "Ribs with one side choice", 8, "Ribs", 159.00)
        ));
        when(menuReadRepository.findModifierGroupsForMenuItems(List.of(501))).thenReturn(List.of(
                new ModifierGroupRow(501, 2, "Choose your side", true, 1, 1),
                new ModifierGroupRow(501, 3, "Rib extras", false, 0, 1),
                new ModifierGroupRow(501, 4, "Chip extras", false, 0, 2)
        ));
        when(menuReadRepository.findModifierOptionsForGroups(List.of(2, 3, 4))).thenReturn(List.of(
                new ModifierOptionRow(2, 9, "Chips", 602, BigDecimal.ZERO),
                new ModifierOptionRow(2, 10, "Onion Rings", 604, BigDecimal.ZERO),
                new ModifierOptionRow(2, 11, "Salad", 605, BigDecimal.ZERO),
                new ModifierOptionRow(3, 12, "Extra Rib Sauce", null, BigDecimal.ZERO),
                new ModifierOptionRow(4, 13, "Extra Chip Sauce", null, BigDecimal.ZERO),
                new ModifierOptionRow(4, 14, "Extra Rib Sauce", null, BigDecimal.ZERO)
        ));
        when(burgerComponentReadRepository.findComponentsForMenuItems(1, List.of(501)))
                .thenReturn(List.<BurgerComponentRow>of());

        MenuItemDetail ribs = menuService.getMenuForBranch(1).get(0);

        assertThat(ribs.modifierGroups()).extracting(group -> group.name())
                .containsExactly("Choose your side", "Rib extras", "Chip extras");
        assertThat(ribs.modifierGroups().get(0).options()).extracting(option -> option.name())
                .containsExactly("Chips", "Onion Rings", "Salad");
    }
}
