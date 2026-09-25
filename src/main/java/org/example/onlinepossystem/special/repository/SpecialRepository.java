package org.example.onlinepossystem.special.repository;

import org.example.onlinepossystem.special.entity.Special;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpecialRepository extends JpaRepository<Special, Long> {
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
    @EntityGraph(attributePaths = {"days", "components", "components.menuItems", "components.pizzas",
            "components.menuCategory", "components.pizzaCategory", "components.pizzaSize",
            "addons", "addons.menuItem", "addons.pizza", "addons.pizzaSize", "branch"})
    Optional<Special> findOneById(Long id);

    @EntityGraph(attributePaths = {"days", "components", "components.menuItems", "components.pizzas",
            "components.menuCategory", "components.pizzaCategory", "components.pizzaSize",
            "addons", "addons.menuItem", "addons.pizza", "addons.pizzaSize", "branch"})
    List<Special> findAllByBranchIdOrderBySortOrderAscIdAsc(Integer branchId);

    @EntityGraph(attributePaths = {"days", "components", "components.menuItems", "components.pizzas",
            "components.menuCategory", "components.pizzaCategory", "components.pizzaSize",
            "addons", "addons.menuItem", "addons.pizza", "addons.pizzaSize", "branch"})
    List<Special> findAllByOrderByBranchIdAscSortOrderAscIdAsc();
}
