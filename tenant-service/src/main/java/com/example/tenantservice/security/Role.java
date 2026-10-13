package com.example.tenantservice.security;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import static com.example.tenantservice.security.Fe.*;

/**
 * ROLE -> FE mapping for this service. Kept server-side, never put in the JWT -
 * the token only carries the role name, exactly like auth-service.
 */
public enum Role {
    /** Platform user: creates/manages/deactivates tenants; never sees tenant business data. */
    SUPER_ADMIN(EnumSet.of(TENANT_CREATE, TENANT_READ, TENANT_UPDATE, TENANT_SUSPEND, TENANT_REACTIVATE, TENANT_DEACTIVATE)),
    TENANT_ADMIN(EnumSet.of(MEMBER_READ)),
    TENANT_USER(EnumSet.of(MEMBER_READ));

    private final Set<Fe> fe;

    Role(Set<Fe> fe) {
        this.fe = Collections.unmodifiableSet(fe);
    }

    public Set<Fe> fe() {
        return fe;
    }
}
