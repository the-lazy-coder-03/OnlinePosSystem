package org.example.onlinepossystem.catalog.repository;

import org.example.onlinepossystem.catalog.entity.MenuItemModifierGroup;
import org.example.onlinepossystem.catalog.entity.MenuItemModifierGroupId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuItemModifierGroupRepository extends JpaRepository<MenuItemModifierGroup, MenuItemModifierGroupId> {
    List<MenuItemModifierGroup> findByMenuItemId(Integer menuItemId);
}
