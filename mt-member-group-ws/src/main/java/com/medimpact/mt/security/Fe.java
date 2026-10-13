package com.medimpact.mt.security;

/** Functional entitlements (permissions) for this service. Same idea as auth-service's Fe enum. */
public enum Fe {
    MEMBER_READ, MEMBER_CREATE, MEMBER_UPDATE, MEMBER_DEACTIVATE, MEMBER_REACTIVATE,
    GROUP_READ, GROUP_CREATE, GROUP_UPDATE, GROUP_DEACTIVATE, GROUP_REACTIVATE,
    GROUP_MEMBER_MANAGE,
    /** Membership history (ended memberships) and other audit views. */
    AUDIT_READ
}
