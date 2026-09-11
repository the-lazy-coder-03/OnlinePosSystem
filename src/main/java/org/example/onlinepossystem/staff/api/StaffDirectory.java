package org.example.onlinepossystem.staff.api;

public interface StaffDirectory {
    void updateOrCreateStaff(String name, String branch, String plainPin, String branchCode);
}
