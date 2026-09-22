package org.example.onlinepossystem.staff.web;

import org.example.onlinepossystem.staff.api.StaffOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Legacy staff records are retained for super-admin maintenance only. */
@RestController
@RequestMapping("/api/staff")
public class StaffController {

    private final StaffOperations staffOperations;

    public StaffController(StaffOperations staffOperations) {
        this.staffOperations = staffOperations;
    }

    /** Tell older POS clients where named account login has moved. */
    @PostMapping("/login")
    public ResponseEntity<?> login() {
        return ResponseEntity.status(410).body(Map.of("success", false,
                "message", "PIN/code login has been retired. Sign in with your admin account at /admin/login."));
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
                "staffId", staff.id()
        ));
    }
}
