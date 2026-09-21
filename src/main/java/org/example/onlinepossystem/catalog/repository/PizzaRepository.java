package org.example.onlinepossystem.catalog.repository;

import org.example.onlinepossystem.catalog.entity.Pizza;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PizzaRepository extends JpaRepository<Pizza, Integer> {
    List<Pizza> findAllByActiveTrue();

    @Query("select coalesce(max(p.sortOrder), 0) + 1 from Pizza p where p.category.id = :categoryId")
    Integer nextSortOrderForCategory(@Param("categoryId") Integer categoryId);
}
