package org.example.onlinepossystem.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

@RestController
public class LocationController {

    // Hardcoded API key for now
    private String googleMapsApiKey = "AIzaSyACQWLP46CeWOmNAMjJRcoWRYSNKkdrWGM";

    @GetMapping("/api/full-address")
    public String getFullAddress(@RequestParam double lat, @RequestParam double lon) {
        String url = "https://maps.googleapis.com/maps/api/geocode/json?latlng="
                + lat + "," + lon + "&key=" + googleMapsApiKey;

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

        Map<String, Object> body = response.getBody();
        if (body != null && body.get("results") != null) {
            List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("results");
            if (!results.isEmpty()) {
                Map<String, Object> firstResult = results.get(0);

                // Extract street number and route
                String streetNumber = "";
                String route = "";
                List<Map<String, Object>> components = (List<Map<String, Object>>) firstResult.get("address_components");
                for (Map<String, Object> comp : components) {
                    List<String> types = (List<String>) comp.get("types");
                    if (types.contains("street_number")) {
                        streetNumber = comp.get("long_name").toString();
                    } else if (types.contains("route")) {
                        route = comp.get("long_name").toString();
                    }
                }

                // If we have both, combine; else, use formatted_address
                String formattedAddress = firstResult.get("formatted_address").toString();
                if (!streetNumber.isEmpty() && !route.isEmpty()) {
                    return streetNumber + " " + route + ", " + formattedAddress.replace(streetNumber + " " + route + ", ", "");
                } else {
                    return formattedAddress;
                }
            }
        }
        return "Address not found";
    }
}
