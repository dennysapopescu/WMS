package com.warehouse.wms.api;

import com.warehouse.wms.dto.auth.ChangePasswordRequest;
import com.warehouse.wms.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication
    ) {

        userService.changePassword(
                authentication.getName(),
                request.oldPassword(),
                request.newPassword(),
                request.confirmPassword()
        );

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Password changed successfully!"
                )
        );

    }
}