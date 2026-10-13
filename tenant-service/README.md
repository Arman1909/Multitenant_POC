# Tenant Web Service

Production-ready tenant provisioning service. Shared PostgreSQL database,
**schema-per-tenant** multi-tenancy, Hibernate runtime routing, Flyway-driven
provisioning.

## Security

Every API call needs an `Authorization: Bearer <jwt>` issued by **auth-service**. The token is validated
locally, the same way auth-service and mt-member-group-ws do it (`security/SecurityConfig.java`): HS256
signature with the shared `JWT_SECRET`, plus expiry.

| API | Who |
|---|---|
| `/api/v1/tenants/**` (create, list, get, update, suspend, reactivate, deactivate) | SUPER_ADMIN only |
| `/api/v1/members/**` (RLS demo) | TENANT_ADMIN, TENANT_USER. The tenant comes from the token's `tenantId`. |

* The audit fields `entered_by` / `updated_by` come from the token's `username` claim.
* The old trusted headers `X-USER-ROLE`, `X-Username` and `X-TENANT-ID` are no longer used.
  `X-DE-ROLE` is still read for the member RLS demo.
* Permissions per URL are in `security/AccessRules.java`, and role → permissions in `security/Role.java`.

Default port is **8081** (auth-service uses 8080, mt-member-group-ws 8082).

## Stack

Java 17 · Spring Boot 3.3 · Spring Data JPA / Hibernate 6 · PostgreSQL ·
Flyway · Maven

## Run locally

```bash
# 1. Start Postgres
docker run --name tenant-pg -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=platform_db -p 5432:5432 -d postgres:16-alpine

# 2. Build & run (platform schema/migrations run automatically on boot)
mvn spring-boot:run
```

Config is externalized via env vars: `DB_HOST`, `DB_PORT`, `DB_NAME`,
`DB_USERNAME`, `DB_PASSWORD`, `DB_POOL_MAX`, `DB_POOL_MIN_IDLE` (see
`application.yml`).

## Run tests

```bash
mvn test
```

Unit tests need nothing running. The integration test
(`TenantProvisioningIntegrationTest`) spins up a real Postgres via
Testcontainers (requires Docker) and exercises the full HTTP → provisioning →
schema-routing flow end to end.

## Sample API requests

First get a SUPER_ADMIN token from auth-service:

```bash
T=$(curl -s -XPOST localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' \
      -d '{"username":"superadmin","password":"password"}' | jq -r .accessToken)
```

### Create a tenant

```bash
curl -i -X POST http://localhost:8081/api/v1/tenants \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $T" \
  -d '{
        "tenantName": "Laker",
        "tenantDesc": "Laker health plan",
        "startDate": "2026-01-01",
        "tenantAddress1": "123 Main St",
        "tenantCity": "Los Angeles",
        "tenantState": "CA",
        "tenantPostalCode": "90001"
      }'
```

```json
{
  "tenantId": 1,
  "tenantName": "Laker",
  "schemaName": "tenant_laker",
  "status": "ACTIVE",
  "enteredBy": "superadmin",
  "...": "..."
}
```

### List tenants

```bash
curl http://localhost:8081/api/v1/tenants -H "Authorization: Bearer $T"
```

### Get one tenant

```bash
curl http://localhost:8081/api/v1/tenants/1 -H "Authorization: Bearer $T"
```

### Update tenant metadata (PATCH - partial)

```bash
curl -i -X PATCH http://localhost:8081/api/v1/tenants/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $T" \
  -d '{"tenantDesc": "Updated description", "tenantCity": "Long Beach"}'
```

### Suspend / Reactivate

```bash
curl -i -X POST http://localhost:8081/api/v1/tenants/1/suspend    -H "Authorization: Bearer $T"
curl -i -X POST http://localhost:8081/api/v1/tenants/1/reactivate -H "Authorization: Bearer $T"
```

### Deactivate (logical delete - schema is NOT dropped)

```bash
curl -i -X DELETE http://localhost:8081/api/v1/tenants/1 -H "Authorization: Bearer $T"
```

## SQL verification queries

```sql
-- All tenants and their status
SELECT tenant_id, tenant_name, schema_name, status, entered, entered_by
FROM platform.tenants
ORDER BY tenant_id;

-- Confirm a tenant's physical schema exists
SELECT schema_name
FROM information_schema.schemata
WHERE schema_name = 'tenant_laker';

-- Confirm the member table was created by Flyway inside that schema
SELECT table_schema, table_name
FROM information_schema.tables
WHERE table_schema = 'tenant_laker' AND table_name = 'member';

-- Each tenant schema has its own independent Flyway history
SELECT * FROM tenant_laker.flyway_schema_history;
SELECT * FROM platform.flyway_schema_history;

-- Prove data isolation: rows in one tenant never appear in another
SELECT * FROM tenant_laker.member;
SELECT * FROM tenant_medimpact.member;
```

## What's deliberately NOT here

Login/token issuing (that's auth-service; this service only validates its JWTs), an API gateway,
a user service, a frontend, or any other microservice. This repository is the Tenant Web Service only.

psql -U postgres -h localhost -p 5432 -d platform_db -f scripts/demo_seed_tenant_laker.sql


& "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -h localhost -p 5432 -d platform_db -f scripts/demo_seed_tenant_medimpact.sql