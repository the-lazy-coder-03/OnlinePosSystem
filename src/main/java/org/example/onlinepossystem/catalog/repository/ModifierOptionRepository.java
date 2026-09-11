package org.example.onlinepossystem.catalog.repository;

import org.example.onlinepossystem.catalog.entity.ModifierOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ModifierOptionRepository extends JpaRepository<ModifierOption, Integer> {
}
