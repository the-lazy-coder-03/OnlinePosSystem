package org.example.onlinepossystem.catalog.repository;

import org.example.onlinepossystem.catalog.entity.PizzaAllowedSize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PizzaAllowedSizeRepository extends JpaRepository<PizzaAllowedSize, PizzaAllowedSize.PizzaAllowedSizeId> {
    List<PizzaAllowedSize> findByPizzaId(Integer pizzaId);
}
