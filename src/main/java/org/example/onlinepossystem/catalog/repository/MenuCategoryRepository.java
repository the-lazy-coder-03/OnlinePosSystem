package org.example.onlinepossystem.catalog.repository;

import org.example.onlinepossystem.catalog.entity.MenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Integer> {
    @Query("select coalesce(max(c.sortOrder), 0) + 1 from MenuCategory c")
    Integer nextSortOrder();
}
