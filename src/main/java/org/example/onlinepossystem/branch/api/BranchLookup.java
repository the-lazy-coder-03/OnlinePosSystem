package org.example.onlinepossystem.branch.api;

import java.util.List;
import java.util.Optional;

public interface BranchLookup {
    Optional<BranchView> findByName(String name);

    BranchView requireByName(String name);

    BranchView requireById(Integer id);

    List<BranchView> findAll();

    boolean hasAnyBranches();

    BranchView ensureBranch(Integer id, String name);
}
