package org.example.onlinepossystem.customer.web;

import org.example.onlinepossystem.customer.api.AdminCustomerNoteView;
import org.example.onlinepossystem.customer.api.AdminCustomerView;
import org.example.onlinepossystem.customer.api.PagedResult;
import org.example.onlinepossystem.customer.service.AdminCustomerRecordService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
public class AdminCustomerController {
    private final AdminCustomerRecordService records;

    public AdminCustomerController(AdminCustomerRecordService records) {
        this.records = records;
    }

    @GetMapping("/api/admin/customers")
    public PagedResult<AdminCustomerView> search(@RequestParam(required = false) String query,
                                                  @RequestParam(defaultValue = "0") int page) {
        return records.search(query, page);
    }

    @GetMapping("/api/admin/customers/{id}")
    public AdminCustomerView detail(@PathVariable Long id) {
        return records.detail(id);
    }

    @GetMapping("/api/admin/customers/{id}/notes")
    public PagedResult<AdminCustomerNoteView> notes(@PathVariable Long id,
                                                     @RequestParam(defaultValue = "0") int page) {
        return records.notes(id, page);
    }

    @PostMapping("/admin/customers/{id}/notes")
    public AdminCustomerNoteView addNote(@PathVariable Long id, @RequestBody NoteRequest request,
                                         Authentication authentication) {
        return records.addNote(id, request.body(), authentication.getName());
    }

    public record NoteRequest(String body) {}
}
