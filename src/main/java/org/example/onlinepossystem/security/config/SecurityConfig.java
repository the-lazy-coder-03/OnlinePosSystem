package org.example.onlinepossystem.security.config;

import jakarta.servlet.http.HttpServletResponse;
import org.example.onlinepossystem.security.JwtAuthenticationFilter;
import org.example.onlinepossystem.security.AccountPrincipalRefreshFilter;
import org.example.onlinepossystem.security.LoggingAuthenticationFailureHandler;
import org.example.onlinepossystem.security.LoginRateLimitFilter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final LoginRateLimitFilter loginRateLimitFilter;
    private final LoggingAuthenticationFailureHandler authenticationFailureHandler;
    private final AuthenticationSuccessHandler authenticationSuccessHandler;

    public SecurityConfig(UserDetailsService userDetailsService,
                          JwtAuthenticationFilter jwtAuthenticationFilter,
                          LoginRateLimitFilter loginRateLimitFilter,
                          LoggingAuthenticationFailureHandler authenticationFailureHandler,
                          @Qualifier("roleAwareAuthenticationSuccessHandler") AuthenticationSuccessHandler authenticationSuccessHandler) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.loginRateLimitFilter = loginRateLimitFilter;
        this.authenticationFailureHandler = authenticationFailureHandler;
        this.authenticationSuccessHandler = authenticationSuccessHandler;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean("staffPasswordEncoder")
    public PasswordEncoder staffPasswordEncoder() {
        return new BCryptPasswordEncoder();
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
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration() {
        var registration = new FilterRegistrationBean<>(jwtAuthenticationFilter);
        registration.setEnabled(false); // Runs only inside Spring Security, after context loading.
        return registration;
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            SecurityContextRepository securityContextRepository
    ) throws Exception {
        http
                .cors(org.springframework.security.config.Customizer.withDefaults())
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(loginRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, CsrfFilter.class)
                .addFilterAfter(new AccountPrincipalRefreshFilter(userDetailsService), JwtAuthenticationFilter.class)
                .securityContext(context -> context
                        .securityContextRepository(securityContextRepository)
                )
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(request -> Boolean.TRUE.equals(
                                request.getAttribute(JwtAuthenticationFilter.AUTHENTICATED_BEARER)))
                        .ignoringRequestMatchers(request -> "POST".equals(request.getMethod())
                                && java.util.Set.of("/api/auth/login", "/api/auth/forgot-password",
                                        "/api/auth/reset-password", "/api/staff/login").contains(request.getRequestURI().substring(request.getContextPath().length()))
                                && request.getContentType() != null
                                && request.getContentType().split(";", 2)[0].trim()
                                        .equalsIgnoreCase("application/json"))
                )
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                )
                .authorizeHttpRequests(auth -> auth
                        // Protect order and profile pages
                        .requestMatchers("/order", "/profile/edit").hasAnyRole("USER", "SUPER_ADMIN")
                        .requestMatchers("/profile/update").hasRole("USER")
                        .requestMatchers("/admin/login").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // Protect sensitive API endpoints
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/staff/create").hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/orders").hasAnyRole("USER", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/orders/menu").permitAll()
                        .requestMatchers("/api/orders", "/api/orders/**").hasRole("ADMIN")
                        .requestMatchers("/input-orders", "/orders", "/InputOrders", "/InputOrders.html").hasRole("ADMIN")
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/forgot-password").permitAll()
                        .requestMatchers("/api/auth/reset-password").permitAll()
                        .requestMatchers("/api/staff/login").permitAll()
                        .requestMatchers("/api/branches/**").permitAll()
                        .requestMatchers("/api/full-address").permitAll()

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
                                "/test",              // Test page
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/webjars/**"
                        ).permitAll()

                        // All other requests authenticated
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedHandler((request, response, exception) ->
                                response.setStatus(HttpServletResponse.SC_FORBIDDEN))
                        .defaultAuthenticationEntryPointFor(
                                (request, response, exception) -> response.sendError(401, "Authentication is required"),
                                new AntPathRequestMatcher("/api/**")
                        )
                        .defaultAuthenticationEntryPointFor(new LoginUrlAuthenticationEntryPoint("/admin/login"),
                                new org.springframework.security.web.util.matcher.OrRequestMatcher(
                                        new AntPathRequestMatcher("/input-orders"), new AntPathRequestMatcher("/orders"),
                                        new AntPathRequestMatcher("/InputOrders"), new AntPathRequestMatcher("/InputOrders.html")))
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/admin/login"),
                                new AntPathRequestMatcher("/admin/**")
                        )
                        .defaultAuthenticationEntryPointFor(new LoginUrlAuthenticationEntryPoint("/login"),
                                org.springframework.security.web.util.matcher.AnyRequestMatcher.INSTANCE)
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .failureHandler(authenticationFailureHandler)
                        .successHandler(authenticationSuccessHandler)
                                                .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .permitAll()
                );

        return http.build();
    }

}
