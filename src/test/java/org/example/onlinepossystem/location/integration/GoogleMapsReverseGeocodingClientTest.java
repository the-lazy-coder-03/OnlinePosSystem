package org.example.onlinepossystem.location.integration;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class GoogleMapsReverseGeocodingClientTest {
    @Test
    void missingApiKeyDoesNotCallGoogle() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        GoogleMapsReverseGeocodingClient client = new GoogleMapsReverseGeocodingClient(restTemplate, " ");

        assertThat(client.findAddress(-33.9258, 18.4232)).isEqualTo("Address service unavailable");
        verifyNoInteractions(restTemplate);
    }
}
