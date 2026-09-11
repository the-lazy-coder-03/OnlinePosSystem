package org.example.onlinepossystem.security.api;

import java.time.Duration;

public interface RateLimiter {
    boolean isAllowed(String key, int maxAttempts, Duration window);

    void reset(String key);
}
