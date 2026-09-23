package org.example.onlinepossystem.catalog.repository;

import org.example.onlinepossystem.catalog.entity.BranchPizzaPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchPizzaPriceRepository extends JpaRepository<BranchPizzaPrice, BranchPizzaPrice.BranchPizzaPriceId> {
    @Query("select price from BranchPizzaPrice price where price.id.branchId = :branchId and price.pizza.category.id = :categoryId and price.id.pizzaSizeId = :pizzaSizeId")
    List<BranchPizzaPrice> findCategoryPrices(@Param("branchId") Integer branchId, @Param("categoryId") Integer categoryId, @Param("pizzaSizeId") Integer pizzaSizeId);

    @Query("select price from BranchPizzaPrice price where price.id.branchId = :branchId")
    List<BranchPizzaPrice> findByBranchId(@Param("branchId") Integer branchId);

    @Query("select price from BranchPizzaPrice price where price.id.branchId = :branchId and price.id.pizzaId = :pizzaId and price.id.pizzaSizeId = :pizzaSizeId")
    Optional<BranchPizzaPrice> findByBranchIdAndPizzaIdAndPizzaSizeId(
            @Param("branchId") Integer branchId,
            @Param("pizzaId") Integer pizzaId,
            @Param("pizzaSizeId") Integer pizzaSizeId
    );
}
