package com.smarthire.util;

import com.smarthire.exception.UnauthorizedException;
import com.smarthire.security.SecurityUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static SecurityUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof SecurityUser securityUser)) {
            throw new UnauthorizedException("Authenticated user not found");
        }
        return securityUser;
    }
}
