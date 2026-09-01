package com.warehouse.wms.dto.user;

import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
        @NotBlank(message = "Username is required") String username,
        @NotBlank(message = "Password is required") String password,
        String fullName,
        @NotBlank(message = "Role is required") String role,
        String shift
) {
}
