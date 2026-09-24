package org.example.onlinepossystem.security;

import org.example.onlinepossystem.security.api.RequestClientIp;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.assertj.core.api.Assertions.assertThat;

class RequestClientIpTest {
    @Test void rawForwardedHeadersCannotChooseRateLimitIdentity() {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.20");
        request.addHeader("X-Forwarded-For", "203.0.113.99, 127.0.0.1");
        assertThat(RequestClientIp.resolve(request)).isEqualTo("198.51.100.20");
    }
}
