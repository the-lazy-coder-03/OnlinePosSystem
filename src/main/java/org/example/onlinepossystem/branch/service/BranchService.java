package org.example.onlinepossystem.branch.service;

import org.example.onlinepossystem.branch.api.BranchLookup;
import org.example.onlinepossystem.branch.entity.Branch;
import org.example.onlinepossystem.branch.repository.BranchRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BranchService implements BranchLookup {
    private final BranchRepository branchRepository;

    public BranchService(BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }

    @Override
    public Optional<Branch> findByName(String name) {
        return branchRepository.findByName(name);
    }

    @Override
    public Branch requireByName(String name) {
        return findByName(name)
                .orElseThrow(() -> new java.util.NoSuchElementException("Branch not found: " + name));
    }

    @Override
    public Branch requireById(Integer id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Branch not found with ID: " + id));
    }

    @Override
    public List<Branch> findAll() {
        return branchRepository.findAll();
    }

    @Override
    public boolean hasAnyBranches() {
        return branchRepository.count() > 0;
    }

    @Override
    public Branch ensureBranch(Integer id, String name) {
        return branchRepository.findByName(name)
                .orElseGet(() -> branchRepository.save(new Branch(id, name)));
    }
}
