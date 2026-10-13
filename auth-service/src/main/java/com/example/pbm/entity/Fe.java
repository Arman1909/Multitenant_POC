package com.example.pbm.entity;
/** Tenant create/manage/deactivate is owned by tenant-service, so there is no TENANT_CREATE here any more. */
public enum Fe {
    TENANT_READ, TENANT_ASSIGN_ADMIN,
    USER_CREATE, USER_READ, USER_UPDATE, USER_DEACTIVATE, USER_REACTIVATE,
    MEMBER_READ, CLAIM_READ
}
