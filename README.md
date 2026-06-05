# CBD Order Tracker — Backend

Spring Boot 3.3.3 REST API (Java 17) for a custom order management and attendance system.
See `CLAUDE.md` for codebase conventions, build/run instructions, role/privilege model,
and the order pipeline.

---

## Attendance with geofencing

Employee check-in / check-out with strict server-side geofence enforcement.

### Endpoints

| Method | Path                       | Auth privilege                                             |
|--------|----------------------------|------------------------------------------------------------|
| POST   | `/api/attendance/check-in` | `attendance-check-in`                                      |
| POST   | `/api/attendance/check-out`| `attendance-check-in`                                      |
| GET    | `/api/attendance/me/current` | `attendance-check-in`                                    |
| GET    | `/api/attendance`          | `attendance-view-all`                                      |
| POST   | `/api/attendance`          | `attendance-edit` (manual admin entry, bypasses geofence)  |
| PATCH  | `/api/attendance/{id}`     | `attendance-edit`                                          |
| GET    | `/api/locations`           | `location-manage` OR `attendance-check-in` OR `attendance-view-all` |
| POST   | `/api/locations`           | `location-manage`                                          |
| PATCH  | `/api/locations/{id}`      | `location-manage`                                          |
| DELETE | `/api/locations/{id}`      | `location-manage` (soft delete if referenced)              |

All endpoints are tenant-scoped via `TenantContext.requireTenantId()`. Superadmins use
`X-Tenant-Id` for impersonation — see `JwtAuthenticationFilter`.

### Geofence formula

For each active location in the tenant, compute Haversine `distance_m` from the submitted
`(lat, lng)` to the location's center. The location qualifies only when:

```
distance_m <= radius_m
```

The geofence tolerance lives entirely in `WorkLocation.radiusM` — to allow check-ins within
~500 m of a location, set that location's `radiusM` to 500. If multiple locations qualify,
the one with the smallest `distance_m` is chosen and persisted on the session.

The client-reported `accuracy_m` is **recorded** on the session (`check_in_accuracy_m` /
`check_out_accuracy_m`) for audit, but it does **not** affect the pass/fail decision — the
tolerance is server-side config, not a client-sent value (which would be spoofable). Note
that a poor GPS fix means the submitted `(lat, lng)` itself can be off by hundreds of meters
or more, so the geofence is only as meaningful as the coordinate the device reports.

The same formula is applied on check-out.

### Check order (check-in and check-out)

1. Body validation (400 with the existing validation envelope)
2. Existing-session conflict → 422 `already_checked_in` (check-in) / `not_checked_in` (check-out)
3. Geofence match → 422 `out_of_geofence` or `no_active_locations`
4. Insert/update session (a `DataIntegrityViolationException` from the
   `uq_attendance_open_session` unique index is caught and re-raised as `already_checked_in`)

### Error reason codes (stable contract — do not rename without an FE migration)

| `reason`              | HTTP | Meaning                                                                      |
|-----------------------|------|------------------------------------------------------------------------------|
| `out_of_geofence`     | 422  | No active location satisfies `distance_m <= radius_m`                        |
| `already_checked_in`  | 422  | Open session exists                                                          |
| `not_checked_in`      | 422  | No open session for check-out                                                |
| `no_active_locations` | 422  | Tenant has zero active locations configured                                  |

Error envelope:

```json
{ "message": "Human-readable message", "reason": "out_of_geofence" }
```

Validation errors (missing fields / out-of-range numbers) use the existing 400
`ProblemDetail` envelope produced by `GlobalExceptionHandler#handleValidation`.

### Auto-close behavior

A scheduled job (`AttendanceAutoCloseJob`) runs daily at `03:00 UTC`. Any open session
older than **16 hours** is closed: `check_out_at` is set to the job's run time,
`auto_closed = true`, and the `check_out_*` location fields (lat/lng/accuracy/ip/user_agent)
are left `NULL`. Admins can edit these afterwards through `PATCH /api/attendance/{id}`.

### Data model notes

`attendance_sessions` includes a generated column:

```sql
open_user_id INT AS (CASE WHEN check_out_at IS NULL THEN user_id END) STORED
```

backed by `UNIQUE KEY uq_attendance_open_session (tenant_id, open_user_id)` — MySQL's
substitute for Postgres partial unique indexes. The generated column and index are added by
`AttendanceSchemaInitializer` at boot (idempotent) because Hibernate `ddl-auto=update`
cannot model the expression cleanly. Requires MySQL 8.0.13+.

### Privileges

Seeded in `data.sql` and back-filled idempotently for existing deployments by
`AttendancePrivilegeMigration`:

| Privilege              | Default roles                                |
|------------------------|----------------------------------------------|
| `attendance-check-in`  | `company_admin`, `manager`, `manufacturer`   |
| `attendance-view-all`  | `company_admin`, `manager`                   |
| `attendance-edit`      | `company_admin`                              |
| `location-manage`      | `company_admin`                              |

### Tests

Unit tests under `src/test/java/cbd/order_tracker/service/impl/`:

- `AttendanceServiceImplTest` — worked-example table (6 cases), double check-in,
  check-out without open session, no active locations, multiple qualifying locations,
  concurrent-race duplicate-key handling, IP / user-agent persistence, auto-close,
  cross-tenant repository filtering.
- `HaversineTest` — distance correctness.
- `WorkLocationServiceImplTest` — soft/hard delete behavior.

Run: `./mvnw test -Dtest='AttendanceServiceImplTest,HaversineTest,WorkLocationServiceImplTest'`.
