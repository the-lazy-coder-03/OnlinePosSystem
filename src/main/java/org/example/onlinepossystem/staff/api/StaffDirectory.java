package org.example.onlinepossystem.staff.api;

import org.example.onlinepossystem.staff.entity.Staff;

public interface StaffDirectory {
    Staff updateOrCreateStaff(String name, String branch, String plainPin, String branchCode);
}
