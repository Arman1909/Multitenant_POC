package com.example.tenantservice.entity;

/**
 * Tenant lifecycle. A tenant is only ever queryable/usable by the rest of the
 * platform once it reaches ACTIVE; PROVISIONING/PROVISIONING_FAILED are
 * transient/terminal-failure states surfaced for operator visibility.
 */
public enum TenantStatus {
    PROVISIONING,
    ACTIVE,
    PROVISIONING_FAILED,
    SUSPENDED,
    DEACTIVATED
}
