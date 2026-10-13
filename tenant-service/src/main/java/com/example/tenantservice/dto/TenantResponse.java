package com.example.tenantservice.dto;

import com.example.tenantservice.entity.Tenant;

import java.time.Instant;
import java.time.LocalDate;

public record TenantResponse(
        Long tenantId,
        String tenantName,
        String tenantDesc,
        String schemaName,
        LocalDate startDate,
        LocalDate endDate,
        String tenantAddress1,
        String tenantAddress2,
        String tenantCity,
        String tenantState,
        String tenantPostalCode,
        String status,
        Instant entered,
        String enteredBy,
        Instant updatedAt,
        String updatedBy
) {
    public static TenantResponse from(Tenant t) {
        return new TenantResponse(
                t.getTenantId(), t.getTenantName(), t.getTenantDesc(), t.getSchemaName(),
                t.getStartDate(), t.getEndDate(),
                t.getTenantAddress1(), t.getTenantAddress2(), t.getTenantCity(),
                t.getTenantState(), t.getTenantPostalCode(),
                t.getStatus().name(), t.getEntered(), t.getEnteredBy(),
                t.getUpdatedAt(), t.getUpdatedBy()
        );
    }
}
