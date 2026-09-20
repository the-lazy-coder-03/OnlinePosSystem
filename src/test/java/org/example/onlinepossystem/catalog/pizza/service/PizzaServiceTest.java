package org.example.onlinepossystem.catalog.pizza.service;

import org.example.onlinepossystem.catalog.entity.BranchPizzaBaseOptionPrice;
import org.example.onlinepossystem.catalog.entity.PizzaBaseOption;
import org.example.onlinepossystem.catalog.entity.PizzaSize;
import org.example.onlinepossystem.catalog.pizza.dto.PizzaSizePriceRow;
import org.example.onlinepossystem.catalog.pizza.repository.PizzaReadRepository;
import org.example.onlinepossystem.catalog.repository.BranchPizzaBaseOptionPriceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PizzaServiceTest {
    @Mock
    private PizzaReadRepository pizzaReadRepository;

    @Mock
    private BranchPizzaBaseOptionPriceRepository baseOptionPriceRepository;

    private PizzaService pizzaService;

    @BeforeEach
    void setUp() {
        pizzaService = new PizzaService(pizzaReadRepository, baseOptionPriceRepository);
    }

    @Test
    void returnsAvailableBaseOptionsAndIncludesSelectedSurchargeInQuote() {
        BranchPizzaBaseOptionPrice baseOptionPrice = baseOptionPrice();
        when(pizzaReadRepository.findSizePricesForPizza(1, 10))
                .thenReturn(List.of(new PizzaSizePriceRow(10, "Margherita", 30, 120.00, 1)));
        when(pizzaReadRepository.findToppingsForPizzaAndSize(1, 10, 30)).thenReturn(List.of());
        when(baseOptionPriceRepository.findAvailableOptions(1, 30)).thenReturn(List.of(baseOptionPrice));
        when(pizzaReadRepository.findBasePrice(1, 10, 30)).thenReturn(Optional.of(120.00));

        assertThat(pizzaService.getPizzaDetail(1, 10, 30).baseOptions())
                .singleElement()
                .satisfies(option -> {
                    assertThat(option.pizzaBaseOptionId()).isEqualTo(1);
                    assertThat(option.name()).isEqualTo("Wheat and Gluten Free Base");
                    assertThat(option.extraPrice()).isEqualByComparingTo("36.00");
                });

        var quote = pizzaService.quotePrice(1, 10, 30, List.of(), 1);
        assertThat(quote.basePrice()).isEqualByComparingTo("120.00");
        assertThat(quote.baseOptionTotal()).isEqualByComparingTo("36.00");
        assertThat(quote.total()).isEqualByComparingTo("156.00");
    }

    @Test
    void rejectsBaseOptionThatIsUnavailableForBranchAndSize() {
        when(pizzaReadRepository.findBasePrice(2, 10, 23)).thenReturn(Optional.of(90.00));
        when(baseOptionPriceRepository.findAvailableOptions(2, 23)).thenReturn(List.of());

        assertThatThrownBy(() -> pizzaService.quotePrice(2, 10, 23, List.of(), 1))
                .isInstanceOf(InvalidPizzaSelectionException.class)
                .hasMessageContaining("not available");
    }

    private BranchPizzaBaseOptionPrice baseOptionPrice() {
        PizzaBaseOption option = new PizzaBaseOption(1, "Wheat and Gluten Free Base");
        PizzaSize size = new PizzaSize(3, 30, 3);
        return new BranchPizzaBaseOptionPrice(1, option, size, 36.00);
    }
}
