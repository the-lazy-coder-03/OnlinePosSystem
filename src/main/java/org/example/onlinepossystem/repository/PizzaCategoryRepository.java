package org.example.onlinepossystem.repository;

import org.example.onlinepossystem.entity.PizzaCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PizzaCategoryRepository extends JpaRepository<PizzaCategory, Integer> {
}
