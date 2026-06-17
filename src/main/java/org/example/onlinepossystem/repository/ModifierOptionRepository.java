package org.example.onlinepossystem.repository;

import org.example.onlinepossystem.entity.ModifierOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModifierOptionRepository extends JpaRepository<ModifierOption, Integer> {
    List<ModifierOption> findByGroupId(Integer groupId);
}
