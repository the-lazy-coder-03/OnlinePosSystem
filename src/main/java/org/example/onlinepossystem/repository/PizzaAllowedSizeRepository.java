package org.example.onlinepossystem.repository;

import org.example.onlinepossystem.entity.PizzaAllowedSize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PizzaAllowedSizeRepository extends JpaRepository<PizzaAllowedSize, PizzaAllowedSize.PizzaAllowedSizeId> {
    List<PizzaAllowedSize> findByPizzaId(Integer pizzaId);
}
