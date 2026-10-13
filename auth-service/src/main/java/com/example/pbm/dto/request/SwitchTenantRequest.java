package com.example.pbm.dto.request;
import jakarta.validation.constraints.NotNull;
public record SwitchTenantRequest(@NotNull Long tenantId) {}
