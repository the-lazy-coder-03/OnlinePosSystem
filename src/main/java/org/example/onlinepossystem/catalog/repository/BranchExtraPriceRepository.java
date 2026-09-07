package org.example.onlinepossystem.catalog.repository;

import org.example.onlinepossystem.catalog.entity.BranchExtraPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BranchExtraPriceRepository extends JpaRepository<BranchExtraPrice, BranchExtraPrice.BranchExtraPriceId> {
    List<BranchExtraPrice> findByBranchId(Integer branchId);
}
