package org.example.onlinepossystem.repository;

import org.example.onlinepossystem.entity.PizzaDefaultIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PizzaDefaultIngredientRepository extends JpaRepository<PizzaDefaultIngredient, PizzaDefaultIngredient.PizzaDefaultIngredientId> {
    List<PizzaDefaultIngredient> findByPizzaId(Integer pizzaId);
}
