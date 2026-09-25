package org.example.onlinepossystem.special.web;

import jakarta.validation.Valid;
import org.example.onlinepossystem.special.dto.SpecialQuoteRequest;
import org.example.onlinepossystem.special.dto.SpecialQuoteResponse;
import org.example.onlinepossystem.special.dto.SpecialView;
import org.example.onlinepossystem.special.service.SpecialService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/branches/{branchId}/specials")
public class SpecialController {
    private final SpecialService service;
    public SpecialController(SpecialService service) { this.service = service; }

    @GetMapping
    public List<SpecialView> available(@PathVariable Integer branchId) { return service.available(branchId); }

    @PostMapping("/{specialId}/quote")
    public SpecialQuoteResponse quote(@PathVariable Integer branchId, @PathVariable Long specialId,
                                      @Valid @RequestBody SpecialQuoteRequest request) {
        return service.quote(branchId, specialId, request);
    }
}
