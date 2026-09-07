package org.example.onlinepossystem.branch.api;

import org.example.onlinepossystem.branch.entity.Branch;

import java.util.List;
import java.util.Optional;

public interface BranchLookup {
    Optional<Branch> findByName(String name);

    Branch requireByName(String name);

    Branch requireById(Integer id);

    List<Branch> findAll();

    boolean hasAnyBranches();

    Branch ensureBranch(Integer id, String name);
}
