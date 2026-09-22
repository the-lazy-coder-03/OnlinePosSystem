package org.example.onlinepossystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;

@SpringBootApplication
public class OnlinePosSystemApplication {
    public static void main(String[] args) {
        String profiles = System.getProperty("spring.profiles.active",
                System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", "default"));
        for (String arg : args) {
            if (arg.startsWith("--spring.profiles.active=")) profiles = arg.substring(arg.indexOf('=') + 1);
        }
        if (Arrays.asList(profiles.split(",")).contains("migrate")) {
            if (!"migrate".equals(profiles)) throw new IllegalArgumentException("Use the migrate profile alone");
            SpringApplication application = new SpringApplication(MigrationConfiguration.class);
            application.setWebApplicationType(WebApplicationType.NONE);
            application.setAdditionalProfiles("migrate");
            try (var context = application.run(args)) {
                // All migration runners have completed; close the owner connection pool.
            }
        } else {
            SpringApplication.run(OnlinePosSystemApplication.class, args);
        }
    }
}
