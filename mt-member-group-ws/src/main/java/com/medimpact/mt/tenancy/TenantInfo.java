package com.medimpact.mt.tenancy;

import java.time.LocalDate;

/** Read-only view of a row in the platform tenants table (owned by the tenant service). */
public record TenantInfo(Long tenantId, String tenantName, String schemaName, String status,
                         LocalDate startDate, LocalDate endDate) {
}
