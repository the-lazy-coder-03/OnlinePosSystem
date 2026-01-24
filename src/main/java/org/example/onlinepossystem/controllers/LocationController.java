package org.example.onlinepossystem.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.Map;

@RestController
public class LocationController {

    @Value("${what3words.api.key}")
    private String w3wKey;

    @GetMapping("/api/convert-to-3words")
    public String convertTo3Words(@RequestParam double lat, @RequestParam double lon) {
        String url = "https://api.what3words.com/v3/convert-to-3wa?coordinates="
                + lat + "," + lon + "&key=" + w3wKey;

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

        Map<String, Object> body = response.getBody();
        if(body != null && body.get("words") != null) {
            return body.get("words").toString(); // returns the 3-word address
        }
        return "Unable to get 3-word address";
    }
}
