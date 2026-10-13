package com.example.tenantservice.service;

import com.example.tenantservice.dto.TenantCreateRequest;
import com.example.tenantservice.dto.TenantResponse;
import com.example.tenantservice.dto.TenantUpdateRequest;
import com.example.tenantservice.entity.Tenant;
import com.example.tenantservice.entity.TenantStatus;
import com.example.tenantservice.exception.DuplicateTenantException;
import com.example.tenantservice.exception.InvalidTenantStateTransitionException;
import com.example.tenantservice.exception.TenantNotFoundException;
import com.example.tenantservice.repository.TenantRepository;
import com.example.tenantservice.tenant.SchemaNameGenerator;
import com.example.tenantservice.tenant.TenantSchemaProvisioner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * Provisioning is split into three separate committed steps because the
 * schema + Flyway step is not part of Spring's JPA transaction:
 *
 *   1. insert Tenant row, status=PROVISIONING   (own transaction, committed)
 *   2. create schema + run Flyway               (no transaction)
 *   3. mark ACTIVE, or PROVISIONING_FAILED      (own transaction, committed)
 *
 * TransactionTemplate is used instead of @Transactional on private/self-invoked
 * methods, because Spring's proxy-based @Transactional is bypassed on
 * this.method() calls.
 */
@Service
@Slf4j
public class TenantService {

    private final TenantRepository tenantRepository;
    private final SchemaNameGenerator schemaNameGenerator;
    private final TenantSchemaProvisioner tenantSchemaProvisioner;
    private final TransactionTemplate requiresNewTx;

    public TenantService(TenantRepository tenantRepository,
                         SchemaNameGenerator schemaNameGenerator,
                         TenantSchemaProvisioner tenantSchemaProvisioner,
                         PlatformTransactionManager transactionManager) {
        this.tenantRepository = tenantRepository;
        this.schemaNameGenerator = schemaNameGenerator;
        this.tenantSchemaProvisioner = tenantSchemaProvisioner;
        this.requiresNewTx = new TransactionTemplate(transactionManager);
        this.requiresNewTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public TenantResponse createTenant(TenantCreateRequest request, String username) {
        String schemaName = schemaNameGenerator.generate(request.tenantName());

        Tenant tenant = insertProvisioningRecord(request, schemaName, username);

        try {
            tenantSchemaProvisioner.provision(schemaName);
        } catch (RuntimeException e) {
            log.error("Provisioning failed for tenant '{}' (schema '{}')",
                    request.tenantName(), schemaName, e);
            markFailed(tenant.getTenantId(), username);
            throw e;
        }

        markActive(tenant.getTenantId(), username);

        return TenantResponse.from(tenantRepository.findById(tenant.getTenantId())
                .orElseThrow(() -> new TenantNotFoundException(tenant.getTenantId())));
    }

    private Tenant insertProvisioningRecord(TenantCreateRequest request, String schemaName, String username) {
        return requiresNewTx.execute(status -> {
            if (tenantRepository.existsByTenantNameIgnoreCase(request.tenantName())
                    || tenantRepository.existsBySchemaName(schemaName)) {
                throw new DuplicateTenantException(request.tenantName());
            }
            Tenant tenant = Tenant.newProvisioning(
                    request.tenantName(), request.tenantDesc(), schemaName,
                    request.startDate(), request.endDate(),
                    request.tenantAddress1(), request.tenantAddress2(), request.tenantCity(),
                    request.tenantState(), request.tenantPostalCode(), username);
            return tenantRepository.save(tenant);
        });
    }

    private void markActive(Long tenantId, String username) {
        requiresNewTx.executeWithoutResult(status -> {
            Tenant tenant = tenantRepository.findByTenantId(tenantId)
                    .orElseThrow(() -> new TenantNotFoundException(tenantId));
            tenant.setStatus(TenantStatus.ACTIVE);
            tenant.touch(username);
        });
    }

    private void markFailed(Long tenantId, String username) {
        requiresNewTx.executeWithoutResult(status ->
                tenantRepository.findByTenantId(tenantId).ifPresent(tenant -> {
                    tenant.setStatus(TenantStatus.PROVISIONING_FAILED);
                    tenant.touch(username);
                }));
    }

    @Transactional(readOnly = true)
    public TenantResponse getTenant(Long tenantId) {
        return TenantResponse.from(findOrThrow(tenantId));
    }

    @Transactional(readOnly = true)
    public List<TenantResponse> listTenants() {
        return tenantRepository.findAll().stream()
                .map(TenantResponse::from)
                .toList();
    }

    @Transactional
    public TenantResponse updateTenant(Long tenantId, TenantUpdateRequest request, String username) {
        Tenant tenant = findOrThrow(tenantId);

        if (tenant.getStatus() == TenantStatus.DEACTIVATED) {
            throw new InvalidTenantStateTransitionException(tenant.getStatus(), tenant.getStatus());
        }

        if (request.tenantName() != null && !request.tenantName().equalsIgnoreCase(tenant.getTenantName())) {
            if (tenantRepository.existsByTenantNameIgnoreCase(request.tenantName())) {
                throw new DuplicateTenantException(request.tenantName());
            }
            tenant.setTenantName(request.tenantName());
            // schemaName is NOT regenerated - it is immutable once provisioned.
        }
        if (request.tenantDesc() != null) tenant.setTenantDesc(request.tenantDesc());
        if (request.startDate() != null) tenant.setStartDate(request.startDate());
        if (request.endDate() != null) tenant.setEndDate(request.endDate());
        if (request.tenantAddress1() != null) tenant.setTenantAddress1(request.tenantAddress1());
        if (request.tenantAddress2() != null) tenant.setTenantAddress2(request.tenantAddress2());
        if (request.tenantCity() != null) tenant.setTenantCity(request.tenantCity());
        if (request.tenantState() != null) tenant.setTenantState(request.tenantState());
        if (request.tenantPostalCode() != null) tenant.setTenantPostalCode(request.tenantPostalCode());

        tenant.touch(username);
        return TenantResponse.from(tenant);
    }

    @Transactional
    public TenantResponse suspendTenant(Long tenantId, String username) {
        Tenant tenant = tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
        requireStatus(tenant, TenantStatus.ACTIVE, TenantStatus.SUSPENDED);
        tenant.setStatus(TenantStatus.SUSPENDED);
        tenant.touch(username);
        return TenantResponse.from(tenant);
    }

    @Transactional
    public TenantResponse reactivateTenant(Long tenantId, String username) {
        Tenant tenant = tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
        requireStatus(tenant, TenantStatus.SUSPENDED, TenantStatus.ACTIVE);
        tenant.setStatus(TenantStatus.ACTIVE);
        tenant.touch(username);
        return TenantResponse.from(tenant);
    }

    @Transactional
    public TenantResponse deactivateTenant(Long tenantId, String username) {
        Tenant tenant = tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
        if (tenant.getStatus() == TenantStatus.DEACTIVATED) {
            return TenantResponse.from(tenant); // idempotent
        }
        tenant.setStatus(TenantStatus.DEACTIVATED);
        tenant.touch(username);
        return TenantResponse.from(tenant);
    }

    private void requireStatus(Tenant tenant, TenantStatus required, TenantStatus target) {
        if (tenant.getStatus() != required) {
            throw new InvalidTenantStateTransitionException(tenant.getStatus(), target);
        }
    }

    private Tenant findOrThrow(Long tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
    }
}