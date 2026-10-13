package com.example.tenantservice.exception;

import com.example.tenantservice.entity.TenantStatus;

public class TenantNotActiveException extends RuntimeException {
    public TenantNotActiveException(Long tenantId, TenantStatus status) {
        super("Tenant " + tenantId + " is " + status + " and cannot serve requests");
    }
}