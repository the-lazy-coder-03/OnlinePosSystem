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

        /* Unused legacy PIN/code authentication; /api/staff/login is retired.
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

    public String authenticateStaff(String enteredPin) {
        List<Staff> allStaff = staffRepository.findAll();
        for (Staff staff : allStaff) {
            if (staff.getPinHash() != null && passwordEncoder.matches(enteredPin, staff.getPinHash())) {
                return staff.getBranch();
            }
        }
        return null;
    }

    public String authenticateByCode(String code) {
        if (code == null || code.length() != 16) {
            return null;
        }

        return staffRepository.findByBranchCode(code)
                .map(Staff::getBranch)
                .orElse(null);
    }
    */

    /**
     * Create a new staff member with a hashed PIN and optional branch code.
     */
    @Override
    public StaffAccount createStaff(String name, String branch, String plainPin, String branchCode) {
        String hashedPin = plainPin != null ? passwordEncoder.encode(plainPin) : null;
        Staff staff = new Staff(name, branch, hashedPin, branchCode);
        return toAccount(staffRepository.save(staff));
    }

    /* Unused convenience overload; active callers use the branch-code form.
    public StaffAccount createStaff(String name, String branch, String plainPin) {
        return createStaff(name, branch, plainPin, null);
    }
    */

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
