package org.example.onlinepossystem.config;

import org.example.onlinepossystem.service.CustomerUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final CustomerUserDetailsService userDetailsService;

    public SecurityConfig(CustomerUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Protect order and profile pages
                        .requestMatchers("/order", "/profile/edit").authenticated()

                        // Protect sensitive API endpoints
                        .requestMatchers("/api/staff/create").authenticated()
                        .requestMatchers("/api/orders/**").permitAll() // Needed for POS frontend
                        .requestMatchers("/api/staff/login").permitAll()

                        // All other api endpoints
                        .requestMatchers("/api/**").permitAll()

                        // Public pages + static resources
                        .requestMatchers(
                                "/",
                                "/home",
                                "/menu",
                                "/menu/**",
                                "/login",
                                "/register",
                                "/input-orders",      // POS frontend page
                                "/orders",            // Alias for POS frontend
                                "/InputOrders",       // Case sensitive alias
                                "/InputOrders.html",  // Direct file alias
                                "/test",              // Test page
                                "/css/**",
                                "/js/**",
                                "/images/**"
                        ).permitAll()

                        // All other requests authenticated
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/", false)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .permitAll()
                );

        return http.build();
    }

}
