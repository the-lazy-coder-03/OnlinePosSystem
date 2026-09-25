package org.example.onlinepossystem.special.service;

import jakarta.persistence.EntityManager;
import org.example.onlinepossystem.branch.api.BranchLookup;
import org.example.onlinepossystem.branch.entity.Branch;
import org.example.onlinepossystem.catalog.api.SpecialCatalogAccess;
import org.example.onlinepossystem.catalog.entity.MenuCategory;
import org.example.onlinepossystem.catalog.entity.MenuItem;
import org.example.onlinepossystem.catalog.entity.Pizza;
import org.example.onlinepossystem.catalog.entity.PizzaCategory;
import org.example.onlinepossystem.catalog.entity.PizzaSize;
import org.example.onlinepossystem.ordering.dto.OrderRequestDTO;
import org.example.onlinepossystem.ordering.service.MenuOrderItemFactory;
import org.example.onlinepossystem.ordering.service.PizzaOrderItemFactory;
import org.example.onlinepossystem.special.dto.SpecialQuoteRequest;
import org.example.onlinepossystem.special.entity.Special;
import org.example.onlinepossystem.special.entity.SpecialComponent;
import org.example.onlinepossystem.special.repository.SpecialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpecialServiceTest {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Africa/Johannesburg");
    private static final Clock MONDAY = Clock.fixed(Instant.parse("2026-09-21T10:00:00Z"), BUSINESS_ZONE);

    @Mock private SpecialRepository specials;
    @Mock private SpecialCatalogAccess catalog;
    @Mock private BranchLookup branches;
    @Mock private EntityManager entityManager;
    @Mock private MenuOrderItemFactory menuFactory;
    @Mock private PizzaOrderItemFactory pizzaFactory;

    private SpecialService service;
    private Special special;
    private SpecialComponent component;
    private MenuItem first;
    private MenuItem second;

    @BeforeEach
    void setUp() {
        service = service(MONDAY);
        MenuCategory category = new MenuCategory(1, "Burgers", 1);
        first = new MenuItem(10, category, "First Burger", null, 1, false, false);
        second = new MenuItem(11, category, "Second Burger", null, 2, false, false);

        special = baseSpecial(1L, new Branch(2, "Uitzicht"), Set.of(1), new BigDecimal("225.00"));
        component = component(10L, "burgers", "Two burgers", "MENU_ITEM", 2);
        component.setMenuItems(new LinkedHashSet<>(List.of(first, second)));
        special.addComponent(component);

        lenient().when(specials.findOneById(1L)).thenReturn(Optional.of(special));
        lenient().when(catalog.findMenuItem(10)).thenReturn(Optional.of(first));
        lenient().when(catalog.findMenuItem(11)).thenReturn(Optional.of(second));
        lenient().when(catalog.menuPrice(2, 10)).thenReturn(Optional.of(new BigDecimal("80.00")));
        lenient().when(catalog.menuPrice(2, 11)).thenReturn(Optional.of(new BigDecimal("90.00")));
    }

    @Test
    void quotesTheConfiguredBundlePriceAndMultipliesOnlyAtBundleLevel() {
        var quote = service.quote(2, 1L, request(2, selection(1, item(10)), selection(2, item(11))));

        assertThat(quote.basePrice()).isEqualByComparingTo("225.00");
        assertThat(quote.customizationTotal()).isZero();
        assertThat(quote.addonTotal()).isZero();
        assertThat(quote.lineTotal()).isEqualByComparingTo("450.00");
        assertThat(quote.selections()).extracting(value -> value.productName())
                .containsExactly("First Burger", "Second Burger");
    }

    @Test
    void rejectsWrongBranchWrongDayAndOutOfRangeDates() {
        assertThatThrownBy(() -> service.quote(1, 1L, request(1, selection(1, item(10)), selection(2, item(11)))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("not available");

        SpecialService tuesday = service(Clock.fixed(Instant.parse("2026-09-22T10:00:00Z"), BUSINESS_ZONE));
        assertThatThrownBy(() -> tuesday.quote(2, 1L, request(1, selection(1, item(10)), selection(2, item(11)))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("not available");

        special.setStartsOn(java.time.LocalDate.of(2026, 9, 22));
        assertThatThrownBy(() -> service.quote(2, 1L, request(1, selection(1, item(10)), selection(2, item(11)))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("not available");
    }

    @Test
    void enforcesExactSlotsIndexesAndRepeatPolicy() {
        assertThatThrownBy(() -> service.quote(2, 1L, request(1, selection(1, item(10)))))
                .hasMessageContaining("exactly 2");
        assertThatThrownBy(() -> service.quote(2, 1L, request(1, selection(1, item(10)), selection(1, item(11)))))
                .hasMessageContaining("Invalid selection slots");
        assertThatThrownBy(() -> service.quote(2, 1L, request(1, selection(1, item(10)), selection(2, item(10)))))
                .hasMessageContaining("does not allow duplicate");

        component.setAllowRepeats(true);
        assertThat(service.quote(2, 1L, request(1, selection(1, item(10)), selection(2, item(10)))).lineTotal())
                .isEqualByComparingTo("225.00");
    }

    @Test
    void rejectsUnconfiguredProductsAndForbiddenCustomizations() {
        MenuItem other = new MenuItem(12, first.getCategory(), "Other Burger", null, 3, false, false);
        when(catalog.findMenuItem(12)).thenReturn(Optional.of(other));
        assertThatThrownBy(() -> service.quote(2, 1L, request(1, selection(1, item(10)), selection(2, item(12)))))
                .hasMessageContaining("does not qualify");

        OrderRequestDTO.OrderItemRequestDTO customized = item(10);
        OrderRequestDTO.CustomizationRequestDTO extra = new OrderRequestDTO.CustomizationRequestDTO();
        extra.setId(99);
        extra.setQuantity(1);
        extra.setType("extra");
        customized.setCustomizations(List.of(extra));
        assertThatThrownBy(() -> service.quote(2, 1L, request(1, selection(1, customized), selection(2, item(11)))))
                .hasMessageContaining("cannot be customized");
    }

    @Test
    void rejectsPizzaSelectionsThatDoNotUseTheMandatedSize() {
        PizzaCategory category = new PizzaCategory(1, "Favourite", 1);
        Pizza pizza = new Pizza(20, category, "Favourite Pizza", null, 1);
        PizzaSize large = new PizzaSize(3, 30, 3);
        Special pizzaSpecial = baseSpecial(2L, new Branch(2, "Uitzicht"), Set.of(1), new BigDecimal("225.00"));
        SpecialComponent pizzaComponent = component(20L, "pizzas", "Large pizzas", "PIZZA", 1);
        pizzaComponent.setPizzaSize(large);
        pizzaComponent.setPizzas(new LinkedHashSet<>(List.of(pizza)));
        pizzaSpecial.addComponent(pizzaComponent);
        when(specials.findOneById(2L)).thenReturn(Optional.of(pizzaSpecial));
        when(catalog.findPizza(20)).thenReturn(Optional.of(pizza));

        OrderRequestDTO.OrderItemRequestDTO wrongSize = new OrderRequestDTO.OrderItemRequestDTO();
        wrongSize.setPizzaId(20);
        wrongSize.setPizzaSizeId(2);
        wrongSize.setQuantity(1);
        wrongSize.setCustomizations(List.of());
        var request = new SpecialQuoteRequest(1, List.of(new SpecialQuoteRequest.Selection(20L, 1, wrongSize)), List.of());

        assertThatThrownBy(() -> service.quote(2, 2L, request))
                .hasMessageContaining("configured pizza size");
    }

    @Test
    void publicOptionsExcludeProductsWithoutABranchPrice() {
        when(specials.findAllByBranchIdOrderBySortOrderAscIdAsc(2)).thenReturn(List.of(special));
        when(catalog.menuPrice(2, 11)).thenReturn(Optional.empty());

        assertThat(service.available(2)).singleElement().satisfies(view ->
                assertThat(view.components().get(0).options()).extracting(option -> option.name())
                        .containsExactly("First Burger"));
    }

    private SpecialService service(Clock clock) {
        return new SpecialService(specials, catalog, branches, entityManager, menuFactory, pizzaFactory, clock);
    }

    private Special baseSpecial(Long id, Branch branch, Set<Integer> days, BigDecimal price) {
        Special value = new Special();
        value.setId(id);
        value.setCode("TEST-SPECIAL-" + id);
        value.setBranch(branch);
        value.setName("Test special");
        value.setBundlePrice(price);
        value.setActive(true);
        value.setDays(new LinkedHashSet<>(days));
        return value;
    }

    private SpecialComponent component(Long id, String code, String label, String type, int quantity) {
        SpecialComponent value = new SpecialComponent();
        value.setId(id);
        value.setCode(code);
        value.setLabel(label);
        value.setProductType(type);
        value.setQuantity(quantity);
        value.setSelectionMode("CUSTOMER_CHOICE");
        value.setAllowRepeats(false);
        value.setAllowCustomization(false);
        return value;
    }

    private SpecialQuoteRequest request(int quantity, SpecialQuoteRequest.Selection... selections) {
        return new SpecialQuoteRequest(quantity, List.of(selections), List.of());
    }

    private SpecialQuoteRequest.Selection selection(int index, OrderRequestDTO.OrderItemRequestDTO item) {
        return new SpecialQuoteRequest.Selection(component.getId(), index, item);
    }

    private OrderRequestDTO.OrderItemRequestDTO item(int menuItemId) {
        OrderRequestDTO.OrderItemRequestDTO value = new OrderRequestDTO.OrderItemRequestDTO();
        value.setMenuItemId(menuItemId);
        value.setQuantity(1);
        value.setCustomizations(List.of());
        return value;
    }
}
