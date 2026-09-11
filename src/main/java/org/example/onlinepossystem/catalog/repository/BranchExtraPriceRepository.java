package org.example.onlinepossystem.catalog.repository;

import org.example.onlinepossystem.catalog.entity.BranchExtraPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BranchExtraPriceRepository extends JpaRepository<BranchExtraPrice, BranchExtraPrice.BranchExtraPriceId> {
    @Query("select price from BranchExtraPrice price where price.id.branchId = :branchId")
    List<BranchExtraPrice> findByBranchId(@Param("branchId") Integer branchId);
}
