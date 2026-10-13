package com.example.tenantservice.exception;

public class InvalidDeRoleException extends RuntimeException {
    public InvalidDeRoleException(String deRole) {
        super("X-DE-ROLE '" + deRole + "' is not a valid role (expected e.g. DE_LAKER_MBR or DE_LAKER_ALL)");
    }
}