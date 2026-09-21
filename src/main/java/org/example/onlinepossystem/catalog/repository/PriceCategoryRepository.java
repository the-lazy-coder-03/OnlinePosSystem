package org.example.onlinepossystem.catalog.repository;

import org.example.onlinepossystem.catalog.entity.PriceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PriceCategoryRepository extends JpaRepository<PriceCategory, Integer> {
    @Query("select coalesce(max(c.sortOrder), 0) + 1 from PriceCategory c")
    Integer nextSortOrder();
}
