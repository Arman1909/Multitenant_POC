package com.medimpact.mt.tenancy;

import com.medimpact.mt.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/** Resolves tenantId -> schema from tenant-service's platform.tenants table, with a short-lived cache. */
@Component
public class TenantRegistry {

    /** Postgres identifier rules, lower-case only, so the value is safe to use in SET search_path / CREATE SCHEMA. */
    static final Pattern SCHEMA_NAME = Pattern.compile("[a-z_][a-z0-9_]{0,62}");

    private static final RowMapper<TenantInfo> ROW_MAPPER = (rs, i) -> new TenantInfo(
            rs.getLong("tenant_id"),
            rs.getString("tenant_name"),
            rs.getString("schema_name"),
            rs.getString("status"),
            rs.getObject("start_date", LocalDate.class),
            rs.getObject("end_date", LocalDate.class));

    private final JdbcTemplate jdbcTemplate;
    private final TenancyProperties properties;
    private final TenantSchemaProvisioner provisioner;
    private final String selectColumns;
    private final Map<Long, CachedTenant> cache = new ConcurrentHashMap<>();

    public TenantRegistry(JdbcTemplate jdbcTemplate, TenancyProperties properties, TenantSchemaProvisioner provisioner) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
        this.provisioner = provisioner;
        this.selectColumns = "select tenant_id, tenant_name, schema_name, status, start_date, end_date from "
                + properties.tenantTable();
    }

    /** Returns the tenant if it exists, is active and in its date range; provisions its schema on first use. */
    public TenantInfo resolveActive(Long tenantId) {
        TenantInfo tenant = find(tenantId);
        if (!isActive(tenant)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tenant " + tenantId + " is not active");
        }
        if (properties.autoProvision()) {
            try {
                provisioner.ensureProvisioned(tenant.schemaName());
            } catch (IllegalStateException e) {
                throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
            }
        }
        return tenant;
    }

    public TenantInfo find(Long tenantId) {
        CachedTenant cached = cache.get(tenantId);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return cached.tenant();
        }
        List<TenantInfo> rows = jdbcTemplate.query(selectColumns + " where tenant_id = ?", ROW_MAPPER, tenantId);
        if (rows.isEmpty()) {
            cache.remove(tenantId);
            throw new ApiException(HttpStatus.NOT_FOUND, "Tenant " + tenantId + " not found");
        }
        TenantInfo tenant = validate(rows.getFirst());
        cache.put(tenantId, new CachedTenant(tenant, Instant.now().plus(properties.cacheTtl())));
        return tenant;
    }

    public List<TenantInfo> findAllActive() {
        return jdbcTemplate.query(selectColumns, ROW_MAPPER).stream()
                .filter(this::isActive)
                .map(TenantRegistry::validate)
                .toList();
    }

    public void evict(Long tenantId) {
        cache.remove(tenantId);
    }

    private boolean isActive(TenantInfo tenant) {
        LocalDate today = LocalDate.now();
        return properties.activeStatuses().stream().anyMatch(s -> s.equalsIgnoreCase(tenant.status()))
                && (tenant.startDate() == null || !tenant.startDate().isAfter(today))
                && (tenant.endDate() == null || !tenant.endDate().isBefore(today));
    }

    private static TenantInfo validate(TenantInfo tenant) {
        if (tenant.schemaName() == null || !SCHEMA_NAME.matcher(tenant.schemaName()).matches()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Tenant " + tenant.tenantId() + " has an invalid schema name");
        }
        return tenant;
    }

    private record CachedTenant(TenantInfo tenant, Instant expiresAt) {
    }
}
