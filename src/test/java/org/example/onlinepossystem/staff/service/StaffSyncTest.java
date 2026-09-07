package org.example.onlinepossystem.staff.service;

import org.example.onlinepossystem.staff.entity.Staff;
import org.example.onlinepossystem.staff.repository.StaffRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class StaffSyncTest {

    @Autowired
    private StaffService staffService;

    @Autowired
    private StaffRepository staffRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    public void testUpdateOrCreateStaff() {
        // 1. Create initial staff
        staffService.updateOrCreateStaff("Initial Manager", "Kenridge", "1234", "CODE1");
        
        Staff staff = staffRepository.findByBranch("Kenridge").orElseThrow();
        assertEquals("Initial Manager", staff.getName());
        assertTrue(passwordEncoder.matches("1234", staff.getPinHash()));
        assertEquals("CODE1", staff.getBranchCode());

        // 2. Update staff
        staffService.updateOrCreateStaff("Updated Manager", "Kenridge", "5678", "CODE2");
        
        Staff updatedStaff = staffRepository.findByBranch("Kenridge").orElseThrow();
        assertEquals("Updated Manager", updatedStaff.getName());
        assertTrue(passwordEncoder.matches("5678", updatedStaff.getPinHash()));
        assertEquals("CODE2", updatedStaff.getBranchCode());
        assertEquals(staff.getId(), updatedStaff.getId(), "Should be the same record");

        // 3. Create another branch
        staffService.updateOrCreateStaff("Uitzicht Manager", "Uitzicht", "0000", "CODE3");
        Staff secondStaff = staffRepository.findByBranch("Uitzicht").orElseThrow();
        assertEquals("Uitzicht Manager", secondStaff.getName());
    }
}
