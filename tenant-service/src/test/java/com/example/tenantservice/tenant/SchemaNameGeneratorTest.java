package com.example.tenantservice.tenant;

import com.example.tenantservice.exception.InvalidTenantNameException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SchemaNameGeneratorTest {

    private final SchemaNameGenerator generator = new SchemaNameGenerator("tenant_", 63);

    @Test
    void generatesExpectedNameForSimpleTenant() {
        assertThat(generator.generate("Laker")).isEqualTo("tenant_laker");
    }

    @Test
    void normalizesMixedCaseAndInternalWord() {
        assertThat(generator.generate("MedImpact")).isEqualTo("tenant_medimpact");
    }

    @Test
    void replacesSpacesAndPunctuationWithUnderscores() {
        assertThat(generator.generate("Bay-Vrio, Inc.")).isEqualTo("tenant_bay_vrio_inc");
    }

    @Test
    void rejectsSqlInjectionAttemptByNormalizingItAway() {
        String malicious = "laker\"; DROP SCHEMA platform; --";
        String schema = generator.generate(malicious);
        // Every character outside [a-z0-9] is stripped/underscored, so no
        // quote, semicolon or SQL keyword survives into the identifier.
        assertThat(schema).matches("^[a-z][a-z0-9_]*$");
        assertThat(schema).doesNotContain("\"", ";", " ");
    }

    @Test
    void truncatesOverlyLongNames() {
        String longName = "a".repeat(100);
        String schema = generator.generate(longName);
        assertThat(schema.length()).isLessThanOrEqualTo(63);
    }

    @Test
    void throwsOnBlankName() {
        assertThatThrownBy(() -> generator.generate("   "))
                .isInstanceOf(InvalidTenantNameException.class);
    }

    @Test
    void throwsOnNameWithNoUsableCharacters() {
        assertThatThrownBy(() -> generator.generate("!!!"))
                .isInstanceOf(InvalidTenantNameException.class);
    }

    @Test
    void validateAcceptsWellFormedIdentifier() {
        generator.validate("tenant_laker"); // no exception
    }

    @Test
    void validateRejectsIdentifierWithIllegalCharacters() {
        assertThatThrownBy(() -> generator.validate("tenant_laker; drop table x"))
                .isInstanceOf(InvalidTenantNameException.class);
    }
}
