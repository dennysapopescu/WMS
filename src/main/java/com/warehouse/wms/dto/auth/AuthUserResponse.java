package com.warehouse.wms.dto.auth;

public record AuthUserResponse(
        Long id,
        String username,
        String fullName,
        String role,
        String shift,
        boolean active
) {
}
