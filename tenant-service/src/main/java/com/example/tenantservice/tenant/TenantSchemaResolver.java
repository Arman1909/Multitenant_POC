package com.example.tenantservice.tenant;

import com.example.tenantservice.entity.Tenant;
import com.example.tenantservice.entity.TenantStatus;
import com.example.tenantservice.exception.TenantNotActiveException;
import com.example.tenantservice.exception.TenantNotFoundException;
import com.example.tenantservice.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TenantSchemaResolver {

    private final TenantRepository tenantRepository;

    public String resolveActiveSchema(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new TenantNotActiveException(tenantId, tenant.getStatus());
        }
        return tenant.getSchemaName();
    }
}