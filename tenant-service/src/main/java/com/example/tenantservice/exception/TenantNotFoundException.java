package com.example.tenantservice.exception;

public class TenantNotFoundException extends RuntimeException {
    public TenantNotFoundException(Long tenantId) {
        super("Tenant not found: " + tenantId);
    }
}
