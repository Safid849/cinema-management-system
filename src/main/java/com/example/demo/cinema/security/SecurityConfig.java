package com.example.demo.cinema.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@AllArgsConstructor
public class SecurityConfig {

    private static final String MANAGER = "MANAGER";
    private static final String EMPLOYEE = "EMPLOYEE";

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .exceptionHandling(
                        ex ->
                                ex.authenticationEntryPoint(
                                                (request, response, authException) -> {
                                                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                                    response.setContentType("application/json");
                                                    response
                                                            .getWriter()
                                                            .write("{\"code\":\"UNAUTHORIZED\",\"message\":\"Missing or invalid token\"}");
                                                })
                                        .accessDeniedHandler(
                                                (request, response, accessDeniedException) -> {
                                                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                                    response.setContentType("application/json");
                                                    response
                                                            .getWriter()
                                                            .write("{\"code\":\"FORBIDDEN\",\"message\":\"Insufficient role\"}");
                                                }))
                .authorizeHttpRequests(
                        auth ->
                                auth
                                        // --- routes publiques ---
                                        .requestMatchers(HttpMethod.GET, "/ping")
                                        .permitAll()
                                        .requestMatchers(HttpMethod.POST, "/users/register", "/users/login")
                                        .permitAll()
                                        .requestMatchers(HttpMethod.GET, "/projections", "/projections/**")
                                        .permitAll()
                                        .requestMatchers(HttpMethod.PUT, "/movies")
                                        .hasRole(MANAGER)
                                        .requestMatchers(HttpMethod.GET, "/movies", "/movies/**")
                                        .authenticated()
                                        .requestMatchers(HttpMethod.PUT, "/projections")
                                        .hasRole(MANAGER)
                                        .requestMatchers(HttpMethod.GET, "/reservations")
                                        .hasAnyRole(MANAGER, EMPLOYEE)
                                        .requestMatchers(HttpMethod.PUT, "/reservations")
                                        .hasAnyRole(MANAGER, EMPLOYEE)
                                        .requestMatchers(HttpMethod.GET, "/reservations/**")
                                        .authenticated()
                                        .anyRequest()
                                        .authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}