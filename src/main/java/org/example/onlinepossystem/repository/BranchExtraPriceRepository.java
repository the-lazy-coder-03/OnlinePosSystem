package org.example.onlinepossystem.repository;

import org.example.onlinepossystem.entity.BranchExtraPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BranchExtraPriceRepository extends JpaRepository<BranchExtraPrice, BranchExtraPrice.BranchExtraPriceId> {
    List<BranchExtraPrice> findByBranchId(Integer branchId);
}
