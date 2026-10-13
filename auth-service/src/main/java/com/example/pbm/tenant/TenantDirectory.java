package com.example.pbm.tenant;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Tenants are owned by tenant-service (platform.tenants, BIGINT ids). auth-service only reads them,
 * so the tenantId in the JWT is the same id every other service uses. Plain JDBC on purpose:
 * mapping it as a JPA entity would let ddl-auto touch a table this service doesn't own.
 */
@Component
public class TenantDirectory {
    private static final RowMapper<TenantInfo> ROW = (rs, i) -> new TenantInfo(
            rs.getLong("tenant_id"), rs.getString("tenant_name"), rs.getString("schema_name"), rs.getString("status"));

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate named;
    private final String select;

    public TenantDirectory(JdbcTemplate jdbc, @Value("${app.tenant-table:platform.tenants}") String table) {
        if (!table.matches("[a-z_][a-z0-9_]*(\\.[a-z_][a-z0-9_]*)?")) throw new IllegalArgumentException("Invalid app.tenant-table: " + table);
        this.jdbc = jdbc;
        this.named = new NamedParameterJdbcTemplate(jdbc);
        this.select = "select tenant_id, tenant_name, schema_name, status from " + table;
    }

    public Optional<TenantInfo> find(Long id) {
        return jdbc.query(select + " where tenant_id = ?", ROW, id).stream().findFirst();
    }
    public Optional<TenantInfo> findByName(String name) {
        return jdbc.query(select + " where tenant_name = ?", ROW, name).stream().findFirst();
    }
    public List<TenantInfo> findAll() {
        return jdbc.query(select + " order by tenant_id", ROW);
    }
    public List<TenantInfo> findAllById(Collection<Long> ids) {
        if (ids.isEmpty()) return List.of();
        return named.query(select + " where tenant_id in (:ids) order by tenant_id", Map.of("ids", ids), ROW);
    }
    /** Login, switch-tenant and admin assignment only work for ACTIVE tenants. */
    public TenantInfo requireActive(Long id) {
        TenantInfo t = find(id).orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Tenant " + id + " not found"));
        if (!t.isActive()) throw new org.springframework.security.access.AccessDeniedException("Tenant " + id + " is " + t.status());
        return t;
    }
}
