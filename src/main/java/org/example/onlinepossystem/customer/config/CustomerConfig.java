package org.example.onlinepossystem.customer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;

@Configuration
public class CustomerConfig {
    @Bean
    public SecureRandom secureRandom() {
        return new SecureRandom();
    }
}
