package com.example.tenantservice.exception;

public class DuplicateTenantException extends RuntimeException {
    public DuplicateTenantException(String tenantName) {
        super("A tenant named '" + tenantName + "' already exists");
    }
}
