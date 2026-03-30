package com.smarthire.dto.auth;

import com.smarthire.entity.enums.UserRole;

public record AuthResponse(
        String token,
        Long userId,
        String name,
        String email,
        UserRole role
) {
}
