package org.example.onlinepossystem.menu.repository;

import org.example.onlinepossystem.entity.MenuItem;
import org.example.onlinepossystem.menu.dto.MenuItemRow;
import org.example.onlinepossystem.menu.dto.ModifierGroupRow;
import org.example.onlinepossystem.menu.dto.ModifierOptionRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MenuReadRepository extends JpaRepository<MenuItem, Integer> {

    @Query("""
            select new org.example.onlinepossystem.menu.dto.MenuItemRow(
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
                on bmp.menuItem = mi and bmp.branch.id = :branchId
            order by mc.name, mi.name
            """)
    List<MenuItemRow> findMenuItemsForBranch(@Param("branchId") Integer branchId);

    @Query("""
            select new org.example.onlinepossystem.menu.dto.ModifierGroupRow(
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
            select new org.example.onlinepossystem.menu.dto.ModifierOptionRow(
                mo.groupId,
                mo.id,
                mo.name,
                mo.menuItemId,
                coalesce(bmp.price, 0)
            )
            from ModifierOption mo
            left join BranchMenuItemPrice bmp
                on bmp.menuItem.id = mo.menuItemId and bmp.branch.id = :branchId
            where mo.groupId in :groupIds
            order by mo.name
            """)
    List<ModifierOptionRow> findModifierOptionsForGroups(
            @Param("branchId") Integer branchId,
            @Param("groupIds") List<Integer> groupIds
    );
}
