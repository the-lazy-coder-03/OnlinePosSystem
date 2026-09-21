package org.example.onlinepossystem.staff.repository;

import org.example.onlinepossystem.staff.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public interface StaffRepository extends JpaRepository<Staff, Long> {

    // Find staff by branch to fetch all staff for PIN validation
    Optional<Staff> findByBranch(String branch);

    // Find staff by branch code
    Optional<Staff> findByBranchCode(String branchCode);
}
