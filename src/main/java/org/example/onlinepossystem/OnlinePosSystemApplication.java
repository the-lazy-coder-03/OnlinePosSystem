package org.example.onlinepossystem;

import org.example.onlinepossystem.entity.User;
import org.example.onlinepossystem.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class OnlinePosSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(OnlinePosSystemApplication.class, args);
    }

    // This runs after Spring Boot starts and the database connection is ready
    @Bean
    CommandLineRunner run(UserRepository userRepository) {
        return args -> {
            // Create a test user
            User user = new User();
            user.setName("John Doe");
            user.setEmail("john@example.com");

            // Save to NeonDB
            userRepository.save(user);

            // Print all users in the table
            System.out.println("Users in database:");
            userRepository.findAll().forEach(System.out::println);
        };
    }
}
