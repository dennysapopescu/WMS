package com.warehouse.wms.service;

import com.warehouse.wms.dto.auth.AuthUserResponse;
import com.warehouse.wms.dto.auth.LoginRequest;
import com.warehouse.wms.model.User;
import com.warehouse.wms.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;

    @Transactional
    public AuthUserResponse login(
            LoginRequest request,
            HttpServletRequest httpRequest
    ) {

        String username =
                request.username().trim();

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                username,
                                request.password()
                        )
                );

        User user =
                userRepository.findByUsername(username)
                        .orElseThrow(() ->
                                new UsernameNotFoundException(
                                        "User not found"
                                )
                        );

        /*
         * Store authentication in SecurityContext.
         */
        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(
                authentication
        );

        SecurityContextHolder.setContext(
                context
        );

        /*
         * Persist SecurityContext in HTTP session.
         */
        HttpSession session =
                httpRequest.getSession(true);

        session.setAttribute(
                HttpSessionSecurityContextRepository
                        .SPRING_SECURITY_CONTEXT_KEY,
                context
        );

        /*
         * Update last login.
         */
        user.setLastLogin(
                LocalDateTime.now()
        );

        userRepository.save(user);

        return toResponse(user);
    }

    public AuthUserResponse getCurrentUser(
            Authentication authentication
    ) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(
                authentication.getPrincipal()
        )) {

            throw new AuthenticationCredentialsNotFoundException(
                    "Unauthenticated"
            );
        }

        return userRepository
                .findByUsername(
                        authentication.getName()
                )
                .map(this::toResponse)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found"
                        )
                );
    }


    public void logout(
            HttpServletRequest request
    ) {

        SecurityContextHolder.clearContext();

        HttpSession session =
                request.getSession(false);

        if (session != null) {
            session.invalidate();
        }
    }

    private AuthUserResponse toResponse(
            User user
    ) {

        return new AuthUserResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole(),
                user.getShift(),
                user.isActive()
        );
    }
}