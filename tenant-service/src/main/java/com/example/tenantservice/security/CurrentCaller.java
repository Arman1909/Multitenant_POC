package com.example.tenantservice.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Caller identity from the validated JWT. Replaces the old trusted X-Username / X-USER-ROLE / X-TENANT-ID headers:
 * identity and tenant now ONLY come from the signed token.
 */
public final class CurrentCaller {

    private CurrentCaller() {
    }

    private static Jwt jwt() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a != null && a.getPrincipal() instanceof Jwt j) {
            return j;
        }
        throw new AccessDeniedException("Not authenticated");
    }

    /** Value for entered_by / updated_by audit columns. */
    public static String username() {
        Jwt jwt = jwt();
        String username = jwt.getClaimAsString("username");
        return username != null ? username : jwt.getSubject();
    }

    /** Active tenant (tenant-service tenant_id) for OWN_TENANT callers; SUPER_ADMIN never has one. */
    public static Long requireActiveTenant() {
        Jwt jwt = jwt();
        if (!"OWN_TENANT".equals(jwt.getClaimAsString("deRole"))) {
            throw new AccessDeniedException("DE_ROLE does not allow tenant data access");
        }
        String tenantId = jwt.getClaimAsString("tenantId");
        if (tenantId == null) {
            throw new AccessDeniedException("No active tenant in token");
        }
        try {
            return Long.valueOf(tenantId);
        } catch (NumberFormatException e) {
            throw new AccessDeniedException("Token tenantId is not a platform tenant id; log in again");
        }
    }
}
