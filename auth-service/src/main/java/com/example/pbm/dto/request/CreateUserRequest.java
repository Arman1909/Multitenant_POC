package com.example.pbm.dto.request;
import jakarta.validation.constraints.*;
/** No tenantId on purpose: the tenant always comes from the JWT. Creates a TENANT_USER. */
public record CreateUserRequest(@NotBlank @Size(max = 100) String username, @NotBlank @Size(min = 8, max = 100) String password) {}
