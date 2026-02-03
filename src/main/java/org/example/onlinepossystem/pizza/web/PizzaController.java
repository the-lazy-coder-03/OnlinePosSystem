package org.example.onlinepossystem.pizza.web;

import org.example.onlinepossystem.pizza.dto.PizzaCategorySplit;
import org.example.onlinepossystem.pizza.dto.PizzaDetail;
import org.example.onlinepossystem.pizza.dto.PriceQuoteRequest;
import org.example.onlinepossystem.pizza.dto.PriceQuoteResponse;
import org.example.onlinepossystem.pizza.service.PizzaService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/branches/{branchId}/pizzas")
public class PizzaController {
    private final PizzaService pizzaService;

    public PizzaController(PizzaService pizzaService) {
        this.pizzaService = pizzaService;
    }

    @GetMapping
    public PizzaCategorySplit listPizzas(@PathVariable Integer branchId) {
        return pizzaService.listPizzasByCategory(branchId);
    }

    @GetMapping("/{pizzaId}")
    public PizzaDetail getPizzaDetail(
            @PathVariable Integer branchId,
            @PathVariable Integer pizzaId,
            @RequestParam(value = "sizeCm", required = false) Integer sizeCm
    ) {
        return pizzaService.getPizzaDetail(branchId, pizzaId, sizeCm);
    }

    @PostMapping("/{pizzaId}/quote")
    public PriceQuoteResponse quotePrice(
            @PathVariable Integer branchId,
            @PathVariable Integer pizzaId,
            @RequestBody PriceQuoteRequest request
    ) {
        if (request.sizeCm() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sizeCm is required");
        }
        return pizzaService.quotePrice(branchId, pizzaId, request.sizeCm(), request.selectedToppingIds());
    }
}
