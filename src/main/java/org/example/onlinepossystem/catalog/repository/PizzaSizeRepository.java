package org.example.onlinepossystem.catalog.repository;

import org.example.onlinepossystem.catalog.entity.PizzaSize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PizzaSizeRepository extends JpaRepository<PizzaSize, Integer> {
    Optional<PizzaSize> findByCm(Integer cm);

    @Query("select coalesce(max(s.sortOrder), 0) + 1 from PizzaSize s")
    Integer nextSortOrder();
}
