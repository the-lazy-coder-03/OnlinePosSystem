package org.example.onlinepossystem.repository;

import org.example.onlinepossystem.entity.MenuItemModifierGroup;
import org.example.onlinepossystem.entity.MenuItemModifierGroupId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuItemModifierGroupRepository extends JpaRepository<MenuItemModifierGroup, MenuItemModifierGroupId> {
    List<MenuItemModifierGroup> findByMenuItemId(Integer menuItemId);
}
