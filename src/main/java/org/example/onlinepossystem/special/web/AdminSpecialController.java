package org.example.onlinepossystem.special.web;

import jakarta.validation.Valid;
import org.example.onlinepossystem.special.dto.AdminSpecialRequest;
import org.example.onlinepossystem.special.dto.AdminSpecialCatalog;
import org.example.onlinepossystem.special.dto.SpecialView;
import org.example.onlinepossystem.special.service.SpecialService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/specials")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminSpecialController {
    private final SpecialService service;

    public AdminSpecialController(SpecialService service) {
        this.service = service;
    }

    @GetMapping
    public List<SpecialView> list() { return service.adminList(); }

    @GetMapping("/catalog")
    public AdminSpecialCatalog catalog() { return service.adminCatalog(); }

    @GetMapping("/{id}")
    public SpecialView detail(@PathVariable Long id) { return service.adminDetail(id); }

    @PostMapping
    public SpecialView create(@Valid @RequestBody AdminSpecialRequest request) {
        return service.adminSave(null, request);
    }

    @PutMapping("/{id}")
    public SpecialView update(@PathVariable Long id, @Valid @RequestBody AdminSpecialRequest request) {
        return service.adminSave(id, request);
    }

    @PostMapping("/{id}/archive")
    public SpecialView archive(@PathVariable Long id) { return service.archive(id); }

    @PostMapping("/{id}/restore")
    public SpecialView restore(@PathVariable Long id) { return service.restore(id); }
}
