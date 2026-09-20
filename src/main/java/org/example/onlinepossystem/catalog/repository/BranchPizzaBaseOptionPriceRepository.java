package org.example.onlinepossystem.catalog.repository;

import org.example.onlinepossystem.catalog.entity.BranchPizzaBaseOptionPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BranchPizzaBaseOptionPriceRepository extends JpaRepository<
        BranchPizzaBaseOptionPrice,
        BranchPizzaBaseOptionPrice.BranchPizzaBaseOptionPriceId
> {
    @Query("""
            select price
            from BranchPizzaBaseOptionPrice price
            where price.id.branchId = :branchId
              and price.pizzaSize.cm = :sizeCm
              and price.pizzaBaseOption.active = true
            order by price.pizzaBaseOption.name
            """)
    List<BranchPizzaBaseOptionPrice> findAvailableOptions(
            @Param("branchId") Integer branchId,
            @Param("sizeCm") Integer sizeCm
    );
}
