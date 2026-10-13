package com.example.tenantservice.exception;

public class InvalidTenantNameException extends RuntimeException {
    public InvalidTenantNameException(String message) {
        super(message);
    }
}
