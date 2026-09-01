package com.warehouse.wms.security;

import com.warehouse.wms.model.User;
import com.warehouse.wms.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    void loadsUserWithRoleAndActiveStatus() {

        User user =
                new User();

        user.setUsername("admin");
        user.setPassword("hashed");
        user.setRole("ROLE_ADMIN");
        user.setActive(true);

        when(
                userRepository.findByUsername("admin")
        ).thenReturn(
                Optional.of(user)
        );

        UserDetails details =
                service.loadUserByUsername(
                        " admin "
                );

        assertEquals(
                "admin",
                details.getUsername()
        );

        assertEquals(
                "hashed",
                details.getPassword()
        );

        assertTrue(
                details.isEnabled()
        );

        assertTrue(
                details.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority()
                                        .equals(
                                                "ROLE_ADMIN"
                                        )
                        )
        );
    }

    @Test
    void inactiveUserIsDisabled() {

        User user =
                new User();

        user.setUsername("operator");
        user.setPassword("hashed");
        user.setRole("ROLE_OPERATOR");
        user.setActive(false);

        when(
                userRepository.findByUsername("operator")
        ).thenReturn(
                Optional.of(user)
        );

        UserDetails details =
                service.loadUserByUsername(
                        "operator"
                );

        assertFalse(
                details.isEnabled()
        );
    }

    @Test
    void roleWithoutPrefixIsHandledCorrectly() {

        User user =
                new User();

        user.setUsername("operator");
        user.setPassword("hashed");
        user.setRole("OPERATOR");
        user.setActive(true);

        when(
                userRepository.findByUsername("operator")
        ).thenReturn(
                Optional.of(user)
        );

        UserDetails details =
                service.loadUserByUsername(
                        "operator"
                );

        assertTrue(
                details.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority()
                                        .equals(
                                                "ROLE_OPERATOR"
                                        )
                        )
        );
    }

    @Test
    void unknownUserThrowsException() {

        when(
                userRepository.findByUsername("ghost")
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                UsernameNotFoundException.class,
                () -> service.loadUserByUsername(
                        "ghost"
                )
        );
    }
}