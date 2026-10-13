package com.medimpact.mt.security;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import static com.medimpact.mt.security.Fe.*;

/**
 * ROLE -> FE mapping for this service (what the role can DO here). Kept server-side, never put in the JWT -
 * the token only carries the role name, exactly like auth-service.
 */
public enum Role {
    /** Platform user: manages tenants only, never sees tenant business data. */
    SUPER_ADMIN(EnumSet.noneOf(Fe.class)),
    /** Everything a tenant user can do, plus deactivate/reactivate and audit/history. */
    TENANT_ADMIN(EnumSet.allOf(Fe.class)),
    /** Day-to-day work: create/read/update members and groups, manage group membership. */
    TENANT_USER(EnumSet.of(
            MEMBER_READ, MEMBER_CREATE, MEMBER_UPDATE,
            GROUP_READ, GROUP_CREATE, GROUP_UPDATE,
            GROUP_MEMBER_MANAGE));

    private final Set<Fe> fe;

    Role(Set<Fe> fe) {
        this.fe = Collections.unmodifiableSet(fe);
    }

    public Set<Fe> fe() {
        return fe;
    }
}
