package com.example.tenantservice.exception;

import com.example.tenantservice.entity.TenantStatus;

public class InvalidTenantStateTransitionException extends RuntimeException {
    public InvalidTenantStateTransitionException(TenantStatus from, TenantStatus to) {
        super("Cannot transition tenant from " + from + " to " + to);
    }
}
