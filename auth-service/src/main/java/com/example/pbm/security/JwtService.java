package com.example.pbm.security;

import com.example.pbm.entity.User;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor
public class JwtService {
    private final JwtEncoder encoder;

    /** tenantId claim = ACTIVE tenant as tenant-service's platform.tenants.tenant_id (null for SUPER_ADMIN). */
    public String issue(User u, Long activeTenant) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder b = JwtClaimsSet.builder().subject(u.getId().toString())
                .issuedAt(now).expiresAt(now.plus(30, ChronoUnit.MINUTES))
                .claim("username", u.getUsername()).claim("role", u.getRole().name()).claim("deRole", u.getDeRole().name());
        if (activeTenant != null) b.claim("tenantId", activeTenant.toString());
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), b.build())).getTokenValue();
    }
}
