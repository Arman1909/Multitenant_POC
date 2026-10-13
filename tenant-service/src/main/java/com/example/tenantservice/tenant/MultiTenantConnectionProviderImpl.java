package com.example.tenantservice.tenant;

import lombok.RequiredArgsConstructor;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * There is exactly ONE PostgreSQL DataSource/connection pool for the whole
 * service (per the "shared database, schema-per-tenant" requirement) - this
 * is intentionally NOT one of the per-tenant-DataSource implementations.
 *
 * Tenant routing is done by calling Connection#setSchema(...) on a connection
 * checked out from the shared pool, and resetting it back to the default
 * schema before the connection is returned to the pool. setSchema() just sets
 * the session's search_path on the server and is safe to call per checkout -
 * HikariCP never hands out a connection concurrently to two threads, so there
 * is no cross-thread race, and resetting on release means a connection can
 * never leave the pool "pointed" at a stale tenant.
 */
@Component
@RequiredArgsConstructor
public class MultiTenantConnectionProviderImpl implements MultiTenantConnectionProvider<String> {

    private static final String DEFAULT_SCHEMA = TenantIdentifierResolverImpl.DEFAULT_TENANT;

    private final DataSource dataSource;

    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        resetAndClose(connection);
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        Connection connection = dataSource.getConnection();
        try {
            connection.setSchema(tenantIdentifier);
        } catch (SQLException e) {
            // Never leave a half-configured connection in the pool.
            connection.close();
            throw new SQLException("Unable to switch connection to tenant schema '"
                    + tenantIdentifier + "'", e);
        }
        return connection;
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        resetAndClose(connection);
    }

    private void resetAndClose(Connection connection) throws SQLException {
        try {
            if (!connection.isClosed()) {
                connection.setSchema(DEFAULT_SCHEMA);
            }
        } finally {
            connection.close();
        }
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        throw new UnsupportedOperationException("Cannot unwrap MultiTenantConnectionProviderImpl as " + unwrapType);
    }
}
