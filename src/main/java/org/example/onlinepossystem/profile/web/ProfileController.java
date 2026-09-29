package org.example.onlinepossystem.profile.web;

import org.example.onlinepossystem.profile.dto.ProfilePageView;
import org.example.onlinepossystem.profile.service.ProfilePageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProfileController {
    private final ProfilePageService profilePageService;
    private final String googleMapsApiKey;

    public ProfileController(ProfilePageService profilePageService,
                             @Value("${GOOGLE_MAPS_API_KEY:}") String googleMapsApiKey) {
        this.profilePageService = profilePageService;
        this.googleMapsApiKey = googleMapsApiKey;
    }

    @GetMapping("/profile/edit")
    public String editProfilePage(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        return profilePageService.getProfilePage(authentication.getName())
                .map(profile -> populateProfileModel(model, profile))
                .orElse("redirect:/login");
    }

    private String populateProfileModel(Model model, ProfilePageView profile) {
        model.addAttribute("customer", profile.customer());
        model.addAttribute("recentOrders", profile.recentOrders());
        model.addAttribute("googleMapsApiKey", googleMapsApiKey);
        return "customerInfoEdit";
    }
}
