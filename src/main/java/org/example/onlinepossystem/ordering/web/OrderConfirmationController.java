package org.example.onlinepossystem.ordering.web;

import org.example.onlinepossystem.ordering.service.OrderConfirmationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class OrderConfirmationController {
    private final OrderConfirmationService confirmationService;

    public OrderConfirmationController(OrderConfirmationService confirmationService) {
        this.confirmationService = confirmationService;
    }

    @GetMapping("/orders/{orderId}/confirmation")
    public String confirmation(@PathVariable Long orderId, Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        var confirmation = confirmationService.findForCustomer(orderId, authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("confirmation", confirmation);
        return "orderConfirmation";
    }
}
