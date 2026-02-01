package org.example.onlinepossystem.repository;

import org.example.onlinepossystem.entity.BranchPizzaPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchPizzaPriceRepository extends JpaRepository<BranchPizzaPrice, BranchPizzaPrice.BranchPizzaPriceId> {
    List<BranchPizzaPrice> findByBranchId(Integer branchId);
    Optional<BranchPizzaPrice> findByBranchIdAndPizzaIdAndPizzaSizeId(Integer branchId, Integer pizzaId, Integer pizzaSizeId);
}
