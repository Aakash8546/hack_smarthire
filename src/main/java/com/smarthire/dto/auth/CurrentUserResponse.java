package com.smarthire.dto.auth;

import java.util.List;

import com.smarthire.entity.enums.UserRole;

public record CurrentUserResponse(
        Long id,
        String name,
        String email,
        UserRole role,
        boolean verified,
        List<String> skills
) {
}
