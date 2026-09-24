package org.example.onlinepossystem.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.beans.factory.annotation.Value;
import java.util.Arrays;
import java.util.List;

/**
 * CORS Configuration to allow frontend to communicate with backend.
 * Enables cross-origin requests for API endpoints.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:${app.base-url:http://localhost:8080}}") String allowedOrigins) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowCredentials(true);
        config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim)
                .filter(origin -> !origin.isEmpty()).toList());
        config.validateAllowCredentials();
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "Authorization", "X-CSRF-TOKEN", "X-XSRF-TOKEN"));

        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
