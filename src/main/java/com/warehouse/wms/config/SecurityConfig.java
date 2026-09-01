package com.warehouse.wms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${wms.security.secret:${JWT_SECRET:unique-and-secret-wms-key-for-local-dev-only}}")
    private String rememberMeSecret;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**")
                )
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                new AntPathRequestMatcher("/api/**")
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        // Frontend static resources and SPA entry points
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/assets/**",
                                "/static/**",
                                "/css/**",
                                "/js/**",
                                "/favicon.ico",
                                "/locations",
                                "/map",
                                "/picking",
                                "/orders",
                                "/users",
                                "/profile",
                                "/login"
                        ).permitAll()

                        // Public REST endpoints, Docs, QR codes, Actuator health
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/v1/products/qr/**",
                                "/api/v1/locations/qr/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/actuator/health",
                                "/actuator/info"
                        ).permitAll()

                        // Role-specific endpoint permissions
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/products", "/api/v1/products/transfer", "/api/v1/products/import").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/products/**").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/products/**").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/locations").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/locations/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/locations/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/outbound-orders/create", "/api/v1/outbound-orders/*/confirm-pick", "/api/v1/outbound-orders/scan-confirm").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/outbound-orders/**").hasAnyRole("ADMIN", "OPERATOR")

                        // Remaining API endpoints require authentication
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll()
                )
                .rememberMe(rm -> rm
                        .key(rememberMeSecret)
                        .tokenValiditySeconds(86400 * 7)
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .deleteCookies("JSESSIONID", "remember-me")
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

