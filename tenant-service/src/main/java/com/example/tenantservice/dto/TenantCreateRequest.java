package com.example.tenantservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TenantCreateRequest(

        @NotBlank(message = "tenantName is required")
        @Size(max = 120, message = "tenantName must be at most 120 characters")
        String tenantName,

        @Size(max = 500, message = "tenantDesc must be at most 500 characters")
        String tenantDesc,

        LocalDate startDate,
        LocalDate endDate,

        @Size(max = 200) String tenantAddress1,
        @Size(max = 200) String tenantAddress2,
        @Size(max = 100) String tenantCity,
        @Size(max = 100) String tenantState,
        @Size(max = 20)  String tenantPostalCode
) {
}
