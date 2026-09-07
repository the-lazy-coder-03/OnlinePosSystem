package org.example.onlinepossystem.catalog.repository;

import org.example.onlinepossystem.catalog.entity.MenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Integer> {
    List<MenuCategory> findAllByActiveTrueOrderBySortOrderAsc();
}
