package com.warehouse.wms.security;

import com.warehouse.wms.model.User;
import com.warehouse.wms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService
        implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(
            String username
    ) throws UsernameNotFoundException {

        User user =
                userRepository
                        .findByUsername(
                                username.trim()
                        )
                        .orElseThrow(() ->
                                new UsernameNotFoundException(
                                        "User not found"
                                )
                        );


        String role =
                user.getRole() == null
                        ? "VIEWER"
                        : user.getRole().trim();

        /*
         * User.builder().roles(...)
         * automatically adds ROLE_.
         *
         * Therefore:
         * ROLE_ADMIN -> ADMIN
         */
        if (role.startsWith("ROLE_")) {
            role = role.substring(
                    "ROLE_".length()
            );
        }

        return org.springframework.security.core.userdetails.User
                .builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .roles(role)
                .disabled(!user.isActive())
                .build();
    }
}