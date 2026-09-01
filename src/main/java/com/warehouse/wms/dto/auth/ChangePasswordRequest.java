package com.warehouse.wms.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required") String oldPassword,
        @NotBlank(message = "New password is required") @Size(min = 4, message = "New password must be at least 4 characters") String newPassword,
        @NotBlank(message = "Password confirmation is required") String confirmPassword
) {
}

