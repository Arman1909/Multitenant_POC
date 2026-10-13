-- Applied by TenantSchemaProvisioner against a single tenant's schema.
-- ${flyway:defaultSchema} is not used deliberately: the schema is selected by
-- Flyway's `schemas(...)` configuration at invocation time, so this script
-- contains no schema-qualified references and is reusable for every tenant.

CREATE TABLE member (
    member_id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    first_name        VARCHAR(100)    NOT NULL,
    last_name         VARCHAR(100)    NOT NULL,
    email             VARCHAR(255)    NOT NULL,
    status            VARCHAR(30)     NOT NULL DEFAULT 'ACTIVE',

    member_number     VARCHAR(30),
    date_of_birth     DATE,
    insurance_number  VARCHAR(40),
    carrier_hq_code   VARCHAR(20),

    created_at        TIMESTAMPTZ     NOT NULL DEFAULT now(),
    created_by        VARCHAR(100),
    updated_at        TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_by        VARCHAR(100),
    version           BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT uq_member_email UNIQUE (email)
);

CREATE UNIQUE INDEX uq_member_member_number ON member (member_number) WHERE member_number IS NOT NULL;
CREATE INDEX idx_member_status ON member (status);
CREATE INDEX idx_member_last_name ON member (last_name);
CREATE INDEX idx_member_carrier_hq_code ON member (carrier_hq_code);