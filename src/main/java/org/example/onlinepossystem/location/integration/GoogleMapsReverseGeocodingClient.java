package org.example.onlinepossystem.location.integration;

import org.example.onlinepossystem.location.api.ReverseGeocodingClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class GoogleMapsReverseGeocodingClient implements ReverseGeocodingClient {
    private final RestTemplate restTemplate;
    private final String apiKey;

    public GoogleMapsReverseGeocodingClient(
            RestTemplate restTemplate,
            @Value("${GOOGLE_MAPS_API_KEY:}") String apiKey
    ) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String findAddress(double latitude, double longitude) {
        if (!StringUtils.hasText(apiKey)) {
            return "Address service unavailable";
        }

        String url = "https://maps.googleapis.com/maps/api/geocode/json?latlng="
                + latitude + "," + longitude + "&key=" + apiKey;

        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        Map<String, Object> body = response.getBody();
        if (body == null || body.get("results") == null) {
            return "Address not found";
        }

        List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("results");
        if (results.isEmpty()) {
            return "Address not found";
        }

        Map<String, Object> firstResult = results.get(0);
        String streetNumber = "";
        String route = "";
        List<Map<String, Object>> components = (List<Map<String, Object>>) firstResult.get("address_components");
        for (Map<String, Object> component : components) {
            List<String> types = (List<String>) component.get("types");
            if (types.contains("street_number")) {
                streetNumber = component.get("long_name").toString();
            } else if (types.contains("route")) {
                route = component.get("long_name").toString();
            }
        }

        String formattedAddress = firstResult.get("formatted_address").toString();
        if (!streetNumber.isEmpty() && !route.isEmpty()) {
            String streetPrefix = streetNumber + " " + route;
            return streetPrefix + ", " + formattedAddress.replace(streetPrefix + ", ", "");
        }
        return formattedAddress;
    }
}
