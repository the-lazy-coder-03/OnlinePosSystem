package org.example.onlinepossystem.staff.web;

import org.example.onlinepossystem.staff.api.StaffOperations;
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

    private final StaffOperations staffOperations;

    public StaffController(StaffOperations staffOperations) {
        this.staffOperations = staffOperations;
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

        if ((enteredCode == null || enteredCode.isEmpty()) && (enteredPin == null || enteredPin.isEmpty())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "PIN or Code is required"));
        }

        return staffOperations.authenticate(enteredPin, enteredCode)
                .<ResponseEntity<?>>map(branch -> ResponseEntity.ok(Map.of(
                        "success", true,
                        "branch", branch
                )))
                .orElseGet(() -> ResponseEntity.status(401)
                        .body(Map.of("success", false, "message", "Invalid credentials")));
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

        var staff = staffOperations.createStaff(name, branch, pin, null);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "staffId", staff.getId()
        ));
    }
}
