package com.warehouse.wms.dto.user;

import java.time.LocalDateTime;

public record UserDto(
        Long id,
        String username,
        String fullName,
        String role,
        String shift,
        boolean active,
        LocalDateTime lastLogin
) {
}
