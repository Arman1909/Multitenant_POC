package com.example.pbm.dto.request;
import jakarta.validation.constraints.*;
/** password is only needed when the admin does not exist yet. */
public record AssignTenantAdminRequest(@NotBlank @Size(max = 100) String username, @Size(min = 8, max = 100) String password) {}
