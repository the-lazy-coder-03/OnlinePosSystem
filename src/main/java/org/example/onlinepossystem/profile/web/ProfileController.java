package org.example.onlinepossystem.profile.web;

import org.example.onlinepossystem.profile.dto.ProfilePageView;
import org.example.onlinepossystem.profile.service.ProfilePageService;
import org.example.onlinepossystem.customer.api.EnvironmentAdminAccount;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProfileController {
    private final ProfilePageService profilePageService;
    private final EnvironmentAdminAccount environmentAdminAccount;

    public ProfileController(ProfilePageService profilePageService, EnvironmentAdminAccount environmentAdminAccount) {
        this.profilePageService = profilePageService;
        this.environmentAdminAccount = environmentAdminAccount;
    }

    @GetMapping("/profile/edit")
    public String editProfilePage(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        if (environmentAdminAccount.matches(authentication.getPrincipal())) {
            Long customerId = environmentAdminAccount.ensureCustomerId(authentication.getPrincipal());
            model.addAttribute("readOnlyHistory", true);
            return profilePageService.getProfilePageForCustomerId(customerId)
                    .map(profile -> populateProfileModel(model, profile))
                    .orElseThrow(() -> new IllegalStateException("Internal admin customer is missing."));
        }
        return profilePageService.getProfilePage(authentication.getName())
                .map(profile -> populateProfileModel(model, profile))
                .orElse("redirect:/login");
    }

    private String populateProfileModel(Model model, ProfilePageView profile) {
        model.addAttribute("customer", profile.customer());
        model.addAttribute("recentOrders", profile.recentOrders());
        return "customerInfoEdit";
    }
}
