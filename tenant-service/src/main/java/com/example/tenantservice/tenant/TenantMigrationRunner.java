package com.example.tenantservice.tenant;

import com.example.tenantservice.entity.TenantStatus;
import com.example.tenantservice.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TenantMigrationRunner implements ApplicationRunner {

    private final TenantRepository tenantRepository;
    private final TenantSchemaProvisioner provisioner;

    @Override
    public void run(ApplicationArguments args) {
        tenantRepository.findAll().stream()
                .filter(t -> t.getStatus() == TenantStatus.ACTIVE || t.getStatus() == TenantStatus.SUSPENDED)
                .forEach(t -> {
                    try {
                        provisioner.migrate(t.getSchemaName());
                    } catch (Exception e) {
                        // one bad tenant must not block the others from starting
                        log.error("Startup migration failed for schema '{}'", t.getSchemaName(), e);
                    }
                });
    }
}