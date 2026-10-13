package com.example.tenantservice.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * PATCH semantics: every field is optional; null means "leave unchanged".
 * Note tenantName may be updated for display purposes, but schemaName is
 * immutable once provisioned (see Tenant entity / SchemaNameGenerator).
 */
public record TenantUpdateRequest(
        @Size(max = 120) String tenantName,
        @Size(max = 500) String tenantDesc,
        LocalDate startDate,
        LocalDate endDate,
        @Size(max = 200) String tenantAddress1,
        @Size(max = 200) String tenantAddress2,
        @Size(max = 100) String tenantCity,
        @Size(max = 100) String tenantState,
        @Size(max = 20)  String tenantPostalCode
) {
}
