package org.example.onlinepossystem.catalog.repository;

import org.example.onlinepossystem.catalog.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Integer> {
    List<MenuItem> findAllByActiveTrue();

    @Query("select coalesce(max(m.sortOrder), 0) + 1 from MenuItem m where m.category.id = :categoryId")
    Integer nextSortOrderForCategory(@Param("categoryId") Integer categoryId);
}
