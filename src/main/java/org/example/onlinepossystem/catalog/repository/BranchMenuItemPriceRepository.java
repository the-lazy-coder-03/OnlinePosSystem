package org.example.onlinepossystem.catalog.repository;

import org.example.onlinepossystem.catalog.entity.BranchMenuItemPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchMenuItemPriceRepository extends JpaRepository<BranchMenuItemPrice, BranchMenuItemPrice.BranchMenuItemPriceId> {
    @Query("select price from BranchMenuItemPrice price where price.id.branchId = :branchId and price.menuItem.category.id = :categoryId")
    List<BranchMenuItemPrice> findCategoryPrices(@Param("branchId") Integer branchId, @Param("categoryId") Integer categoryId);

    @Query("select price from BranchMenuItemPrice price where price.id.branchId = :branchId")
    List<BranchMenuItemPrice> findByBranchId(@Param("branchId") Integer branchId);

    @Query("select price from BranchMenuItemPrice price where price.id.branchId = :branchId and price.id.menuItemId = :menuItemId")
    Optional<BranchMenuItemPrice> findByBranchIdAndMenuItemId(
            @Param("branchId") Integer branchId,
            @Param("menuItemId") Integer menuItemId
    );
}
