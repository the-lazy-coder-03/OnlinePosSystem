package org.example.onlinepossystem.catalog.service;

import org.example.onlinepossystem.branch.api.BranchLookup;
import org.example.onlinepossystem.branch.api.BranchView;
import org.example.onlinepossystem.catalog.entity.BranchPizzaPrice;
import org.example.onlinepossystem.catalog.entity.Ingredient;
import org.example.onlinepossystem.catalog.entity.Pizza;
import org.example.onlinepossystem.catalog.entity.PizzaAllowedSize;
import org.example.onlinepossystem.catalog.entity.PizzaCategory;
import org.example.onlinepossystem.catalog.entity.PizzaDefaultIngredient;
import org.example.onlinepossystem.catalog.entity.PizzaSize;
import org.example.onlinepossystem.catalog.repository.BranchPizzaPriceRepository;
import org.example.onlinepossystem.catalog.repository.IngredientRepository;
import org.example.onlinepossystem.catalog.repository.PizzaAllowedSizeRepository;
import org.example.onlinepossystem.catalog.repository.PizzaCategoryRepository;
import org.example.onlinepossystem.catalog.repository.PizzaDefaultIngredientRepository;
import org.example.onlinepossystem.catalog.repository.PizzaRepository;
import org.example.onlinepossystem.catalog.repository.PizzaSizeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

@Service
public class PizzaCatalogAdminService {
    private static final Logger logger = LoggerFactory.getLogger(PizzaCatalogAdminService.class);

    private final PizzaRepository pizzaRepository;
    private final PizzaCategoryRepository pizzaCategoryRepository;
    private final PizzaSizeRepository pizzaSizeRepository;
    private final BranchLookup branchLookup;
    private final BranchPizzaPriceRepository branchPizzaPriceRepository;
    private final PizzaAllowedSizeRepository pizzaAllowedSizeRepository;
    private final IngredientRepository ingredientRepository;
    private final PizzaDefaultIngredientRepository pizzaDefaultIngredientRepository;

    public PizzaCatalogAdminService(
            PizzaRepository pizzaRepository,
            PizzaCategoryRepository pizzaCategoryRepository,
            PizzaSizeRepository pizzaSizeRepository,
            BranchLookup branchLookup,
            BranchPizzaPriceRepository branchPizzaPriceRepository,
            PizzaAllowedSizeRepository pizzaAllowedSizeRepository,
            IngredientRepository ingredientRepository,
            PizzaDefaultIngredientRepository pizzaDefaultIngredientRepository
    ) {
        this.pizzaRepository = pizzaRepository;
        this.pizzaCategoryRepository = pizzaCategoryRepository;
        this.pizzaSizeRepository = pizzaSizeRepository;
        this.branchLookup = branchLookup;
        this.branchPizzaPriceRepository = branchPizzaPriceRepository;
        this.pizzaAllowedSizeRepository = pizzaAllowedSizeRepository;
        this.ingredientRepository = ingredientRepository;
        this.pizzaDefaultIngredientRepository = pizzaDefaultIngredientRepository;
    }

    @Transactional
    public void savePizza(Integer id, String name, Integer categoryId, String description, Integer sortOrder,
                          List<Integer> ingredientIds, Map<String, String> parameters, String actor) {
        Pizza pizza = id == null ? new Pizza() : pizzaRepository.findById(id).orElseGet(Pizza::new);
        if (pizza.getId() == null) {
            pizza.setId(nextId(pizzaRepository.findAll(), Pizza::getId));
        }
        PizzaCategory category = pizzaCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Pizza category not found with ID: " + categoryId));
        pizza.setCategory(category);
        pizza.setName(cleanText(name));
        pizza.setDescription(cleanText(description));
        pizza.setSortOrder(sortOrder == null ? 0 : sortOrder);
        pizza.setActive(parameters.containsKey("active"));

        Pizza savedPizza = pizzaRepository.save(pizza);
        replaceDefaultIngredients(savedPizza, ingredientIds);
        savePriceMatrix(savedPizza, parameters);
        logger.info("Admin action=savePizza pizzaId={} admin={}", savedPizza.getId(), actorName(actor));
    }

    public void deletePizza(Integer id, String actor) {
        pizzaRepository.deleteById(id);
        logger.info("Admin action=deletePizza pizzaId={} admin={}", id, actorName(actor));
    }

    @Transactional
    public void updatePizzaPrice(Integer branchId, Integer pizzaId, Integer pizzaSizeId, Double price, String actor) {
        branchLookup.requireById(branchId);
        Pizza pizza = pizzaRepository.findById(pizzaId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Pizza not found with ID: " + pizzaId));
        PizzaSize pizzaSize = pizzaSizeRepository.findById(pizzaSizeId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Pizza size not found with ID: " + pizzaSizeId));
        savePrice(branchId, pizza, pizzaSize, price);
        logger.info("Admin action=updatePizzaPrice branchId={} pizzaId={} sizeId={} admin={}",
                branchId, pizzaId, pizzaSizeId, actorName(actor));
    }

    private void replaceDefaultIngredients(Pizza pizza, List<Integer> ingredientIds) {
        pizzaDefaultIngredientRepository.deleteAll(pizzaDefaultIngredientRepository.findByPizzaId(pizza.getId()));
        if (ingredientIds == null) {
            return;
        }
        for (int index = 0; index < ingredientIds.size(); index++) {
            Ingredient ingredient = ingredientRepository.findById(ingredientIds.get(index)).orElse(null);
            if (ingredient != null) {
                pizzaDefaultIngredientRepository.save(new PizzaDefaultIngredient(pizza, ingredient, true, 1, index));
            }
        }
    }

    private void savePriceMatrix(Pizza pizza, Map<String, String> parameters) {
        for (BranchView branch : branchLookup.findAll()) {
            for (PizzaSize size : pizzaSizeRepository.findAll()) {
                String key = "pizzaPrice_" + branch.id() + "_" + size.getId();
                parsePrice(parameters.get(key)).ifPresent(price -> savePrice(branch.id(), pizza, size, price));
            }
        }
    }

    private void savePrice(Integer branchId, Pizza pizza, PizzaSize size, Double price) {
        BranchPizzaPrice branchPrice = branchPizzaPriceRepository
                .findByBranchIdAndPizzaIdAndPizzaSizeId(branchId, pizza.getId(), size.getId())
                .orElse(new BranchPizzaPrice(branchId, pizza, size, price));
        branchPrice.setPrice(price);
        branchPizzaPriceRepository.save(branchPrice);

        PizzaAllowedSize.PizzaAllowedSizeId allowedSizeId =
                new PizzaAllowedSize.PizzaAllowedSizeId(pizza.getId(), size.getId());
        if (!pizzaAllowedSizeRepository.existsById(allowedSizeId)) {
            pizzaAllowedSizeRepository.save(new PizzaAllowedSize(pizza, size));
        }
    }

    private <T> Integer nextId(List<T> items, Function<T, Integer> idExtractor) {
        return items.stream().map(idExtractor).filter(Objects::nonNull)
                .mapToInt(Integer::intValue).max().orElse(0) + 1;
    }

    private Optional<Double> parsePrice(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        double price = Double.parseDouble(raw);
        if (price < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
        return Optional.of(price);
    }

    private String cleanText(String value) {
        return value == null ? null : value.trim();
    }

    private String actorName(String actor) {
        return actor == null || actor.isBlank() ? "unknown" : actor;
    }
}
