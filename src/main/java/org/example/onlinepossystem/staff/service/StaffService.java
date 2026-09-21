package org.example.onlinepossystem.staff.service;

import org.example.onlinepossystem.staff.api.StaffDirectory;
import org.example.onlinepossystem.staff.api.StaffAccount;
import org.example.onlinepossystem.staff.api.StaffOperations;
import org.example.onlinepossystem.staff.entity.Staff;
import org.example.onlinepossystem.staff.repository.StaffRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@org.springframework.transaction.annotation.Transactional
public class StaffService implements StaffDirectory, StaffOperations {

    private final StaffRepository staffRepository;

    private final PasswordEncoder passwordEncoder;

    public StaffService(StaffRepository staffRepository,
                        @Qualifier("staffPasswordEncoder") PasswordEncoder passwordEncoder) {
        this.staffRepository = staffRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Optional<String> authenticate(String pin, String code) {
        if (code != null && !code.isEmpty()) {
            return Optional.ofNullable(authenticateByCode(code));
        }
        if (pin != null && !pin.isEmpty()) {
            return Optional.ofNullable(authenticateStaff(pin));
        }
        return Optional.empty();
    }

    /**
     * Authenticate staff by checking PIN against all staff records.
     * Returns the branch name if a matching PIN is found, otherwise null.
     *
     * SECURITY: Uses BCrypt to compare entered PIN against stored hashed PINs.
     * Never stores or compares plain text PINs.
     */
    public String authenticateStaff(String enteredPin) {
        // Fetch all staff records
        List<Staff> allStaff = staffRepository.findAll();

        // Check entered PIN against each staff member's hashed PIN
        for (Staff staff : allStaff) {
            if (staff.getPinHash() != null && passwordEncoder.matches(enteredPin, staff.getPinHash())) {
                // PIN matches - return the branch
                return staff.getBranch();
            }
        }

        // No match found
        return null;
    }

    /**
     * Authenticate staff by 16-character branch code.
     * Returns the branch name if the code matches, otherwise null.
     */
    public String authenticateByCode(String code) {
        if (code == null || code.length() != 16) {
            return null;
        }

        return staffRepository.findByBranchCode(code)
                .map(Staff::getBranch)
                .orElse(null);
    }

    /**
     * Create a new staff member with a hashed PIN and optional branch code.
     */
    @Override
    public StaffAccount createStaff(String name, String branch, String plainPin, String branchCode) {
        String hashedPin = plainPin != null ? passwordEncoder.encode(plainPin) : null;
        Staff staff = new Staff(name, branch, hashedPin, branchCode);
        return toAccount(staffRepository.save(staff));
    }

    public StaffAccount createStaff(String name, String branch, String plainPin) {
        return createStaff(name, branch, plainPin, null);
    }

    /**
     * Update or create a staff member.
     * If a staff member with the same branch already exists, update their details.
     */
    @Override
    public void updateOrCreateStaff(String name, String branch, String plainPin, String branchCode) {
        staffRepository.findByBranch(branch)
                .map(existingStaff -> {
                    existingStaff.setName(name);
                    if (plainPin != null && !plainPin.isEmpty()) {
                        existingStaff.setPinHash(passwordEncoder.encode(plainPin));
                    }
                    existingStaff.setBranchCode(branchCode);
                    return staffRepository.save(existingStaff);
                })
                .orElseGet(() -> {
                    String hashedPin = plainPin != null ? passwordEncoder.encode(plainPin) : null;
                    return staffRepository.save(new Staff(name, branch, hashedPin, branchCode));
                });
    }

    private StaffAccount toAccount(Staff staff) {
        return new StaffAccount(staff.getId(), staff.getName(), staff.getBranch(), staff.getBranchCode());
    }
}
