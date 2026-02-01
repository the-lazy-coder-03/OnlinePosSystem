package org.example.onlinepossystem.repository;

import org.example.onlinepossystem.entity.BurgerTopping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BurgerToppingRepository extends JpaRepository<BurgerTopping, Integer> {
    List<BurgerTopping> findByBurgerId(Integer burgerId);
}
