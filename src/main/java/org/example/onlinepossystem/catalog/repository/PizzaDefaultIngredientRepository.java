package org.example.onlinepossystem.catalog.repository;

import org.example.onlinepossystem.catalog.entity.PizzaDefaultIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PizzaDefaultIngredientRepository extends JpaRepository<PizzaDefaultIngredient, PizzaDefaultIngredient.PizzaDefaultIngredientId> {
    List<PizzaDefaultIngredient> findByPizzaId(Integer pizzaId);
}
