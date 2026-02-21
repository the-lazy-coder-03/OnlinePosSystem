package org.example.onlinepossystem.pizza.service;

import org.example.onlinepossystem.pizza.dto.PizzaCard;
import org.example.onlinepossystem.pizza.dto.PizzaCardRow;
import org.example.onlinepossystem.pizza.dto.PizzaCategorySplit;
import org.example.onlinepossystem.pizza.dto.PizzaDetail;
import org.example.onlinepossystem.pizza.dto.PizzaSizePriceRow;
import org.example.onlinepossystem.pizza.dto.PriceQuoteResponse;
import org.example.onlinepossystem.pizza.dto.SizePrice;
import org.example.onlinepossystem.pizza.dto.ToppingItem;
import org.example.onlinepossystem.pizza.dto.ToppingPrice;
import org.example.onlinepossystem.pizza.dto.ToppingRow;
import org.example.onlinepossystem.pizza.repository.PizzaReadRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class PizzaService {
    private final PizzaReadRepository pizzaReadRepository;

    public PizzaService(PizzaReadRepository pizzaReadRepository) {
        this.pizzaReadRepository = pizzaReadRepository;
    }

    public PizzaCategorySplit listPizzasByCategory(Integer branchId) {
        List<PizzaCard> favouritePizzas = mapPizzaCards(
                pizzaReadRepository.findPizzaCardsByBranchAndCategory(branchId, 1)
        );
        List<PizzaCard> supremePizzas = mapPizzaCards(
                pizzaReadRepository.findPizzaCardsByBranchAndCategory(branchId, 2)
        );
        return new PizzaCategorySplit(favouritePizzas, supremePizzas);
    }

    public PizzaDetail getPizzaDetail(Integer branchId, Integer pizzaId, Integer sizeCmParam) {
        List<PizzaSizePriceRow> sizeRows = pizzaReadRepository.findSizePricesForPizza(branchId, pizzaId);
        if (sizeRows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pizza not found for branch");
        }

        String name = sizeRows.get(0).name();
        List<SizePrice> availableSizes = sizeRows.stream()
                .map(row -> new SizePrice(row.sizeCm(), toBigDecimal(row.basePrice())))
                .toList();

        Integer selectedSizeCm = sizeCmParam != null ? sizeCmParam : sizeRows.get(0).sizeCm();
        SizePrice selectedSize = availableSizes.stream()
                .filter(size -> Objects.equals(size.sizeCm(), selectedSizeCm))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Size not available for pizza"));

        List<ToppingRow> toppingRows = pizzaReadRepository.findToppingsForPizzaAndSize(branchId, pizzaId, selectedSizeCm);

        List<ToppingItem> allToppings = toppingRows.stream()
                .map(row -> new ToppingItem(row.toppingId(), row.name()))
                .toList();

        List<ToppingItem> defaultToppings = toppingRows.stream()
                .filter(row -> row.defaultSortOrder() != null)
                .sorted(Comparator.comparing(ToppingRow::defaultSortOrder))
                .map(row -> new ToppingItem(row.toppingId(), row.name()))
                .toList();

        List<ToppingPrice> extraPrices = toppingRows.stream()
                .map(row -> new ToppingPrice(row.toppingId(), toBigDecimal(row.extraPrice())))
                .toList();

        return new PizzaDetail(
                pizzaId,
                name,
                availableSizes,
                selectedSize.basePrice(),
                defaultToppings,
                allToppings,
                extraPrices
        );
    }

    public PriceQuoteResponse quotePrice(Integer branchId, Integer pizzaId, Integer sizeCm, List<Integer> selectedToppingIds) {
        Double basePriceRaw = pizzaReadRepository.findBasePrice(branchId, pizzaId, sizeCm)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Price not found for pizza and size"));

        List<Integer> uniqueToppings = (selectedToppingIds == null ? List.<Integer>of() : selectedToppingIds)
                .stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Double toppingsTotalRaw = uniqueToppings.isEmpty()
                ? 0.0
                : pizzaReadRepository.sumExtraPrices(branchId, pizzaId, sizeCm, uniqueToppings);

        BigDecimal basePrice = toBigDecimal(basePriceRaw);
        BigDecimal toppingsTotal = toBigDecimal(toppingsTotalRaw);
        BigDecimal total = basePrice.add(toppingsTotal);

        return new PriceQuoteResponse(pizzaId, sizeCm, basePrice, toppingsTotal, total);
    }

    private BigDecimal toBigDecimal(Double value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(value);
    }

    private List<PizzaCard> mapPizzaCards(List<PizzaCardRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        Map<Integer, PizzaCard> cards = new LinkedHashMap<>();
        for (PizzaCardRow row : rows) {
            if (!cards.containsKey(row.pizzaId())) {
                cards.put(row.pizzaId(), new PizzaCard(
                        row.pizzaId(),
                        row.name(),
                        row.pizzaCategoryId(),
                        row.pizzaCategoryName(),
                        row.sizeCm(),
                        toBigDecimal(row.basePrice())
                ));
            }
        }
        return new ArrayList<>(cards.values());
    }
}
