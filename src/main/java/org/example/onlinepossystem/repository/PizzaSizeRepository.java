package org.example.onlinepossystem.repository;

import org.example.onlinepossystem.entity.PizzaSize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PizzaSizeRepository extends JpaRepository<PizzaSize, Integer> {
}
