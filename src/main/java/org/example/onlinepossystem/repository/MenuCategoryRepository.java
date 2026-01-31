package org.example.onlinepossystem.repository;

import org.example.onlinepossystem.entity.MenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Integer> {
    List<MenuCategory> findAllByActiveTrueOrderBySortOrderAsc();
}
