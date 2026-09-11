package org.example.onlinepossystem.location.service;

import org.example.onlinepossystem.location.api.ReverseGeocodingClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocationServiceTest {
    @Test
    void delegatesReverseGeocodingToInjectedClient() {
        ReverseGeocodingClient client = mock(ReverseGeocodingClient.class);
        when(client.findAddress(-33.9258, 18.4232)).thenReturn("1 Long Street, Cape Town");
        LocationService service = new LocationService(client);

        String result = service.getFullAddress(-33.9258, 18.4232);

        assertThat(result).isEqualTo("1 Long Street, Cape Town");
        verify(client).findAddress(-33.9258, 18.4232);
    }
}
