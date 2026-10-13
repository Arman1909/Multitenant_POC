package com.example.tenantservice.tenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

/**
 * Non-deprecated Hibernate 6 SPI. Registered with Hibernate via
 * MultiTenancyConfig (hibernate.tenant_identifier_resolver property), not via
 * the legacy `hibernate.multiTenancy` setting, which no longer exists in
 * Hibernate 6.
 */
@Component
public class TenantIdentifierResolverImpl implements CurrentTenantIdentifierResolver<String> {

    /** Schema platform.tenants itself lives in; used whenever no tenant context is set. */
    public static final String DEFAULT_TENANT = "public";

    @Override
    public String resolveCurrentTenantIdentifier() {
        String tenant = TenantContext.getCurrentTenant();
        return tenant != null ? tenant : DEFAULT_TENANT;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        // Force Hibernate to open a fresh Session rather than silently reuse
        // one bound to a different tenant on the same thread.
        return true;
    }
}
