package com.example.pbm.tenant;

/** Read-only view of a row in tenant-service's platform.tenants. */
public record TenantInfo(Long tenantId, String tenantName, String schemaName, String status) {
    public boolean isActive() { return "ACTIVE".equals(status); }
}
