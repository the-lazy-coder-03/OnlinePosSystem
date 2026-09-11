package org.example.onlinepossystem.staff.api;

import java.util.Optional;

public interface StaffOperations {
    Optional<String> authenticate(String pin, String code);

    StaffAccount createStaff(String name, String branch, String plainPin, String branchCode);
}
