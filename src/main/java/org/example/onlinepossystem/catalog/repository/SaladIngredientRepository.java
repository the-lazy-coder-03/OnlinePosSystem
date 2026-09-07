package org.example.onlinepossystem.catalog.repository;

import org.example.onlinepossystem.catalog.entity.SaladIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SaladIngredientRepository extends JpaRepository<SaladIngredient, Integer> {
    List<SaladIngredient> findBySaladId(Integer saladId);
}
