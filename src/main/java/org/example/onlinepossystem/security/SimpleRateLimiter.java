package org.example.onlinepossystem.security;

import org.example.onlinepossystem.security.api.RateLimiter;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SimpleRateLimiter implements RateLimiter {

    private static final int MAX_TRACKED_KEYS = 10_000;

    private final Map<String, AttemptWindow> attempts = new ConcurrentHashMap<>();

    @Override
    public boolean isAllowed(String key, int maxAttempts, Duration window) {
        Instant now = Instant.now();
        if (attempts.size() >= MAX_TRACKED_KEYS) {
            attempts.entrySet().removeIf(entry -> entry.getValue().expiresAt.isBefore(now));
            if (attempts.size() >= MAX_TRACKED_KEYS && !attempts.containsKey(key)) return false;
        }
        AttemptWindow current = attempts.compute(key, (ignored, existing) -> {
            if (existing == null || existing.expiresAt.isBefore(now)) {
                return new AttemptWindow(1, now.plus(window));
            }
            return new AttemptWindow(existing.count + 1, existing.expiresAt);
        });
        return current.count <= maxAttempts;
    }

    @Override
    public void reset(String key) {
        attempts.remove(key);
    }

    private record AttemptWindow(int count, Instant expiresAt) {
    }
}
