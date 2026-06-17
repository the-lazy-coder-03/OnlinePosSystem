package org.example.onlinepossystem.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SimpleRateLimiter {

    private final Map<String, AttemptWindow> attempts = new ConcurrentHashMap<>();

    public boolean isAllowed(String key, int maxAttempts, Duration window) {
        Instant now = Instant.now();
        AttemptWindow current = attempts.compute(key, (ignored, existing) -> {
            if (existing == null || existing.windowStartedAt.plus(window).isBefore(now)) {
                return new AttemptWindow(1, now);
            }
            return new AttemptWindow(existing.count + 1, existing.windowStartedAt);
        });
        return current.count <= maxAttempts;
    }

    public void reset(String key) {
        attempts.remove(key);
    }

    private record AttemptWindow(int count, Instant windowStartedAt) {
    }
}
