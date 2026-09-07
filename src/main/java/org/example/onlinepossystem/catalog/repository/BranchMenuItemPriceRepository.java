package org.example.onlinepossystem.catalog.repository;

import org.example.onlinepossystem.catalog.entity.BranchMenuItemPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchMenuItemPriceRepository extends JpaRepository<BranchMenuItemPrice, BranchMenuItemPrice.BranchMenuItemPriceId> {
    List<BranchMenuItemPrice> findByBranchId(Integer branchId);
    Optional<BranchMenuItemPrice> findByBranchIdAndMenuItemId(Integer branchId, Integer menuItemId);
}
