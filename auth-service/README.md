# PBM Auth POC
Needs Java 21, Maven and PostgreSQL. Tests use H2, so no database is needed for `mvn test`.

## How it fits
* One shared database for the POC: **`platform_db`**.
  * `platform.tenants`: owned by **tenant-service**. auth-service only reads it; tenants are created there.
  * `auth.*` (users, user_tenants, demo members/claims): owned by auth-service (Hibernate `ddl-auto: update`).
* JWT: **HS256** with the shared secret `JWT_SECRET` (UTF-8 bytes). Claims: `sub`, `username`, `role`, `deRole`,
  `tenantId` = tenant-service's `platform.tenants.tenant_id` (a number, sent as a string). Expires after 30 min.
  Other services (e.g. mt-member-group-ws) validate it locally with the same secret.

## Run
```
# tenant-service first (creates platform.tenants), then:
mvn spring-boot:run          # port 8080, db platform_db (DB_URL / DB_USER / DB_PASSWORD)
mvn test                     # H2 + a test copy of platform.tenants (src/test/resources/schema.sql)
```
On startup it seeds `superadmin`. If tenant-service already has 2 ACTIVE tenants, it also seeds
`tenantadmin1` (both tenants), `usera1` (1st) and `userb1` (2nd). All passwords are `password`.
If the tenants don't exist yet, create them in tenant-service and restart auth-service.

```
T=$(curl -s -XPOST localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' -d '{"username":"superadmin","password":"password"}' | jq -r .accessToken)
curl -s localhost:8080/api/v1/tenants -H "Authorization: Bearer $T"          # tenant ids (from tenant-service)
curl -s -XPOST localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' -d '{"username":"tenantadmin1","password":"password","tenantId":1}'
curl -s -XPOST localhost:8080/api/v1/auth/switch-tenant -H "Authorization: Bearer <jwt>" -H 'Content-Type: application/json' -d '{"tenantId":2}'
```

## Permissions
| Capability | SUPER_ADMIN | TENANT_ADMIN | TENANT_USER |
|---|:-:|:-:|:-:|
| View tenant list (`GET /tenants`) | Yes | – | – |
| Assign tenant admins (`POST /tenants/{id}/admins`) | Yes | – | – |
| Switch active tenant (among assigned) | – | Yes | – |
| Users: create / update (password) / deactivate / reactivate | – | Yes | – |
| Users: read | – | Yes | Yes |
| Members, claims: read (demo data) | – | Yes | Yes |

User management rules:
* A TENANT_ADMIN can't deactivate themselves, and a tenant always keeps at least one active TENANT_ADMIN.
  Admin rows are locked during the check, so two admins can't deactivate each other at the same time.
* Deactivating a TENANT_ADMIN who is assigned to other tenants only removes this tenant from them; they stay
  active elsewhere. Anyone else is set INACTIVE and can't log in.
* A TENANT_ADMIN can reset TENANT_USER passwords and their own, not other admins'.
* A caller who was deactivated or unassigned can't manage users with a token they still hold.
  Other tokens stay valid until they expire (30 min max). That's the trade-off of stateless JWTs.
* Login and switch-tenant only work for tenants that are ACTIVE in tenant-service.
