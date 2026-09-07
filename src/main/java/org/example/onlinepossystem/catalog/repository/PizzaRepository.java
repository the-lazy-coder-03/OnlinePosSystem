package org.example.onlinepossystem.catalog.repository;

import org.example.onlinepossystem.catalog.entity.Pizza;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PizzaRepository extends JpaRepository<Pizza, Integer> {
    List<Pizza> findAllByActiveTrue();
}
