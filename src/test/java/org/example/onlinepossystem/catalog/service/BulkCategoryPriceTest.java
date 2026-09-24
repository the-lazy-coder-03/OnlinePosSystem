package org.example.onlinepossystem.catalog.service;

import org.example.onlinepossystem.branch.api.BranchLookup;
import org.example.onlinepossystem.catalog.entity.*;
import org.example.onlinepossystem.catalog.repository.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class BulkCategoryPriceTest {
    @Test
    void pizzaBulkPricesCanThenBeAdjustedIndividually() {
        var branches = mock(BranchLookup.class);
        var pizzas = mock(PizzaRepository.class);
        var categories = mock(PizzaCategoryRepository.class);
        var sizes = mock(PizzaSizeRepository.class);
        var prices = mock(BranchPizzaPriceRepository.class);
        var allowed = mock(PizzaAllowedSizeRepository.class);
        var service = new PizzaCatalogAdminService(pizzas, categories, sizes, branches, prices,
                allowed, mock(IngredientRepository.class), mock(PizzaDefaultIngredientRepository.class));
        var category = new PizzaCategory(1, "Favourites", 1);
        var size = new PizzaSize(3, 30, 1);
        var first = new Pizza(10, category, "First", "", 1);
        var hidden = new Pizza(11, category, "Hidden", "", 2);
        hidden.setActive(false);
        var a = new BranchPizzaPrice(1, first, size, 80.0);
        var b = new BranchPizzaPrice(1, hidden, size, 90.0);
        when(categories.findById(1)).thenReturn(Optional.of(category));
        when(sizes.findById(3)).thenReturn(Optional.of(size));
        when(prices.findCategoryPrices(1, 1, 3)).thenReturn(List.of(a, b));
        assertThat(service.updatePizzaCategoryPrice(1, 1, 3, "100.00", "admin")).isEqualTo(2);
        assertThat(a.getPrice()).isEqualTo(100.0);
        assertThat(b.getPrice()).isEqualTo(100.0);
        verifyNoInteractions(allowed);
        when(pizzas.findById(10)).thenReturn(Optional.of(first));
        when(prices.findByBranchIdAndPizzaIdAndPizzaSizeId(1, 10, 3)).thenReturn(Optional.of(a));
        when(allowed.existsById(any())).thenReturn(true);
        service.updatePizzaPrice(1, 10, 3, 110.0, "admin");
        assertThat(a.getPrice()).isEqualTo(110.0);
        assertThat(b.getPrice()).isEqualTo(100.0);
    }

    @Test
    void menuBulkSupportsHiddenCombosEmptyResultsAndRejectsInvalidPrices() {
        var items = mock(MenuItemRepository.class);
        var categories = mock(MenuCategoryRepository.class);
        var prices = mock(BranchMenuItemPriceRepository.class);
        var service = new MenuCatalogAdminService(items, categories, mock(BranchLookup.class), prices,
                mock(ModifierGroupRepository.class), mock(MenuItemModifierGroupRepository.class));
        var category = new MenuCategory(1, "Burgers", 1);
        var combo = new MenuItem(301, category, "Combo", "", 1, false, false);
        combo.setActive(false);
        var record = new BranchMenuItemPrice(1, combo, 80.0);
        when(categories.findById(1)).thenReturn(Optional.of(category));
        when(prices.findCategoryPrices(1, 1)).thenReturn(List.of(record));
        assertThat(service.updateMenuItemCategoryPrice(1, 1, "100", "admin")).isEqualTo(1);
        assertThat(record.getPrice()).isEqualTo(100.0);
        when(items.findById(301)).thenReturn(Optional.of(combo));
        when(prices.findByBranchIdAndMenuItemId(1, 301)).thenReturn(Optional.of(record));
        service.updateMenuItemPrice(1, 301, 110.0, "admin");
        assertThat(record.getPrice()).isEqualTo(110.0);
        when(prices.findCategoryPrices(1, 1)).thenReturn(List.of());
        assertThat(service.updateMenuItemCategoryPrice(1, 1, "0.00", "admin")).isZero();
        for (String invalid : List.of("", "-1", "NaN", "Infinity", "1.234", "100000000")) {
            assertThatThrownBy(() -> service.updateMenuItemCategoryPrice(1, 1, invalid, "admin"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(record.getPrice()).isEqualTo(110.0);
    }
}
