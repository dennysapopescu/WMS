package com.warehouse.wms.api;

import com.warehouse.wms.dto.auth.AuthUserResponse;
import com.warehouse.wms.dto.auth.LoginRequest;
import com.warehouse.wms.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthUserResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {

        return ResponseEntity.ok(
                authService.login(
                        request,
                        httpRequest
                )
        );
    }

    @GetMapping("/me")
    public ResponseEntity<AuthUserResponse> getCurrentUser(
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                authService.getCurrentUser(
                        authentication
                )
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            HttpServletRequest request
    ) {

        authService.logout(request);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Logout successful"
                )
        );

    }
}