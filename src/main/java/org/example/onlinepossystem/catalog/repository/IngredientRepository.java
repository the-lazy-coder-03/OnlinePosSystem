package org.example.onlinepossystem.catalog.repository;

import org.example.onlinepossystem.catalog.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IngredientRepository extends JpaRepository<Ingredient, Integer> {
    List<Ingredient> findAllByActiveTrue();
}
