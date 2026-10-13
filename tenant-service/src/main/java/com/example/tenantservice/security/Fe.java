package com.example.tenantservice.security;

/** Functional entitlements (permissions) for this service. Same idea as auth-service's Fe enum. */
public enum Fe {
    TENANT_CREATE, TENANT_READ, TENANT_UPDATE, TENANT_SUSPEND, TENANT_REACTIVATE, TENANT_DEACTIVATE,
    MEMBER_READ
}
