package com.example.pbm.dto.request;
import jakarta.validation.constraints.NotBlank;
/** tenantId = tenant-service tenant_id; required for TENANT_ADMIN, optional for TENANT_USER, ignored for SUPER_ADMIN. */
public record LoginRequest(@NotBlank String username, @NotBlank String password, Long tenantId) {}
