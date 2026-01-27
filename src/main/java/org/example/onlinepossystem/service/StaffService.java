package org.example.onlinepossystem.service;

import org.example.onlinepossystem.entity.Staff;
import org.example.onlinepossystem.repository.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StaffService {

    @Autowired
    private StaffRepository staffRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

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
    public Staff createStaff(String name, String branch, String plainPin, String branchCode) {
        String hashedPin = plainPin != null ? passwordEncoder.encode(plainPin) : null;
        Staff staff = new Staff(name, branch, hashedPin, branchCode);
        return staffRepository.save(staff);
    }

    public Staff createStaff(String name, String branch, String plainPin) {
        return createStaff(name, branch, plainPin, null);
    }

    /**
     * Update or create a staff member.
     * If a staff member with the same branch already exists, update their details.
     */
    public Staff updateOrCreateStaff(String name, String branch, String plainPin, String branchCode) {
        return staffRepository.findByBranch(branch)
                .map(existingStaff -> {
                    existingStaff.setName(name);
                    if (plainPin != null && !plainPin.isEmpty()) {
                        existingStaff.setPinHash(passwordEncoder.encode(plainPin));
                    }
                    existingStaff.setBranchCode(branchCode);
                    return staffRepository.save(existingStaff);
                })
                .orElseGet(() -> createStaff(name, branch, plainPin, branchCode));
    }

    /**
     * Get all staff members (for admin purposes).
     */
    public List<Staff> getAllStaff() {
        return staffRepository.findAll();
    }
}
