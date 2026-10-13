package com.example.tenantservice.controller;

import com.example.tenantservice.dto.TenantCreateRequest;
import com.example.tenantservice.dto.TenantResponse;
import com.example.tenantservice.dto.TenantUpdateRequest;
import com.example.tenantservice.security.CurrentCaller;
import com.example.tenantservice.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * SUPER_ADMIN only (see security.AccessRules). The caller is authenticated by the auth-service JWT;
 * its "username" claim is used for the audit trail (entered_by / updated_by).
 */
@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @PostMapping
    public ResponseEntity<TenantResponse> createTenant(@Valid @RequestBody TenantCreateRequest request) {
        TenantResponse response = tenantService.createTenant(request, CurrentCaller.username());
        return ResponseEntity
                .created(URI.create("/api/v1/tenants/" + response.tenantId()))
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<TenantResponse>> listTenants() {
        return ResponseEntity.ok(tenantService.listTenants());
    }

    @GetMapping("/{tenantId}")
    public ResponseEntity<TenantResponse> getTenant(@PathVariable Long tenantId) {
        return ResponseEntity.ok(tenantService.getTenant(tenantId));
    }

    @PatchMapping("/{tenantId}")
    public ResponseEntity<TenantResponse> updateTenant(@PathVariable Long tenantId,
                                                       @Valid @RequestBody TenantUpdateRequest request) {
        return ResponseEntity.ok(tenantService.updateTenant(tenantId, request, CurrentCaller.username()));
    }

    @PostMapping("/{tenantId}/suspend")
    public ResponseEntity<TenantResponse> suspendTenant(@PathVariable Long tenantId) {
        return ResponseEntity.ok(tenantService.suspendTenant(tenantId, CurrentCaller.username()));
    }

    @PostMapping("/{tenantId}/reactivate")
    public ResponseEntity<TenantResponse> reactivateTenant(@PathVariable Long tenantId) {
        return ResponseEntity.ok(tenantService.reactivateTenant(tenantId, CurrentCaller.username()));
    }

    @DeleteMapping("/{tenantId}")
    public ResponseEntity<TenantResponse> deactivateTenant(@PathVariable Long tenantId) {
        return ResponseEntity.ok(tenantService.deactivateTenant(tenantId, CurrentCaller.username()));
    }
}
