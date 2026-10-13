package com.example.tenantservice.exception;

public class MemberNotFoundException extends RuntimeException {
    public MemberNotFoundException(Long memberId) {
        super("Member not found: " + memberId);
    }
}