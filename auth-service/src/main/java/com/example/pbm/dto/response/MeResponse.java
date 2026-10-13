package com.example.pbm.dto.response;

import com.example.pbm.entity.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MeResponse(boolean valid, UUID userId, String username, Role role, DeRole deRole,
                         Instant expiresAt, List<Fe> permissions, TenantResponse activeTenant,
                         List<TenantResponse> tenants, String scope) {}