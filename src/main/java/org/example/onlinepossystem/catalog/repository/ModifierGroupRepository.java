package org.example.onlinepossystem.catalog.repository;

import org.example.onlinepossystem.catalog.entity.ModifierGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ModifierGroupRepository extends JpaRepository<ModifierGroup, Integer> {
}
