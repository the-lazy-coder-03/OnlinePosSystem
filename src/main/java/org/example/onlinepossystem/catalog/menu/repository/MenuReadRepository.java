package org.example.onlinepossystem.catalog.menu.repository;

import org.example.onlinepossystem.catalog.entity.MenuItem;
import org.example.onlinepossystem.catalog.menu.dto.MenuItemRow;
import org.example.onlinepossystem.catalog.menu.dto.ModifierGroupRow;
import org.example.onlinepossystem.catalog.menu.dto.ModifierOptionRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MenuReadRepository extends JpaRepository<MenuItem, Integer> {

    @Query("""
            select new org.example.onlinepossystem.catalog.menu.dto.MenuItemRow(
                mi.id,
                mi.name,
                mi.description,
                mc.id,
                mc.name,
                coalesce(bmp.price, 0)
            )
            from MenuItem mi
            join mi.category mc
            left join BranchMenuItemPrice bmp
                on bmp.menuItem = mi and bmp.id.branchId = :branchId
            where mi.active = true
              and mc.active = true
            order by mc.sortOrder, mi.sortOrder, mi.name
            """)
    List<MenuItemRow> findMenuItemsForBranch(@Param("branchId") Integer branchId);

    @Query("""
            select new org.example.onlinepossystem.catalog.menu.dto.ModifierGroupRow(
                mimg.menuItemId,
                mg.id,
                mg.name,
                mg.required,
                mg.minSelect,
                mg.maxSelect
            )
            from MenuItemModifierGroup mimg, ModifierGroup mg
            where mimg.groupId = mg.id
              and mimg.menuItemId in :menuItemIds
            order by mg.name
            """)
    List<ModifierGroupRow> findModifierGroupsForMenuItems(@Param("menuItemIds") List<Integer> menuItemIds);

    @Query("""
            select new org.example.onlinepossystem.catalog.menu.dto.ModifierOptionRow(
                mo.groupId,
                mo.id,
                mo.name,
                mo.menuItemId,
                mo.additionalPrice
            )
            from ModifierOption mo
            where mo.groupId in :groupIds
            order by mo.groupId,
                case when mo.groupId = 5 then mo.id else 0 end,
                mo.name
            """)
    List<ModifierOptionRow> findModifierOptionsForGroups(@Param("groupIds") List<Integer> groupIds);
}
