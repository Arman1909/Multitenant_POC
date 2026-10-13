package com.example.pbm.dto.response;
import com.example.pbm.tenant.TenantInfo;
public record TenantResponse(Long id, String name, String status) {
    public static TenantResponse from(TenantInfo t) { return new TenantResponse(t.tenantId(), t.tenantName(), t.status()); }
}
