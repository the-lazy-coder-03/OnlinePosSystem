package org.example.onlinepossystem.staff.web;

import org.example.onlinepossystem.staff.service.StaffService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API Controller for Staff authentication.
 * Handles PIN-based login for branch staff.
 */
@RestController
@RequestMapping("/api/staff")
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    /**
     * POST /api/staff/login
     * Authenticate staff using PIN or 16-character code.
     *
     * Request Body: { "pin": "1234" } or { "code": "..." }
     * Response:
     *   - Success: { "success": true, "branch": "Kenridge" }
     *   - Failure: { "success": false, "message": "Invalid credentials" }
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String enteredPin = request.get("pin");
        String enteredCode = request.get("code");

        String branch = null;

        if (enteredCode != null && !enteredCode.isEmpty()) {
            branch = staffService.authenticateByCode(enteredCode);
        } else if (enteredPin != null && !enteredPin.isEmpty()) {
            branch = staffService.authenticateStaff(enteredPin);
        } else {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "PIN or Code is required"));
        }

        if (branch != null) {
            // Authentication successful
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "branch", branch
            ));
        } else {
            // Authentication failed
            return ResponseEntity.status(401)
                    .body(Map.of("success", false, "message", "Invalid credentials"));
        }
    }

    /**
     * POST /api/staff/create
     * Create a new staff member (for initial setup or admin use).
     *
     * Request Body: { "name": "John Doe", "branch": "Kenridge", "pin": "1234" }
     * Response: { "success": true, "staffId": 1 }
     */
    @PostMapping("/create")
    public ResponseEntity<?> createStaff(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        String branch = request.get("branch");
        String pin = request.get("pin");

        if (name == null || branch == null || pin == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Name, branch, and PIN are required"));
        }

        var staff = staffService.createStaff(name, branch, pin);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "staffId", staff.getId()
        ));
    }
}
