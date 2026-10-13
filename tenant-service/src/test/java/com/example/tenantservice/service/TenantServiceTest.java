package com.example.tenantservice.service;

import com.example.tenantservice.dto.TenantCreateRequest;
import com.example.tenantservice.dto.TenantResponse;
import com.example.tenantservice.entity.Tenant;
import com.example.tenantservice.entity.TenantStatus;
import com.example.tenantservice.exception.DuplicateTenantException;
import com.example.tenantservice.exception.InvalidTenantStateTransitionException;
import com.example.tenantservice.exception.TenantNotFoundException;
import com.example.tenantservice.exception.TenantProvisioningException;
import com.example.tenantservice.repository.TenantRepository;
import com.example.tenantservice.tenant.SchemaNameGenerator;
import com.example.tenantservice.tenant.TenantSchemaProvisioner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private SchemaNameGenerator schemaNameGenerator;
    @Mock private TenantSchemaProvisioner tenantSchemaProvisioner;

    @InjectMocks
    private TenantService tenantService;

    private TenantCreateRequest request;

    @BeforeEach
    void setUp() {
        request = new TenantCreateRequest("Laker", "desc", null, null,
                null, null, null, null, null);
    }

    @Test
    void createTenant_happyPath_marksActive() {
        when(schemaNameGenerator.generate("Laker")).thenReturn("tenant_laker");
        when(tenantRepository.existsByTenantNameIgnoreCase("Laker")).thenReturn(false);
        when(tenantRepository.existsBySchemaName("tenant_laker")).thenReturn(false);

        Tenant saved = Tenant.newProvisioning("Laker", "desc", "tenant_laker",
                null, null, null, null, null, null, null, "alice");
        setId(saved, 1L);
        when(tenantRepository.save(any(Tenant.class))).thenReturn(saved);
        when(tenantRepository.findByTenantId(1L)).thenReturn(Optional.of(saved));
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(saved));

        TenantResponse response = tenantService.createTenant(request, "alice");

        assertThat(response.status()).isEqualTo("ACTIVE");
        verify(tenantSchemaProvisioner).provision("tenant_laker");
        assertThat(saved.getStatus()).isEqualTo(TenantStatus.ACTIVE);
    }

    @Test
    void createTenant_duplicateName_throwsBeforeProvisioning() {
        when(schemaNameGenerator.generate("Laker")).thenReturn("tenant_laker");
        when(tenantRepository.existsByTenantNameIgnoreCase("Laker")).thenReturn(true);

        assertThatThrownBy(() -> tenantService.createTenant(request, "alice"))
                .isInstanceOf(DuplicateTenantException.class);

        verifyNoInteractions(tenantSchemaProvisioner);
    }

    @Test
    void createTenant_provisioningFails_marksFailedAndPropagates() {
        when(schemaNameGenerator.generate("Laker")).thenReturn("tenant_laker");
        when(tenantRepository.existsByTenantNameIgnoreCase("Laker")).thenReturn(false);
        when(tenantRepository.existsBySchemaName("tenant_laker")).thenReturn(false);

        Tenant saved = Tenant.newProvisioning("Laker", "desc", "tenant_laker",
                null, null, null, null, null, null, null, "alice");
        setId(saved, 1L);
        when(tenantRepository.save(any(Tenant.class))).thenReturn(saved);
        when(tenantRepository.findByTenantId(1L)).thenReturn(Optional.of(saved));

        doThrow(new TenantProvisioningException("boom", new RuntimeException()))
                .when(tenantSchemaProvisioner).provision("tenant_laker");

        assertThatThrownBy(() -> tenantService.createTenant(request, "alice"))
                .isInstanceOf(TenantProvisioningException.class);

        assertThat(saved.getStatus()).isEqualTo(TenantStatus.PROVISIONING_FAILED);
    }

    @Test
    void suspendTenant_fromActive_succeeds() {
        Tenant tenant = activeTenant();
        when(tenantRepository.findByTenantId(1L)).thenReturn(Optional.of(tenant));

        TenantResponse response = tenantService.suspendTenant(1L, "alice");

        assertThat(response.status()).isEqualTo("SUSPENDED");
    }

    @Test
    void suspendTenant_fromNonActive_throws() {
        Tenant tenant = activeTenant();
        tenant.setStatus(TenantStatus.SUSPENDED);
        when(tenantRepository.findByTenantId(1L)).thenReturn(Optional.of(tenant));

        assertThatThrownBy(() -> tenantService.suspendTenant(1L, "alice"))
                .isInstanceOf(InvalidTenantStateTransitionException.class);
    }

    @Test
    void getTenant_notFound_throws() {
        when(tenantRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.getTenant(99L))
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void deactivateTenant_isIdempotent() {
        Tenant tenant = activeTenant();
        tenant.setStatus(TenantStatus.DEACTIVATED);
        when(tenantRepository.findByTenantId(1L)).thenReturn(Optional.of(tenant));

        TenantResponse first = tenantService.deactivateTenant(1L, "alice");
        TenantResponse second = tenantService.deactivateTenant(1L, "alice");

        assertThat(first.status()).isEqualTo("DEACTIVATED");
        assertThat(second.status()).isEqualTo("DEACTIVATED");
    }

    private Tenant activeTenant() {
        Tenant tenant = Tenant.newProvisioning("Laker", "desc", "tenant_laker",
                null, null, null, null, null, null, null, "alice");
        setId(tenant, 1L);
        tenant.setStatus(TenantStatus.ACTIVE);
        return tenant;
    }

    private void setId(Tenant tenant, Long id) {
        try {
            var field = Tenant.class.getDeclaredField("tenantId");
            field.setAccessible(true);
            field.set(tenant, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
