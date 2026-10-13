# mt-member-group-ws

POC: multi-tenant **Member & Group** web service. Schema-per-tenant on PostgreSQL, sharing the database and
tenant schemas created by **tenant-service**, and validating **auth-service** JWTs itself (no gateway).

## How it fits with the other services

```
                    login                         Bearer JWT (HS256)
  client ───────────────────────► auth-service ─────────────┐
     │                            :8080, db "pbm"           │ same JWT_SECRET
     │   Bearer JWT                                         ▼
     └───────────────────────────────────────────► mt-member-group-ws :8082
                                                        │ validates signature + exp locally
                                                        │ tenantId claim ─► platform.tenants.schema_name
                                                        ▼
  tenant-service :8081 ──creates──► db "platform_db": platform.tenants
                                                    tenant_x.member            (tenant-service Flyway)
                                                    tenant_x.member_group      (this service)
                                                    tenant_x.group_membership  (this service)
```

### JWT validation (same pattern as auth-service)

* `SecurityConfig`: Spring Security OAuth2 resource server, `NimbusJwtDecoder.withSecretKey(...)` + `HS256`.
  The key is `JWT_SECRET` as UTF-8 bytes, exactly like auth-service. The signature and `exp` are checked on every request.
* Token claims used: `sub`, `username`, `role`, `deRole`, `tenantId`.
* `Role` → `Fe` (permissions) mapping is kept **server-side** in this service; the token only carries the role.
* `AccessRules` is the single URL → permission table; any other URL is denied.
* `TenantContextFilter` runs after validation. The tenant comes **only** from the signed `tenantId` claim, never
  from a header, path or body. It is resolved to a schema in `platform.tenants`, which must exist and be `ACTIVE`.

#### Agreed permission matrix (whole platform)

| Capability | SUPER_ADMIN | TENANT_ADMIN | TENANT_USER | Owner |
|---|:-:|:-:|:-:|---|
| Tenant create / manage / deactivate | Yes | – | – | tenant-service |
| Assign tenant admins to tenants | Yes | – | – | auth-service |
| View tenant list | Yes | – | – | tenant-service |
| Switch active tenant (among assigned) | – | Yes | – | auth-service |
| Tenant users: create / update / deactivate | – | Yes | – | auth-service |
| Tenant users: read | – | Yes | Yes (read only) | auth-service |
| Members: create / read / update | – | Yes | Yes | **this service** |
| Groups: create / read / update | – | Yes | Yes | **this service** |
| Add / remove members in groups | – | Yes | Yes | **this service** |
| Claims: read | – | Yes | Yes | claims |
| Deactivate a member | – | Yes | – | **this service** |
| Deactivate a group (ends every membership in it) | – | Yes | – | **this service** |
| Reactivate records | – | Yes | – | **this service** |
| View audit / history | – | Yes | – | **this service** (membership history) |
| Bulk import (later) | – | Yes | – | – |

Rules for the whole table:
* SUPER_ADMIN never gets tenant business data (users, members, groups, claims).
* TENANT_ADMIN and TENANT_USER always work in their active tenant only, taken from the token.
* A TENANT_ADMIN can't deactivate themselves or the last admin of a tenant (enforced in auth-service).

#### How this service implements its part (`Role.java`)

| Permission | TENANT_ADMIN | TENANT_USER |
|---|:-:|:-:|
| MEMBER_READ / CREATE / UPDATE | yes | yes |
| GROUP_READ / CREATE / UPDATE | yes | yes |
| GROUP_MEMBER_MANAGE | yes | yes |
| MEMBER_DEACTIVATE / MEMBER_REACTIVATE | yes | - |
| GROUP_DEACTIVATE / GROUP_REACTIVATE | yes | - |
| AUDIT_READ (`includeHistory=true`) | yes | - |

SUPER_ADMIN has no permissions here. Status can't be set through create/update; it only changes via
deactivate/reactivate, so a TENANT_USER can't deactivate a record with a normal update.
`RolePermissionMatrixTest` locks this matrix in.

## APIs (`/api/v1`, all need `Authorization: Bearer <jwt>`)

| Method | Path | Permission |
|---|---|---|
| POST | `/members` | MEMBER_CREATE |
| GET | `/members?q=&status=&page=&size=&sort=` | MEMBER_READ |
| GET | `/members/{memberId}` | MEMBER_READ |
| PUT | `/members/{memberId}` (send `version` for optimistic locking) | MEMBER_UPDATE |
| DELETE | `/members/{memberId}` (deactivate: soft delete, ends memberships) | MEMBER_DEACTIVATE |
| POST | `/members/{memberId}/reactivate` | MEMBER_REACTIVATE |
| GET | `/members/{memberId}/groups?includeHistory=` | MEMBER_READ (+ AUDIT_READ for history) |
| POST | `/groups` | GROUP_CREATE |
| GET | `/groups?q=&status=&page=&size=&sort=` | GROUP_READ |
| GET | `/groups/{groupId}` | GROUP_READ |
| PUT | `/groups/{groupId}` | GROUP_UPDATE |
| DELETE | `/groups/{groupId}` (deactivate: soft delete, ends every membership) | GROUP_DEACTIVATE |
| POST | `/groups/{groupId}/reactivate` | GROUP_REACTIVATE |
| GET | `/groups/{groupId}/members?includeHistory=` | GROUP_READ (+ AUDIT_READ for history) |
| POST | `/groups/{groupId}/members` `{ "memberId": 1, "startDate": "2026-10-07" }` | GROUP_MEMBER_MANAGE |
| DELETE | `/groups/{groupId}/members/{memberId}` | GROUP_MEMBER_MANAGE |
| GET | `/context` (shows what was resolved from your token) | any valid token |

Swagger UI: http://localhost:8082/swagger-ui.html (use **Authorize** and paste the token).

## Running the POC locally

Nothing has to be deployed. Each service is a separate Spring Boot app on its own port, and all of them share
**one local PostgreSQL database, `platform_db`**.

| Service | Port | Owns in `platform_db` |
|---|---|---|
| tenant-service | **8081** (it defaults to 8080 too, so override it) | `platform.tenants`, `tenant_*` schemas, `tenant_*.member` |
| auth-service | 8080 | `auth.*` (users, user_tenants); reads `platform.tenants` |
| mt-member-group-ws | 8082 | `tenant_*.member_group`, `tenant_*.group_membership` |

1. Start PostgreSQL and create the database once:
   ```
   & "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -h localhost -c "CREATE DATABASE platform_db;"
   ```
2. tenant-service: `cd H:\tenant-service; $env:SERVER_PORT=8081; mvn spring-boot:run`, then create **two** tenants
   (`POST http://localhost:8081/api/v1/tenants` with `X-USER-ROLE: SUPER_ADMIN`). Each one gets `tenant_<name>` with a member table.
3. auth-service: `cd H:\auth-service; mvn spring-boot:run`. It seeds `superadmin`, plus `tenantadmin1`, `usera1`
   and `userb1` for the first two ACTIVE tenants (password `password`).
4. this service: `cd H:\mt-member-group-ws; mvn spring-boot:run`

DB credentials default to `postgres`/`postgres` everywhere (`DB_USERNAME`/`DB_PASSWORD`; auth-service uses `DB_USER`).
`JWT_SECRET` must be the same for auth-service and this service. Both use the same default.

### Getting a token

* **From auth-service** (end-to-end):
  `POST http://localhost:8080/api/v1/auth/login` with `{"username":"tenantadmin1","password":"password","tenantId":1}`
  → `accessToken`, whose `tenantId` is tenant-service's `tenant_id`.
* **Without auth-service running**: `.\scripts\new-dev-token.ps1 -Role TENANT_ADMIN -TenantId 1` prints a token with
  the same claims and secret (`-Role TENANT_USER`, `-Role SUPER_ADMIN` also work).

## Known gaps

1. **tenant-service has no JWT validation** (it trusts `X-USER-ROLE`). It can reuse this service's `security`
   package so only `SUPER_ADMIN` tokens can manage tenants.
2. tenant-service's member RLS (`set_user_context('DE_LAKER_MBR')`) expects DE roles like `DE_<TENANT>_<PREFIX>`,
   while auth-service's `deRole` is `PLATFORM`/`OWN_TENANT`. This service connects as the table owner, so RLS does not
   filter it.
3. JWTs are stateless: a user deactivated in auth-service keeps access here until their token expires (max 30 min).
   Fix later with shorter tokens + refresh, or a deny-list check.
4. Audit/history currently means membership history. A full field-level change log (e.g. Hibernate Envers) and bulk import are not built yet.
