package com.example.tenantservice.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenantIdentifierResolverImplTest {

    private final TenantIdentifierResolverImpl resolver = new TenantIdentifierResolverImpl();

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void returnsDefaultTenantWhenNoContextSet() {
        assertThat(resolver.resolveCurrentTenantIdentifier())
                .isEqualTo(TenantIdentifierResolverImpl.DEFAULT_TENANT);
    }

    @Test
    void returnsTenantSchemaWhenContextSet() {
        TenantContext.setCurrentTenant("tenant_laker");
        assertThat(resolver.resolveCurrentTenantIdentifier()).isEqualTo("tenant_laker");
    }

    @Test
    void alwaysValidatesExistingSessions() {
        assertThat(resolver.validateExistingCurrentSessions()).isTrue();
    }
}
