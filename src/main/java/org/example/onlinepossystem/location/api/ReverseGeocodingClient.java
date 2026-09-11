package org.example.onlinepossystem.location.api;

public interface ReverseGeocodingClient {
    String findAddress(double latitude, double longitude);
}
