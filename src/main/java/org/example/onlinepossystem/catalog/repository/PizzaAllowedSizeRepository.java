package org.example.onlinepossystem.catalog.repository;

import org.example.onlinepossystem.catalog.entity.PizzaAllowedSize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PizzaAllowedSizeRepository extends JpaRepository<PizzaAllowedSize, PizzaAllowedSize.PizzaAllowedSizeId> {
}
