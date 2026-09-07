package org.example.onlinepossystem.location.web;

import org.example.onlinepossystem.location.service.LocationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping("/api/full-address")
    public String getFullAddress(@RequestParam double lat, @RequestParam double lon) {
        return locationService.getFullAddress(lat, lon);
    }
}
