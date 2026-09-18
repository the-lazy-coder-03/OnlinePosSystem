package org.example.onlinepossystem.shared.config;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class HttpJsonClient {

    private final RestTemplate restTemplate;

    public HttpJsonClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void post(String url, Object request, HttpHeaders headers) {
        restTemplate.postForEntity(url, new HttpEntity<>(request, headers), String.class);
    }
}
