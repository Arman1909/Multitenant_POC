package com.example.pbm.controller;

import com.example.pbm.dto.request.*;
import com.example.pbm.dto.response.*;
import com.example.pbm.service.AuthService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Tag(name = "1. Auth")
@RestController @RequestMapping("/api/v1/auth") @RequiredArgsConstructor
public class AuthController {
    private final AuthService svc;

    @RequestBody(content = @Content(mediaType = "application/json", examples = {
            @ExampleObject(name = "1. SUPER_ADMIN", value = "{\"username\":\"superadmin\",\"password\":\"password\"}"),
            @ExampleObject(name = "2. TENANT_ADMIN (tenantId = tenant-service tenant_id)",
                    value = "{\"username\":\"tenantadmin1\",\"password\":\"password\",\"tenantId\":1}"),
            @ExampleObject(name = "3. TENANT_USER", value = "{\"username\":\"usera1\",\"password\":\"password\"}")
    }))
    @PostMapping("/login")
    public LoginResponse login(@Valid @org.springframework.web.bind.annotation.RequestBody LoginRequest r) {
        return svc.login(r);
    }

    @PostMapping("/switch-tenant")
    public LoginResponse switchTenant(@Valid @org.springframework.web.bind.annotation.RequestBody SwitchTenantRequest r) {
        return svc.switchTenant(r);
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) { return svc.me(jwt); }
}