package com.example.pbm.security;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

import java.nio.charset.StandardCharsets;
import java.util.*;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import com.example.pbm.entity.Role;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

@Configuration @EnableMethodSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain chain(HttpSecurity http) throws Exception {
        http.csrf(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> {
                    a.requestMatchers("/api/v1/auth/login", "/error",
                            "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll();
                    AccessRules.RULES.forEach(r ->
                            a.requestMatchers(r.method(), r.pattern()).hasAuthority(r.fe().name()));
                    a.requestMatchers("/api/v1/auth/me").authenticated();
                    a.requestMatchers("/api/v1/auth/switch-tenant").hasRole("TENANT_ADMIN");
                    a.anyRequest().denyAll();
                })
                .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter())));
        return http.build();
    }
    /** FE authorities are derived from the role server-side (not carried in the token). */
    private Converter<Jwt, AbstractAuthenticationToken> converter() {
        return jwt -> {
            Role role = Role.valueOf(jwt.getClaimAsString("role"));
            List<GrantedAuthority> auths = new ArrayList<>();
            auths.add(new SimpleGrantedAuthority("ROLE_" + role));
            role.fe().forEach(f -> auths.add(new SimpleGrantedAuthority(f.name())));
            return new JwtAuthenticationToken(jwt, auths, jwt.getSubject());
        };
    }
    @Bean
    SecretKey jwtSecretKey(@Value("${jwt.secret}") String s) {
        return new SecretKeySpec(s.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
    @Bean JwtDecoder jwtDecoder(SecretKey k) {
        return NimbusJwtDecoder.withSecretKey(k).macAlgorithm(MacAlgorithm.HS256).build();
    }
    @Bean JwtEncoder jwtEncoder(SecretKey k) { return new NimbusJwtEncoder(new ImmutableSecret<>(k)); }
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}
