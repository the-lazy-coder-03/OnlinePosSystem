package org.example.onlinepossystem.catalog.repository;

import org.example.onlinepossystem.catalog.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Integer> {
    List<MenuItem> findAllByActiveTrue();
}
