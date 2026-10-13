package com.example.pbm.entity;
import java.util.*;
/** ROLE -> FE mapping (what the role can DO). Kept server-side, never put in the JWT. */
public enum Role {
    SUPER_ADMIN(Fe.TENANT_READ, Fe.TENANT_ASSIGN_ADMIN),
    TENANT_ADMIN(Fe.USER_CREATE, Fe.USER_READ, Fe.USER_UPDATE, Fe.USER_DEACTIVATE, Fe.USER_REACTIVATE,
            Fe.MEMBER_READ, Fe.CLAIM_READ),
    TENANT_USER(Fe.USER_READ, Fe.MEMBER_READ, Fe.CLAIM_READ);
    private final Set<Fe> fe;
    Role(Fe... fe) { this.fe = EnumSet.copyOf(Arrays.asList(fe)); }
    public Set<Fe> fe() { return fe; }
}
