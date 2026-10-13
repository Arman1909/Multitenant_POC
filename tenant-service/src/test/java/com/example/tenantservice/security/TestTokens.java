package com.example.tenantservice.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** Issues tokens exactly like auth-service's JwtService (HS256, same default secret, same claims). */
public final class TestTokens {

    public static final String SECRET = "HRdiJ624Je8Zq2o4/aLvFzAB2rqa8gzdoFb0kLZeo3GOt59akrvUtv8YGVr/kwDK";

    private static final NimbusJwtEncoder ENCODER = new NimbusJwtEncoder(new ImmutableSecret<>(
            new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256")));

    private TestTokens() {
    }

    public static String superAdmin() {
        return token("superadmin", "SUPER_ADMIN", "PLATFORM", null, Duration.ofMinutes(30));
    }

    public static String tenantAdmin(long tenantId) {
        return token("tenantadmin1", "TENANT_ADMIN", "OWN_TENANT", tenantId, Duration.ofMinutes(30));
    }

    public static String tenantUser(long tenantId) {
        return token("usera1", "TENANT_USER", "OWN_TENANT", tenantId, Duration.ofMinutes(30));
    }

    public static String token(String username, String role, String deRole, Long tenantId, Duration validFor) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .subject(UUID.randomUUID().toString())
                .issuedAt(now.minusSeconds(120))
                .expiresAt(now.plus(validFor))
                .claim("username", username).claim("role", role).claim("deRole", deRole);
        if (tenantId != null) {
            claims.claim("tenantId", tenantId.toString());
        }
        return ENCODER.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims.build()))
                .getTokenValue();
    }

    public static String bearer(String token) {
        return "Bearer " + token;
    }
}
