package com.warehouse.wms.service;

import com.warehouse.wms.dto.auth.AuthUserResponse;
import com.warehouse.wms.dto.auth.LoginRequest;
import com.warehouse.wms.model.User;
import com.warehouse.wms.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpSession session;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void loginAuthenticatesStoresSecurityContextAndUpdatesLastLogin() {

        User user =
                new User();

        user.setId(1L);
        user.setUsername("admin");
        user.setFullName("Admin");
        user.setRole("ROLE_ADMIN");
        user.setActive(true);

        when(
                authenticationManager.authenticate(
                        any(
                                UsernamePasswordAuthenticationToken.class
                        )
                )
        ).thenReturn(authentication);

        when(
                userRepository.findByUsername("admin")
        ).thenReturn(
                Optional.of(user)
        );

        when(
                request.getSession(true)
        ).thenReturn(session);

        AuthUserResponse result =
                authService.login(
                        new LoginRequest(
                                " admin ",
                                "secret",
                                false
                        ),
                        request
                );

        assertEquals(
                "admin",
                result.username()
        );

        assertTrue(
                result.active()
        );

        assertNotNull(
                user.getLastLogin()
        );

        assertEquals(
                authentication,
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        );

        verify(userRepository)
                .save(user);

        verify(session)
                .setAttribute(
                        any(String.class),
                        any()
                );
    }

    @Test
    void loginPropagatesBadCredentials() {

        when(
                authenticationManager.authenticate(
                        any(
                                UsernamePasswordAuthenticationToken.class
                        )
                )
        ).thenThrow(
                new BadCredentialsException("bad")
        );

        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(
                        new LoginRequest(
                                "admin",
                                "wrong",
                                false
                        ),
                        request
                )
        );

        verifyNoInteractions(
                userRepository
        );
    }

    @Test
    void logoutClearsContextAndInvalidatesSession() {

        when(
                request.getSession(false)
        ).thenReturn(session);

        authService.logout(request);

        verify(session)
                .invalidate();

        assertNull(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        );
    }
}