package com.warehouse.wms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // DEZACTIVARE CSRF pentru scanare
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/orders/scan-confirm/**")
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/js/**", "/api/products/qr/**").permitAll()

                        // Permisiuni pentru ORDERS
                        .requestMatchers("/orders").hasAnyRole("ADMIN", "OPERATOR", "VIEWER")
                        .requestMatchers("/orders/create", "/orders/confirm/**", "/orders/scan-confirm/**", "/orders/delete/**")
                        .hasAnyRole("ADMIN", "OPERATOR")

                        .requestMatchers("/profile/**").authenticated()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/products/**").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers("/products/delete/**").hasRole("ADMIN")
                        .requestMatchers("/products/update/**", "/products/reduce/**").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers("/locations").hasAnyRole("ADMIN", "OPERATOR", "VIEWER")
                        .requestMatchers("/locations/add/**").hasRole("ADMIN")
                        .requestMatchers("/locations/qr/**").hasAnyRole("ADMIN", "OPERATOR", "VIEWER")
                        .requestMatchers("/", "/products", "/products/export").hasAnyRole("ADMIN", "OPERATOR", "VIEWER")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .defaultSuccessUrl("/", true)
                        .permitAll()
                )
                .rememberMe(rm -> rm
                        .key("uniqueAndSecretWmsKey")
                        .tokenValiditySeconds(86400 * 7)
                )
                .logout(logout -> logout
                        .deleteCookies("JSESSIONID", "remember-me")
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}