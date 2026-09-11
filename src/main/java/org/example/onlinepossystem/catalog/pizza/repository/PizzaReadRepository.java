package org.example.onlinepossystem.catalog.pizza.repository;

import org.example.onlinepossystem.catalog.entity.Pizza;
import org.example.onlinepossystem.catalog.pizza.dto.PizzaCardRow;
import org.example.onlinepossystem.catalog.pizza.dto.PizzaSizePriceRow;
import org.example.onlinepossystem.catalog.pizza.dto.ToppingRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PizzaReadRepository extends JpaRepository<Pizza, Integer> {

    @Query("""
            select new org.example.onlinepossystem.catalog.pizza.dto.PizzaCardRow(
                p.id,
                p.name,
                p.category.id,
                p.category.name,
                s.cm,
                bpp.price,
                p.sortOrder,
                s.sortOrder
            )
            from BranchPizzaPrice bpp
            join bpp.pizza p
            join bpp.pizzaSize s
            join PizzaAllowedSize pas on pas.pizza = p and pas.pizzaSize = s
            where bpp.id.branchId = :branchId
              and p.active = true
              and s.active = true
            order by p.sortOrder, s.sortOrder
            """)
    List<PizzaCardRow> findPizzaCardsByBranch(@Param("branchId") Integer branchId);

    @Query("""
            select new org.example.onlinepossystem.catalog.pizza.dto.PizzaCardRow(
                p.id,
                p.name,
                p.category.id,
                p.category.name,
                s.cm,
                bpp.price,
                p.sortOrder,
                s.sortOrder
            )
            from BranchPizzaPrice bpp
            join bpp.pizza p
            join bpp.pizzaSize s
            join PizzaAllowedSize pas on pas.pizza = p and pas.pizzaSize = s
            where bpp.id.branchId = :branchId
              and p.category.id = :categoryId
              and p.active = true
              and s.active = true
            order by p.sortOrder, s.sortOrder
            """)
    List<PizzaCardRow> findPizzaCardsByBranchAndCategory(
            @Param("branchId") Integer branchId,
            @Param("categoryId") Integer categoryId
    );

    @Query("""
            select new org.example.onlinepossystem.catalog.pizza.dto.PizzaSizePriceRow(
                p.id,
                p.name,
                s.cm,
                bpp.price,
                s.sortOrder
            )
            from BranchPizzaPrice bpp
            join bpp.pizza p
            join bpp.pizzaSize s
            join PizzaAllowedSize pas on pas.pizza = p and pas.pizzaSize = s
            where bpp.id.branchId = :branchId
              and p.id = :pizzaId
              and p.active = true
              and s.active = true
            order by s.sortOrder
            """)
    List<PizzaSizePriceRow> findSizePricesForPizza(@Param("branchId") Integer branchId, @Param("pizzaId") Integer pizzaId);

    @Query("""
            select new org.example.onlinepossystem.catalog.pizza.dto.ToppingRow(
                i.id,
                i.name,
                pdi.sortOrder,
                bep.price
            )
            from Ingredient i
            left join PizzaDefaultIngredient pdi
                on pdi.ingredient = i and pdi.pizza.id = :pizzaId
            join BranchExtraPrice bep
                on bep.priceCategory = i.priceCategory and bep.id.branchId = :branchId
            where i.active = true
              and bep.pizzaSize.cm = :sizeCm
              and bep.pizzaSize.active = true
            order by i.priceCategory.sortOrder, i.name
            """)
    List<ToppingRow> findToppingsForPizzaAndSize(
            @Param("branchId") Integer branchId,
            @Param("pizzaId") Integer pizzaId,
            @Param("sizeCm") Integer sizeCm
    );

    @Query("""
            select bpp.price
            from BranchPizzaPrice bpp
            where bpp.id.branchId = :branchId
              and bpp.pizza.id = :pizzaId
              and bpp.pizzaSize.cm = :sizeCm
            """)
    Optional<Double> findBasePrice(
            @Param("branchId") Integer branchId,
            @Param("pizzaId") Integer pizzaId,
            @Param("sizeCm") Integer sizeCm
    );

    @Query("""
            select coalesce(sum(bep.price), 0)
            from Ingredient i
            join BranchExtraPrice bep
                on bep.priceCategory = i.priceCategory and bep.id.branchId = :branchId
            where i.active = true
              and i.id in :ingredientIds
              and i.id not in (
                select pdi.ingredient.id
                from PizzaDefaultIngredient pdi
                where pdi.pizza.id = :pizzaId
              )
              and bep.pizzaSize.cm = :sizeCm
            """)
    Double sumExtraPrices(
            @Param("branchId") Integer branchId,
            @Param("pizzaId") Integer pizzaId,
            @Param("sizeCm") Integer sizeCm,
            @Param("ingredientIds") List<Integer> ingredientIds
    );
}
