package org.example.onlinepossystem.catalog.repository;

import org.example.onlinepossystem.catalog.entity.PizzaCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PizzaCategoryRepository extends JpaRepository<PizzaCategory, Integer> {
}
