package com.example.tenantservice.tenant;

import com.example.tenantservice.exception.InvalidTenantNameException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Turns an arbitrary, user-supplied tenant_name into a safe PostgreSQL schema
 * identifier. Output is ALWAYS validated against a strict allow-list pattern
 * before use, so this name is safe to place inside DDL that cannot use bind
 * parameters (CREATE SCHEMA "<name>") - it is never built by directly
 * concatenating the raw, unvalidated tenant_name.
 */
@Component
public class SchemaNameGenerator {

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[a-z][a-z0-9_]*$");
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");

    private final String prefix;
    private final int maxLength;

    public SchemaNameGenerator(@Value("${tenant.provisioning.schema-prefix:tenant_}") String prefix,
                                @Value("${tenant.provisioning.max-schema-name-length:63}") int maxLength) {
        this.prefix = prefix;
        this.maxLength = maxLength;
    }

    public String generate(String tenantName) {
        if (tenantName == null || tenantName.isBlank()) {
            throw new InvalidTenantNameException("tenantName must not be blank");
        }

        String normalized = Normalizer.normalize(tenantName, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")          // strip accents
                .toLowerCase();

        String slug = NON_ALNUM.matcher(normalized).replaceAll("_")
                .replaceAll("^_+|_+$", "");         // trim leading/trailing underscores

        if (slug.isBlank()) {
            throw new InvalidTenantNameException(
                    "tenantName '" + tenantName + "' does not contain any usable characters");
        }

        String candidate = prefix + slug;

        if (candidate.length() > maxLength) {
            candidate = candidate.substring(0, maxLength).replaceAll("_+$", "");
        }

        validate(candidate);
        return candidate;
    }

    /** Defence in depth: re-validated immediately before any DDL use. */
    public void validate(String schemaName) {
        if (schemaName == null || !SAFE_IDENTIFIER.matcher(schemaName).matches()
                || schemaName.length() > maxLength) {
            throw new InvalidTenantNameException(
                    "Generated schema name '" + schemaName + "' is not a safe PostgreSQL identifier");
        }
    }
}
