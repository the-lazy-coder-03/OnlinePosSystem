package org.example.onlinepossystem.location.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class LocationService {
    private final RestTemplate restTemplate;
    private final String googleMapsApiKey;

    public LocationService(RestTemplate restTemplate,
                           @Value("${GOOGLE_MAPS_API_KEY}") String googleMapsApiKey) {
        this.restTemplate = restTemplate;
        this.googleMapsApiKey = googleMapsApiKey;
    }

    @SuppressWarnings("unchecked")
    public String getFullAddress(double lat, double lon) {
        String url = "https://maps.googleapis.com/maps/api/geocode/json?latlng="
                + lat + "," + lon + "&key=" + googleMapsApiKey;

        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        Map<String, Object> body = response.getBody();
        if (body != null && body.get("results") != null) {
            List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("results");
            if (!results.isEmpty()) {
                Map<String, Object> firstResult = results.get(0);

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

                String formattedAddress = firstResult.get("formatted_address").toString();
                if (!streetNumber.isEmpty() && !route.isEmpty()) {
                    return streetNumber + " " + route + ", " + formattedAddress.replace(streetNumber + " " + route + ", ", "");
                }
                return formattedAddress;
            }
        }
        return "Address not found";
    }
}
