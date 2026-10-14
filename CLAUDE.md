# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository layout

**DMS** is a web-based Dormitory Management System for Thuy Loi University dormitories (course project "Chuyên đề CNTT", class 65CNTT, group 3, 12 weeks, 5 members). The git repository root is this folder.

- `Docs/`: all project documents, written in Vietnamese. Numbered by project phase.
- `DMS/`: source code. `DMS/backend` is Spring Boot 3.5 on Java 17, built with the Maven wrapper; it contains the root package `vn.edu.tlu.dms` with one package per module, Flyway migrations, and test scaffolding. `DMS/frontend` is React 19 + Vite + TypeScript (npm, oxlint) with the folder layout from the SAD. `DMS/docker-compose.yml` runs PostgreSQL and Keycloak for development. The backend CRUD framework is implemented (see `DMS/backend/README.md` for exact scope and remaining work). Security lives in `account/config/SecurityConfig`; shared DTOs are in `common/dto`. Frontend role routes use a reusable CRUD framework; shared run instructions are in the root `README.md`. Detailed business workflows remain future work.

### Commands

Run from `DMS/backend` (on Windows use `mvnw.cmd`); Docker must be running for the integration tests:

- Start the database: `docker compose up -d` (in `DMS/`)
- Run the backend: `./mvnw spring-boot:run` (profile `dev` by default, port 8080, Flyway migrates on startup)
- Run all tests plus JaCoCo: `./mvnw verify`
- Run a single test: `./mvnw test -Dtest=ModuleDependencyTest` or `-Dtest=Class#method`

Run from `DMS/frontend`: `npm run dev` (port 5173, proxies `/api` to 8080), `npm run lint`, `npm run build`. There is no frontend test runner yet.

`ModuleDependencyTest` (ArchUnit) enforces the SAD 4.2 dependency order and forbids using another module's `repository` or `api` package. `DmsApplicationTests` boots the app against Testcontainers PostgreSQL and checks the table count after migration.

Migrations: `V3__account_security_version.sql` adds account session revocation; `V1__init.sql` is a copy of `Docs/.../DB-DMS-schema.sql` as of the scaffold; `V2__spring_session.sql` creates the Spring Session tables. From now on the migration files are the real schema. Add a new `V<n>__*.sql` and update `DB-Design-DMS.md` in the same change; never edit an applied migration.

### Documents

| Path under `Docs/` | Role |
|---|---|
| `01 Requirements/SRS-DMS.md` | **Authoritative spec.** Functional reqs `FR-<module>-<n>`, business rules `BR-01..11`, NFRs, acceptance scenarios, state tables (section 10) |
| `02 External Design/Software Architecture/SAD-DMS.md` | Approved architecture: modules, dependency rules, transactions and locking, security, errors, uploads, deployment, test strategy |
| `02 External Design/API Specification/API-Spec-DMS.md` | Endpoints per module, request/response schemas, the `errorCode` catalogue (section 5), `allowedActions` per object (section 6), Excel import format |
| `02 External Design/Database Design/` | `DB-Design-DMS.md` (tables, constraints, constraint→`errorCode` mapping, JPA notes), `DB-DMS-schema.sql` (full DDL for PostgreSQL 16), ERD images |
| `02 External Design/Screen Design/DMS-UI-UX-Design.md` | Screens, design tokens, frontend component architecture (section 27) |
| `00 Project Management/he-thong-quan-ly-ky-tuc-xa-mini.md` | Original topic proposal. Older than the SRS and its `BR-` numbering differs (e.g. SRS BR-05 is room transfer, BR-04 is invoices). Cite SRS codes |
| `00 Project Management/Reports/<yyyy-MM-dd>/` | Work reports, one per member per finished task, one folder per day: `<yyyy-MM-dd>_<initials>_<task-slug>.md` (initials without diacritics: NVS, NPN, LDN, NVQ, NTL). Follow the template `Reports/_mau-bao-cao.md` (what was done, files touched, verification, deviations from the design, remaining work). Records of work done, not specs: they never override the documents above and carry no version or change log. When asked for a report, write it here in Vietnamese, name the member in the "Thực hiện" line, and state only what was actually verified |
| `05 Reviews/` | Review comments on the design documents |
| `06 Presentation/DMS-thuyet-trinh.html` | Slides (single HTML file) |
| `03 Internal Design/`, `04 Testing/` | Placeholders for detailed design and test documents |

Precedence on conflict: SRS, then SAD / DB / API, then UI, then the proposal. To change a rule, update the SRS first and then the downstream documents, and bump the version and change log of each document you touch. Write documents in Vietnamese with proper diacritics.

## Architecture (from the SAD)

Fixed constraints: React SPA + Spring Boot 3 (Java 17+) + PostgreSQL 15+; modular monolith, three layers, business logic only in Service; Docker Compose on one server; Keycloak is the requested authentication integration; student data still arrives as Excel. Four roles (Student, Staff, Technician, Admin), each with its own portal.

### Backend

- Root package `vn.edu.tlu.dms` (name is an assumption in the SAD). Modules: `common`, `account`, `room`, `billing`, `contract`, `registration`, `maintenance`, `notification`. Each has `api/` (controller + DTO records), `service/`, `domain/` (JPA entities, enums, events), `repository/`, `job/`.
- **Service interfaces and implementations:** `service/*Service.java` declares methods only, with Vietnamese JavaDoc on every method. All logic and `@Service`/`@Transactional` annotations belong in `service/impl/*ServiceImpl.java`, implementing the corresponding interface. Inject interfaces, not implementation classes. Keep shared abstract implementation bases in `common/service/impl`. Service interfaces must not have default/static method bodies. ArchUnit checks this structure.
- **Dependency order**, calls go downward only: `common → account → room → billing → contract → registration / maintenance → notification`. Call another module's public Service, never its Repository or tables. Entities do not leave their module (pass IDs or DTOs). Cross-module references are stored as plain ID columns, not `@ManyToOne`. Upward notification uses Spring events:
  - *State events* change business state and are handled synchronously in the same transaction (a listener failure rolls back the business operation).
  - *Notification events* use `@TransactionalEventListener(AFTER_COMMIT)` with `@Transactional(REQUIRES_NEW)`, and failures are only logged.
- `room_assignment_history` is owned by `room`. Every occupy / release / move of a bed goes through `BedOccupancyService`. Each stateful entity has an `<Entity>Policy` implementing the SRS section 10 transition table. It is used both to validate actions and to compute `allowedActions`.
- **Transactions and locks:** one write request = one `@Transactional` Service call; READ COMMITTED plus explicit `FOR UPDATE`. Lock order is global: `bed → student_profile → dormitory_contract → invoice`; room transfer locks the two beds by ascending `bed_id`. Lock first, then re-check the condition.
- **Scheduled jobs** (`@Scheduled`, single instance): select candidate IDs, then process each in its own `TransactionTemplate` transaction, lock the row and re-check the condition. Do not rely on `@Transactional(REQUIRES_NEW)` inside the same class as the `@Scheduled` method (proxy self-invocation is skipped). Jobs run as user `SYSTEM` and write `audit_log`.
- **Security:** chỉ Keycloak OIDC/PKCE và Spring Security Resource Server JWT, cấu hình trong account/config/SecurityConfig. Actor lấy từ subject liên kết vào app_user; role/version/locked kiểm tra với dữ liệu DMS mới nhất, scope tiếp tục do ScopeGuard kiểm tra. Không có session/BCrypt fallback. AuthService/Impl gộp khóa tài khoản, reset và thu hồi token; IdentityAccountService/Impl gộp Admin REST và chuyển tài khoản. Xem README.md để chạy.
- **Errors:** Services throw `BusinessException(errorCode, details)`, and a `@RestControllerAdvice` renders `{ errorCode, message, details }`. `DataIntegrityViolationException` is mapped from the DB constraint name to an `errorCode` (e.g. `ux_assignment_open_bed` → `BED_TAKEN`). Unexpected errors return 500 `INTERNAL_ERROR` with a `requestId`. A filter puts `requestId` in the MDC. Every state change goes through `StatusHistoryService.record(...)` in the same transaction.
- **Schema and config:** Flyway owns the schema (`V1__init.sql` = `DB-DMS-schema.sql`; later changes are new migration files, never edits). Business parameters live in the `system_config` table via `ConfigService`; technical settings in `application.yml` plus environment variables (profiles `dev`, `prod`).
- **Files:** incident photos (max 3, ≤ 5 MB each, JPEG/PNG/WebP checked by magic bytes) on volume `/data/uploads`, served only through a permission-checked endpoint; files are deleted if the transaction rolls back. Excel import and report export use Apache POI (.xlsx ≤ 5 MB).
- **Tests:** JUnit 5 + Mockito for Services and Policies (JaCoCo ≥ 60% on the Service layer); Spring Boot Test + Testcontainers PostgreSQL for constraints, error mapping, cross-unit access and concurrency (two requests for one bed, crossing room transfers).

### Frontend

React SPA with Ant Design (themed from the design tokens), React Router, TanStack Query, React Hook Form + Zod, Axios ( Bearer JWT, 401 interceptor). Layout: `app/`, `services/api/`, `features/<module>/`, `pages/<role>/`, `components/`, `constants/statusConfig.ts`, `utils/errorMessages.ts`. Four root routes `/student`, `/staff`, `/technician`, `/admin`; the guard reads the role from `GET /api/auth/me`. After each action invalidate the related queries; on 409 refetch and show the message for the `errorCode`. Mobile-first (from 360 px) for Student and Technician; desktop for Staff and Admin. Vite dev server proxies `/api` to `localhost:8080`.

The UI template (shell) is in place; module work only fills in pages:

- `app/router/{student,staff,technician,admin}Routes.tsx` hold every route of UI section 26, one file per role so members do not collide. Routes render `pages/CrudPage` using resources from `features/crud/resources.ts`, grouped CRUD tabs, or simple navigation home pages. Extend the resource configuration for basic CRUD; create a separate page when implementing a detailed workflow. `app/guards/RequireRole` enforces login, forced password change, then role (wrong role redirects to that role's home, not 403). `app/layouts/` has `StudentLayout` (bottom nav under 640 px), `TechnicianLayout`, and `SidebarShell` shared by `StaffLayout` and `AdminLayout` (sidebar from 1024 px, Drawer below).
- Menus and role home paths live only in `constants/navigation.ts`. Labels, colours and icons for every status live only in `constants/statusConfig.ts` (three layers: DB status badge, derived chip, bed occupancy). Money and date formatting is in `utils/format.ts`.
- Shared components (`components/index.ts`): `StatusBadge`, `DerivedChip`, `ConfirmDialog` / `useConfirm` (optional mandatory reason), `ActionBar` (renders only the buttons present in `allowedActions`), `FormField` (label, required mark, `aria-describedby` error), `StatusTimeline` + `stepsFromFlow`, `ResponsiveTable` (cards under 640 px), `PageHeader`, and presentational business components (`BedCard`, `RoomCard`, `InvoiceCard`, `ContractStatus`, `DepositLedger`, `NotificationItem`). Business components take props only; fetch in `features/<module>/`. Use `useApiErrorHandler` as a mutation `onError`.
- Authentication uses Keycloak and backend /me. Không tạo demo/mock hoặc nhánh legacy. LoginPage và ChangePasswordPage dùng trực tiếp luồng Keycloak; helpers và tín hiệu nhiều tab nằm trong services/api/auth.ts. Hạn chế file mới: ưu tiên file sẵn có, chỉ tách Service/Impl khi có trách nhiệm riêng cần thiết.

### Deployment

Docker Compose with three services: `nginx` (TLS, serves the built React app, proxies `/api` to the backend, so same origin and no CORS in production), `backend` (one instance), `postgres` 16. Volumes `pgdata` and `uploads` need backing up. JVM and containers use `Asia/Ho_Chi_Minh`; DB columns are `TIMESTAMPTZ`. Secrets come from environment variables; `.env` is never committed.

## Rules that span the whole design

These are easy to violate if you only read one file:

- **Scope-based RBAC, not just roles.** Staff belong to a `ManagementUnit` (K1, or N2+N3) and see only that unit's data. Cross-scope reads return 404, not 403. ADMIN has every registered business permission across all units; keep state, history and data constraints. Applications belong to the unit chosen as "đơn vị nguyện vọng".
- **Invariants are enforced in the DB as well as in the Service.** Capacity is guarded by partial unique indexes on open assignments (`ux_assignment_open_bed`, `ux_assignment_open_student`); other examples are `CHECK(end_reading >= start_reading)`, one invoice per (student, semester) or (room, month), one `REFUND` per contract.
- **Never store derived values:** free beds, "full", "expiring", "overdue". Compute them from `bed` and open assignments, using the views in the DB design (e.g. `v_current_stay`, `v_room_occupancy`, `v_deposit_balance`).
- **Never overwrite history.** Room, bed, and applied price live only on `room_assignment_history`. A move closes the old row and opens a new one in one transaction. The contract represents one stay and holds no room or price. Base price lives only on `room_type` and is snapshotted onto the assignment. No hard delete once business data exists.
- **Money model:** room fee per student per semester; electricity/water invoices belong to the *room* (the room leader pays; the system never splits per student); a contract becomes ACTIVE only after the building's deposit is collected; the deposit is a ledger (collect / deduct / top-up / refund) where only damage from *use* (not natural wear) is deducted. Payments are confirmed manually by Staff (no gateway).
- **Atomicity and idempotence:** assignment, activation, transfer, move-out, and invoice issuing are single transactions. Duplicate submits are rejected by the Policy (409) or by a unique index; there is no `Idempotency-Key`.

## API conventions (SRS 6.2, UI 27.2, API-Spec)

- Prefix `/api`. State transitions are `POST /api/<resource>/{id}/<action>` (`/approve`, `/reject`, `/activate`, `/move-out`).
- 400/409 return a stable `errorCode` (e.g. `BED_TAKEN`, `DUPLICATE_INVOICE`, `CONTRACT_AUTO_CANCELLED`) plus `details`. The frontend maps codes to Vietnamese messages and never parses `message`. Add new codes to the API-Spec catalogue and `errorMessages.ts` together.
- Detail responses include `allowedActions`. The UI shows only the buttons in that list and does not duplicate the transition table. The backend re-validates anyway.
- Irreversible transitions need a confirm dialog, and those requiring a reason make the reason mandatory.

## Process notes from the SRS

- Requirements use MoSCoW priority. Items marked ✂ drop from Must to Should if the core is not internally accepted by end of week 7.
- Configurable parameters (not hard-coded): activation window (7 days), "expiring" threshold (15 days), ticket auto-close (3 days), invoice due date (7 days after notice), deposit per building (K1 1,000,000 đ; N2, N3 2,000,000 đ), priority weights, unit prices.
- Student accounts come from an Admin Excel import; login is by student ID with a default password and a forced change on first login.
- Targets: ~1,500 students; 95% of requests under 2 s at 100 concurrent users; load tests with JMeter or k6.
- Out of MVP scope: payment gateway, QR access, visitors, IoT meters, chatbot, roommate matching, auto-assignment, email and realtime notifications.

Current Keycloak integration: IdentityAccountService/Impl and IdentityAccountService/Impl keep Vietnamese contracts. Secret is backend-only, realm template has no demo users. Compose includes separate Keycloak DB. All detailed business permissions remain in ScopeGuard. Do not edit V1–V4. Keycloak external changes may leave issued JWTs valid until five-minute TTL; DMS changes revoke via security_version.

## Role authorization update (14/10/2026)

The user overrides the old "Admin has no business rights" rule: ADMIN may perform every business operation across all units. Keep status/data/history constraints and the actual Admin audit identity. ScopeGuard owns the fixed role-to-action matrix; GET /api/auth/me returns permissions. Service Impl methods enforce action permission and data scope; React gates menu/routes and separate CRUD buttons with those same codes. Reuse existing files, retain Service interface/Impl and Vietnamese comments; do not introduce dynamic role matrices/tables unless asked. Changing a Staff/Technician/Admin account role must revoke old tokens and synchronize Keycloak; login itself remains unchanged.


RequirePrivilege update (14/10/2026): every mapped REST method declares common.security.RequirePrivilege. Multiple codes mean OR; method annotation overrides class fallback. SecurityConfig uses Spring Security Method Security / AuthorizationManagerBeforeMethodInterceptor before Controller execution; missing/empty/ungranted annotations deny. ScopeGuard retains the role matrix, fresh account lookup and internal Service defense. New Controllers and endpoints must be proxyable and declare valid codes; ArchTest enforces annotation coverage. Reuse existing files; RequirePrivilege remains the annotation source, and DmsConstants centralizes fixed codes.

Constants update (15/10/2026): common.constants.DmsConstants owns fixed Role/Action/Resource/Permission codes and domain-specific status groups. AuditAction holds audit event names independently of permission codes, even when current string values match. Use compile-time Permission constants in @RequirePrivilege and the same constants in Service Impl. ScopeGuard retains the unchanged role matrix; introducing a constant never grants a permission implicitly. frontend/src/constants/authorization.ts mirrors the wire codes with const-derived types; invalid permissionFor combinations deny. Environment/Keycloak secrets and system_config parameters stay configurable, not in constants. Do not change persisted values or applied Flyway migrations when refactoring codes.
