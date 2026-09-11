package org.example.onlinepossystem.location.service;

import org.example.onlinepossystem.location.api.ReverseGeocodingClient;
import org.springframework.stereotype.Service;

@Service
public class LocationService {
    private final ReverseGeocodingClient reverseGeocodingClient;

    public LocationService(ReverseGeocodingClient reverseGeocodingClient) {
        this.reverseGeocodingClient = reverseGeocodingClient;
    }

    public String getFullAddress(double lat, double lon) {
        return reverseGeocodingClient.findAddress(lat, lon);
    }
}
