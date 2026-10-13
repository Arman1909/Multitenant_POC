package com.medimpact.mt.context;

/**
 * Caller identity for the current request, taken from the validated JWT (sub, username, role, deRole, tenantId).
 * {@code tenantId}/{@code schema} are null for PLATFORM callers (SUPER_ADMIN), which have no access to tenant data.
 */
public record CallerContext(String userId, String username, String role, String deRole,
                            Long tenantId, String schema) {
}
