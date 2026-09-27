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
        if (!Double.isFinite(lat) || !Double.isFinite(lon) || lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Coordinates are outside the valid range");
        }
        return locationService.getFullAddress(lat, lon);
    }
}
