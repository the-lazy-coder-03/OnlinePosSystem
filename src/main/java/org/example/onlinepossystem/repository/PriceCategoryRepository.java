package org.example.onlinepossystem.repository;

import org.example.onlinepossystem.entity.PriceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PriceCategoryRepository extends JpaRepository<PriceCategory, Integer> {
}
