package com.example.pbm.dto.request;
import jakarta.validation.constraints.*;
/** Password reset by a TENANT_ADMIN (for TENANT_USERs of the active tenant, or the admin themselves). */
public record UpdateUserRequest(@NotBlank @Size(min = 8, max = 100) String password) {}
