package com.medimpact.mt.config;

import com.medimpact.mt.tenancy.SchemaPerTenantConnectionProvider;
import com.medimpact.mt.tenancy.TenantIdentifierResolver;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Enables Hibernate schema-per-tenant multi-tenancy (same wiring as tenant-service's MultiTenancyConfig). */
@Configuration
public class HibernateTenancyConfig {

    @Bean
    HibernatePropertiesCustomizer multiTenancyCustomizer(SchemaPerTenantConnectionProvider connectionProvider,
                                                         TenantIdentifierResolver identifierResolver) {
        return properties -> {
            properties.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, connectionProvider);
            properties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, identifierResolver);
        };
    }
}
