package com.medimpact.mt.tenancy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** On startup, makes sure every active tenant has its schema and tables. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.tenancy.auto-provision", havingValue = "true")
public class TenantProvisioningRunner implements ApplicationRunner {

    private final TenantRegistry tenantRegistry;
    private final TenantSchemaProvisioner provisioner;

    @Override
    public void run(ApplicationArguments args) {
        // Never block startup (e.g. tenant-service has not created platform.tenants yet);
        // tenants are also provisioned on their first request.
        try {
            for (TenantInfo tenant : tenantRegistry.findAllActive()) {
                try {
                    provisioner.ensureProvisioned(tenant.schemaName());
                } catch (RuntimeException e) {
                    log.warn("Skipped tenant {} ({}): {}", tenant.tenantId(), tenant.schemaName(), e.getMessage());
                }
            }
        } catch (RuntimeException e) {
            log.warn("Startup tenant provisioning skipped: {}", e.getMessage());
        }
    }
}
