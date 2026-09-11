package org.example.onlinepossystem.staff.api;

import org.example.onlinepossystem.staff.entity.Staff;

import java.util.Optional;

public interface StaffOperations {
    Optional<String> authenticate(String pin, String code);

    Staff createStaff(String name, String branch, String plainPin, String branchCode);
}
