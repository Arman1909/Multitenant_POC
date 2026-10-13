package com.medimpact.mt.tenancy;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties("app.tenancy")
public record TenancyProperties(
        String tenantTable,
        List<String> activeStatuses,
        boolean autoProvision,
        Duration cacheTtl) {

    public TenancyProperties {
        tenantTable = tenantTable == null ? "platform.tenants" : tenantTable;
        if (!tenantTable.matches("[a-z_][a-z0-9_]*(\\.[a-z_][a-z0-9_]*)?")) {
            throw new IllegalArgumentException("Invalid app.tenancy.tenant-table: " + tenantTable);
        }
        activeStatuses = activeStatuses == null || activeStatuses.isEmpty() ? List.of("ACTIVE") : activeStatuses;
        cacheTtl = cacheTtl == null ? Duration.ofMinutes(5) : cacheTtl;
    }
}
