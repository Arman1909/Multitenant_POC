package com.example.tenantservice.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates the auth-service JWT locally (no gateway, no call to auth-service): HS256 signature with the shared
 * secret + expiry. Same as auth-service / mt-member-group-ws, so all three accept exactly the same tokens.
 */
@Slf4j
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain chain(HttpSecurity http) throws Exception {
        http.csrf(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> {
                    a.requestMatchers("/error", "/actuator/health/**", "/actuator/info",
                            "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll();
                    AccessRules.RULES.forEach(r ->
                            a.requestMatchers(r.method(), r.pattern()).hasAuthority(r.fe().name()));
                    a.anyRequest().denyAll();
                })
                .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter())));
        return http.build();
    }

    /** FE authorities are derived from the role server-side (not carried in the token). */
    private Converter<Jwt, AbstractAuthenticationToken> converter() {
        return jwt -> {
            List<GrantedAuthority> auths = new ArrayList<>();
            String roleClaim = jwt.getClaimAsString("role");
            try {
                Role role = Role.valueOf(roleClaim);
                auths.add(new SimpleGrantedAuthority("ROLE_" + role));
                role.fe().forEach(f -> auths.add(new SimpleGrantedAuthority(f.name())));
            } catch (IllegalArgumentException | NullPointerException e) {
                // Valid signature but unknown role -> authenticated with no permissions (403 everywhere)
                log.warn("JWT for sub={} has unknown role '{}'", jwt.getSubject(), roleClaim);
            }
            return new JwtAuthenticationToken(jwt, auths, jwt.getSubject());
        };
    }

    @Bean
    SecretKey jwtSecretKey(@Value("${jwt.secret}") String secret) {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey key) {
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
