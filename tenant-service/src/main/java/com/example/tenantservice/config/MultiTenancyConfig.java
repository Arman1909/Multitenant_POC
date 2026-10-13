package com.example.tenantservice.config;

import com.example.tenantservice.tenant.MultiTenantConnectionProviderImpl;
import com.example.tenantservice.tenant.TenantIdentifierResolverImpl;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Hibernate 6 removed the old `hibernate.multiTenancy=SCHEMA` enum setting;
 * multi-tenancy is now activated implicitly by supplying these two SPI beans
 * via hibernate properties. This is the supported Spring Boot 3 / Hibernate 6
 * wiring mechanism - no deprecated APIs are used.
 */
@Configuration
public class MultiTenancyConfig {

    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(
            MultiTenantConnectionProviderImpl multiTenantConnectionProvider,
            TenantIdentifierResolverImpl tenantIdentifierResolver) {

        return hibernateProperties -> {
            hibernateProperties.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER,
                    multiTenantConnectionProvider);
            hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER,
                    tenantIdentifierResolver);
        };
    }
}
