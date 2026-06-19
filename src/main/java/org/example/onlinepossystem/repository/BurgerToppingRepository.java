package org.example.onlinepossystem.repository;

import org.example.onlinepossystem.entity.BurgerTopping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BurgerToppingRepository extends JpaRepository<BurgerTopping, Integer> {
    List<BurgerTopping> findByBurgerId(Integer burgerId);

    @Query("""
            select bt
            from BurgerTopping bt
            join fetch bt.burger b
            where b.id in :burgerIds
            order by b.id, bt.isDefault desc, bt.toppingName
            """)
    List<BurgerTopping> findByBurgerIds(@Param("burgerIds") List<Integer> burgerIds);
}
