package com.example.tenantservice.tenant;

import com.example.tenantservice.exception.TenantProvisioningException;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Coordinates the two physically-separate provisioning steps: (1) DDL schema
 * creation via plain JDBC, (2) a dedicated, on-demand Flyway run scoped to
 * that one schema, using migration scripts under db/migration/tenant.
 *
 * Deliberately NOT annotated @Transactional: CREATE SCHEMA and Flyway's own
 * migration runs each manage their own connections/commits. Wrapping this in
 * a single Spring @Transactional would not make it atomic (Flyway opens its
 * own connections outside of Spring's transaction synchronization), so we
 * don't pretend otherwise - TenantService instead tracks provisioning state
 * explicitly via TenantStatus and handles partial failure there.
 */
@Component
@Slf4j
public class TenantSchemaProvisioner {

    private final DataSource dataSource;
    private final SchemaNameGenerator schemaNameGenerator;
    private final String tenantMigrationLocation;

    public TenantSchemaProvisioner(DataSource dataSource,
                                    SchemaNameGenerator schemaNameGenerator,
                                    @Value("${tenant.provisioning.migration-locations}") String tenantMigrationLocation) {
        this.dataSource = dataSource;
        this.schemaNameGenerator = schemaNameGenerator;
        this.tenantMigrationLocation = tenantMigrationLocation;
    }

    /**
     * Creates the schema (idempotent) and runs tenant migrations against it.
     * Adding a new tenant table in future only requires a new versioned
     * script under db/migration/tenant - no code changes here.
     */
    public void provision(String schemaName) {
        schemaNameGenerator.validate(schemaName);

//        createSchema(schemaName);
        runTenantMigrations(schemaName);
    }

    /** Applies any pending tenant migrations to an already-provisioned schema. */
    public void migrate(String schemaName) {
        schemaNameGenerator.validate(schemaName);
        runTenantMigrations(schemaName);
    }

//    private void createSchema(String schemaName) {
//        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
//        try {
//            // schemaName was generated + re-validated against a strict
//            // [a-z][a-z0-9_]* allow-list, so quoting it here is safe; it is
//            // never built by concatenating the raw, user-supplied tenant name.
//            jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS \"" + schemaName + "\"");
//            log.info("Created schema '{}'", schemaName);
//        } catch (Exception e) {
//            throw new TenantProvisioningException(
//                    "Failed to create schema '" + schemaName + "'", e);
//        }
//    }

    private void runTenantMigrations(String schemaName) {
        try {
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .schemas(schemaName)
                    .locations(tenantMigrationLocation)
                    // Each tenant schema has its own independent Flyway
                    // history table, so tenants never interfere with one
                    // another's migration state.
                    .createSchemas(true)
                    .table("flyway_schema_history")
                    .baselineOnMigrate(false)
                    .load();

            flyway.migrate();
            log.info("Applied tenant migrations to schema '{}'", schemaName);
        } catch (Exception e) {
            throw new TenantProvisioningException(
                    "Failed to run tenant migrations for schema '" + schemaName + "'", e);
        }
    }
}
