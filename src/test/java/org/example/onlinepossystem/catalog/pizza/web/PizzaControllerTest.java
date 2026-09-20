package org.example.onlinepossystem.catalog.pizza.web;

import org.example.onlinepossystem.catalog.pizza.dto.PizzaCard;
import org.example.onlinepossystem.catalog.pizza.dto.PizzaCategorySplit;
import org.example.onlinepossystem.catalog.pizza.service.PizzaService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class PizzaControllerTest {

    @Test
    void listsDefaultToppingsOnPizzaCards() throws Exception {
        PizzaService pizzaService = mock(PizzaService.class);
        PizzaCard card = new PizzaCard(
                101,
                "Margherita",
                1,
                "Favourite",
                23,
                new BigDecimal("80.00"),
                List.of("Tomato", "Cheese")
        );
        when(pizzaService.listPizzasByCategory(1))
                .thenReturn(new PizzaCategorySplit(List.of(card), List.of()));
        MockMvc mockMvc = standaloneSetup(new PizzaController(pizzaService)).build();

        mockMvc.perform(get("/api/branches/1/pizzas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favouritePizzas[0].defaultToppings[0]").value("Tomato"))
                .andExpect(jsonPath("$.favouritePizzas[0].defaultToppings[1]").value("Cheese"));
    }
}
