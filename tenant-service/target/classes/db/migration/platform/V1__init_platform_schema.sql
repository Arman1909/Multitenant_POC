-- Platform schema: registry of all tenants.
-- This is the ONLY schema Flyway manages automatically at application startup.

CREATE SCHEMA IF NOT EXISTS platform;

CREATE TABLE platform.tenants (
    tenant_id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    tenant_name         VARCHAR(120)    NOT NULL,
    tenant_desc         VARCHAR(500),
    schema_name         VARCHAR(63)     NOT NULL,

    start_date          DATE,
    end_date            DATE,

    tenant_address1     VARCHAR(200),
    tenant_address2     VARCHAR(200),
    tenant_city         VARCHAR(100),
    tenant_state        VARCHAR(100),
    tenant_postal_code  VARCHAR(20),

    status              VARCHAR(30)     NOT NULL
                            CONSTRAINT chk_tenant_status
                            CHECK (status IN ('PROVISIONING', 'ACTIVE', 'PROVISIONING_FAILED', 'SUSPENDED', 'DEACTIVATED')),

    -- Audit fields
    entered             TIMESTAMPTZ     NOT NULL DEFAULT now(),
    entered_by          VARCHAR(100)    NOT NULL,
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_by          VARCHAR(100),
    version             BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT uq_tenant_name   UNIQUE (tenant_name),
    CONSTRAINT uq_schema_name   UNIQUE (schema_name),
    CONSTRAINT chk_dates        CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date)
);

CREATE INDEX idx_tenants_status ON platform.tenants (status);
CREATE INDEX idx_tenants_schema_name ON platform.tenants (schema_name);

COMMENT ON TABLE platform.tenants IS 'Registry of all provisioned tenants and their lifecycle status.';
COMMENT ON COLUMN platform.tenants.schema_name IS 'Generated once at creation from tenant_name; immutable thereafter even if tenant_name is edited.';
