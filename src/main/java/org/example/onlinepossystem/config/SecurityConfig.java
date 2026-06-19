package org.example.onlinepossystem.config;

import org.example.onlinepossystem.security.JwtAuthenticationFilter;
import org.example.onlinepossystem.security.LoggingAuthenticationFailureHandler;
import org.example.onlinepossystem.security.LoginRateLimitFilter;
import org.example.onlinepossystem.service.CustomerUserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    private final CustomerUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final LoginRateLimitFilter loginRateLimitFilter;
    private final LoggingAuthenticationFailureHandler authenticationFailureHandler;

    public SecurityConfig(CustomerUserDetailsService userDetailsService,
                          JwtAuthenticationFilter jwtAuthenticationFilter,
                          LoginRateLimitFilter loginRateLimitFilter,
                          LoggingAuthenticationFailureHandler authenticationFailureHandler) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.loginRateLimitFilter = loginRateLimitFilter;
        this.authenticationFailureHandler = authenticationFailureHandler;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        // Special case for admin password from .env which might not be BCrypt encoded
        // But for simplicity, we'll use NoOpPasswordEncoder for the admin ONLY if it matches the .env password
        // Or better, just don't encode it in UserDetailsService if it's the admin.
        // Spring Security 5+ requires an ID for PasswordEncoder, e.g. {bcrypt}, {noop}.
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        SavedRequestAwareAuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
        successHandler.setDefaultTargetUrl("/");
        successHandler.setAlwaysUseDefaultTargetUrl(false);

        http
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(loginRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**", "/h2-console/**")
                )
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                )
                .authorizeHttpRequests(auth -> auth
                        // Protect order and profile pages
                        .requestMatchers("/order", "/profile/edit", "/profile/update").authenticated()
                        .requestMatchers("/admin/login").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // Protect sensitive API endpoints
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/staff/create").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/orders").authenticated()
                        .requestMatchers("/api/orders/**").permitAll() // Needed for POS frontend
                        .requestMatchers("/api/auth/login").permitAll()
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
                                "/admin/login",
                                "/register",
                                "/forgot-password",
                                "/reset-password",
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
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/admin/login"),
                                new AntPathRequestMatcher("/admin/**")
                        )
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .failureHandler(authenticationFailureHandler)
                        .successHandler((request, response, authentication) -> {
                            logger.info("Successful login for role(s) {}", authentication.getAuthorities());
                            boolean adminLogin = "true".equals(request.getParameter("adminLogin"));
                            boolean isAdmin = authentication.getAuthorities().stream()
                                    .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
                            if (adminLogin && !isAdmin) {
                                SecurityContextHolder.clearContext();
                                request.getSession().invalidate();
                                response.sendRedirect("/admin/login?error");
                                return;
                            }
                            successHandler.onAuthenticationSuccess(request, response, authentication);
                        })
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
