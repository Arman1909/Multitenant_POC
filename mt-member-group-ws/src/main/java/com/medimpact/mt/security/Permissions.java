package com.medimpact.mt.security;

import com.medimpact.mt.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** In-code permission checks for rules {@link AccessRules} can't express (e.g. depending on a query parameter). */
public final class Permissions {

    private Permissions() {
    }

    public static boolean has(Fe fe) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> fe.name().equals(a.getAuthority()));
    }

    public static void require(Fe fe) {
        if (!has(fe)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Requires permission " + fe);
        }
    }
}
