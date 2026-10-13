package com.example.pbm.controller;
import com.example.pbm.dto.request.*;
import com.example.pbm.dto.response.*;
import com.example.pbm.service.TenantService;
import jakarta.validation.Valid;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Tenant create/manage/deactivate lives in tenant-service; this only lists tenants and assigns admins. */
@RestController @RequestMapping("/api/v1/tenants") @RequiredArgsConstructor
public class TenantController {
    private final TenantService svc;
    @GetMapping @PreAuthorize("hasAuthority('TENANT_READ')")
    public List<TenantResponse> list() { return svc.list(); }
    @PostMapping("/{tenantId}/admins") @PreAuthorize("hasAuthority('TENANT_ASSIGN_ADMIN')")
    public UserResponse assign(@PathVariable Long tenantId, @Valid @RequestBody AssignTenantAdminRequest r) { return svc.assignAdmin(tenantId, r); }
}
