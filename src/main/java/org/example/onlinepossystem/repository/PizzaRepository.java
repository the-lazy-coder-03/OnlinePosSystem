package org.example.onlinepossystem.repository;

import org.example.onlinepossystem.entity.Pizza;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PizzaRepository extends JpaRepository<Pizza, Integer> {
    List<Pizza> findAllByActiveTrue();
}
