package org.example.onlinepossystem.staff.api;

import java.util.Optional;

public interface StaffOperations {
    // Unused legacy PIN/code authentication API; /api/staff/login is retired.
    // Optional<String> authenticate(String pin, String code);

    StaffAccount createStaff(String name, String branch, String plainPin, String branchCode);
}
