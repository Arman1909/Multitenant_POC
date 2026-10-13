package com.medimpact.mt.tenancy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adds this service's tables (member_group, group_membership) to a tenant schema.
 * The schema itself and the member table are created by tenant-service when the tenant is provisioned,
 * so this only runs once that has happened. The DDL is idempotent.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantSchemaProvisioner {

    private static final Resource TENANT_DDL = new ClassPathResource("db/tenant/tenant-schema.sql");

    private final DataSource dataSource;
    private final Set<String> provisioned = ConcurrentHashMap.newKeySet();

    public void ensureProvisioned(String schema) {
        if (provisioned.contains(schema)) {
            return;
        }
        synchronized (this) {
            if (!provisioned.contains(schema)) {
                provision(schema);
            }
        }
    }

    private void provision(String schema) {
        if (!TenantRegistry.SCHEMA_NAME.matcher(schema).matches()) {
            throw new IllegalArgumentException("Invalid schema name: " + schema);
        }
        try (Connection connection = dataSource.getConnection()) {
            if (!memberTableExists(connection, schema)) {
                throw new IllegalStateException("Schema " + schema + " has no member table yet - "
                        + "the tenant must be provisioned by tenant-service first");
            }
            String originalSchema = connection.getSchema();
            connection.setSchema(schema);
            try {
                ScriptUtils.executeSqlScript(connection, TENANT_DDL);
            } finally {
                connection.setSchema(originalSchema);
            }
            provisioned.add(schema);
            log.info("Member/group tables ready in tenant schema {}", schema);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to provision schema " + schema, e);
        }
    }

    private static boolean memberTableExists(Connection connection, String schema) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "select 1 from information_schema.tables where table_schema = ? and table_name = 'member'")) {
            ps.setString(1, schema);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
