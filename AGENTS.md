# CourseChecker — Agent Memory

> This file documents how an AI agent should interact with this repository.
> Read this before making any changes.

---

## Architecture Overview

```
┌────────────────┐     HTTP/REST     ┌────────────────────┐     JDBC     ┌──────────────┐
│  Vite + React  │ ◄──────────────► │  Spring Boot 4 API  │ ◄──────────► │  PostgreSQL  │
│  (TypeScript)  │    localhost:5173 │  (Java 17 + JPA)    │  :5433      │  (Alpine)    │
│   client/      │                  │   server/            │              │  Docker      │
└────────────────┘                  └────────────────────┘              └──────────────┘
```

| Layer     | Directory  | Tech Stack                          |
|-----------|-----------|--------------------------------------|
| Frontend  | `client/` | Vite 7, React 19, TypeScript         |
| Backend   | `server/` | Spring Boot 4.0.3, JPA, Flyway       |
| Database  | Docker    | PostgreSQL Alpine via docker-compose |

---

## Composite Pattern — Prerequisite Tree (CRITICAL)

The core domain model uses the **Composite Design Pattern** to represent nested AND/OR prerequisite logic.

### Entity Hierarchy

```
                 Requirement (abstract, SINGLE_TABLE inheritance)
                 discriminator: "requirement_type"
                       │
         ┌─────────────┼─────────────┐
         │             │             │
    CourseRequirement AndRequirement OrRequirement
    (Leaf: COURSE)   (Composite: AND) (Composite: OR)
    └─ requiredCode  └─ children[]   └─ children[]
```

### Key Rules

1. **All requirement types live in ONE table** (`requirement`) with a `requirement_type` discriminator column.
2. **`parent_id`** is a self-referencing FK used by `@OneToMany` in And/OrRequirement to form trees.
3. **`Course.prerequisite`** is a `@OneToOne` FK pointing to the ROOT node of the tree (can be null).
4. **Cascading**: `CascadeType.ALL` + `orphanRemoval = true` — deleting a course deletes its entire prerequisite tree.

### Example: COMPSCI 220

```
COMPSCI 220 requires: COMPSCI 187 AND (MATH 131 OR MATH 132)

Course(COMPSCI 220)
  └─ prerequisite → AndRequirement
                        ├─ CourseRequirement("COMPSCI 187")
                        └─ OrRequirement
                              ├─ CourseRequirement("MATH 131")
                              └─ CourseRequirement("MATH 132")
```

---

## Database & Migrations

### Running the Database

```bash
# Start PostgreSQL (from project root)
docker compose up -d

# Stop
docker compose down

# Reset (destroy data)
docker compose down -v && rm -rf docker/postgres-data
```

### Connection Details

| Property | Value              |
|----------|--------------------|
| Host     | localhost          |
| Port     | 5433               |
| Database | coursechecker_db   |
| Username | coursechecker      |
| Password | coursechecker      |

### Schema Migrations

- **Flyway** manages all schema changes.
- Migration files live in: `server/src/main/resources/db/migration/`
- Naming convention: `V{N}__description.sql` (double underscore!)
- Hibernate `ddl-auto` is set to `validate` — it NEVER modifies the schema.
- **To add a new table/column**: Create a new `V{N+1}__description.sql` file. Never edit existing migrations.

---

## Auth Flow (JWT, Server)

The auth system was introduced in commit `8e1324e` (`feat(server): add jwt auth and scoped crud`).  
Latest commit (`94809ca`) is test-only and does not change runtime auth behavior.

### Key Files

- `server/src/main/java/com/example/server/security/SecurityConfig.java`
- `server/src/main/java/com/example/server/security/JwtAuthenticationFilter.java`
- `server/src/main/java/com/example/server/security/JwtService.java`
- `server/src/main/java/com/example/server/controller/AuthController.java`
- `server/src/main/java/com/example/server/service/AuthService.java`
- `server/src/main/java/com/example/server/security/AuthenticatedUser.java`
- `server/src/main/java/com/example/server/security/RestAuthenticationEntryPoint.java`
- `server/src/main/java/com/example/server/security/RestAccessDeniedHandler.java`
- `server/src/main/java/com/example/server/security/ApplicationSecretsValidator.java`

### Request Lifecycle

1. `SecurityConfig` enables stateless security and permits only `POST /api/v1/auth/register` and `POST /api/v1/auth/login` publicly.
2. `JwtAuthenticationFilter` reads `Authorization: Bearer <token>`, validates/parses via `JwtService`, and extracts `userId`.
3. The filter loads the user, wraps it in `AuthenticatedUser`, and stores it in `SecurityContext`.
4. `AuthService.register()` validates uniqueness, hashes password with BCrypt, saves user, and returns `AuthResponse(token, user)`.
5. `AuthService.login()` verifies password and returns a new JWT in `AuthResponse`.
6. Protected endpoints consume `@AuthenticationPrincipal AuthenticatedUser`; ownership checks are enforced in service layer methods (for example transcript CRUD).
7. Unauthorized requests return JSON `401` via `RestAuthenticationEntryPoint`; forbidden requests return JSON `403` via `RestAccessDeniedHandler`.
8. Outside `dev/test`, `ApplicationSecretsValidator` requires both `app.jwt.secret` and `app.admin.secret`.

---

## PDF Import / Parser Flow (Server)

### Key Files

- `server/src/main/java/com/example/server/controller/AdminImportController.java`
- `server/src/main/java/com/example/server/imports/orchestrator/AdminSecretValidator.java`
- `server/src/main/java/com/example/server/imports/orchestrator/AsyncPdfImportRunner.java`
- `server/src/main/java/com/example/server/imports/orchestrator/PdfImportOrchestrator.java`
- `server/src/main/java/com/example/server/imports/orchestrator/HttpContentFetcher.java`
- `server/src/main/java/com/example/server/imports/parser/PdfTextExtractor.java`
- `server/src/main/java/com/example/server/imports/parser/PdfCourseCatalogParser.java`
- `server/src/main/java/com/example/server/imports/parser/RequirementExpressionParser.java`
- `server/src/main/java/com/example/server/service/CourseImportService.java`
- `server/src/main/java/com/example/server/model/ImportJob.java`
- `server/src/main/java/com/example/server/model/ImportCourseResult.java`

### End-to-End Pipeline

1. `POST /api/v1/admin/imports/pdf-url` receives `sourcePageUrl` and `X-Admin-Secret`.
2. `AdminSecretValidator` verifies admin secret and `PdfImportOrchestrator.enqueueFromPageUrl()` creates a `PENDING` `ImportJob`.
3. `AsyncPdfImportRunner` executes the job asynchronously via `@Async("importTaskExecutor")`.
4. The submitted URL is treated as a direct PDF download URL (for example `https://www.cics.umass.edu/media/8761/download?attachment`).
5. Orchestrator moves job to `RUNNING`, downloads PDF bytes, enforces fetch constraints, and computes a SHA-256 source hash.
6. `PdfTextExtractor` (PDFBox) extracts raw text from bytes.
7. `PdfCourseCatalogParser` parses course blocks; prerequisite text is converted into recursive `RequirementDefinition` trees via `RequirementExpressionParser`.
8. `RequirementExpressionParser` normalizes UMass catalog phrasing and distinguishes `NOT_PRESENT`, `PARSED`, `UNSUPPORTED`, and `MALFORMED` prerequisite outcomes.
9. Each parsed course is imported through `CourseImportService.importCourse()` (new transaction) to upsert course + prerequisite tree.
10. If prerequisite parsing fails, inserts still proceed, updates preserve the existing prerequisite tree, and the import stores structured warning details rather than silently clearing prerequisites.
11. Per-course outcomes are stored as `ImportCourseResult` rows (`INSERTED`, `UPDATED`, `FAILED`) with warnings/errors, including raw and normalized prerequisite text.
12. `ImportJob` also records:
    - `prerequisiteTextExtractedCount`
    - `prerequisiteParsedCount`
    - `prerequisiteParseFailedCount`
13. Job status is finalized as `SUCCEEDED`, `PARTIAL_SUCCESS`, or `FAILED`, and results are available at:
    - `GET /api/v1/admin/imports/{jobId}`
    - `GET /api/v1/admin/imports/{jobId}/results`

### Current Assumptions

- Admin requests use a direct PDF download URL, not an HTML landing page.
- The current UMass CICS PDF layout is parsed as:
  - description text first
  - optional prerequisite sentence inside the description block
  - instructor line
  - course header line last (for example `CICS 109  Intro to Data Analysis in R`)
- `PdfCourseCatalogParser` treats the header line as the end of the record and builds the course from the preceding text block.

### Operational Notes

- If import jobs fail with a generic network error, restart the server with a clean rebuild so the latest `HttpContentFetcher` error messages are present.
- Local development uses `coursechecker-local-admin-secret` unless `APP_ADMIN_SECRET` overrides it.
- Frontend startup should prefer `corepack yarn dev` over assuming a globally installed `yarn` binary.
- Avoid leaving duplicate files with names like `* 2.java` or `* 2.sql` in source or test trees; they can break compilation or produce Flyway duplicate-version failures.

---

## Seed Data System

The seeder is **modular and decoupled**:

```
CourseDataProvider (interface)     ← contract for data
  └─ MockCourseDataProvider       ← hardcoded test data (current)
  └─ (future) WebCrawlerProvider  ← scrapes UMass website

DatabaseSeeder (CommandLineRunner) ← insertion logic (never changes)
  └─ injects CourseDataProvider
  └─ converts DTOs → JPA entities
  └─ idempotent (safe to re-run)
```

### To Add a New Data Source

1. Create a new class implementing `CourseDataProvider`
2. Annotate it with `@Component` and `@Primary` (or use `@Profile`)
3. The `DatabaseSeeder` will automatically pick it up via dependency injection

### Seed DTOs

- `CourseDefinition` — simple POJO (courseCode, title, credits, description, prerequisite)
- `RequirementDefinition` — recursive tree with factory methods: `.course()`, `.and()`, `.or()`
- These DTOs are intentionally NOT JPA entities — they isolate data shape from persistence

---

## Running the Project

```bash
# 1. Start the database
docker compose up -d

# 2. Start the Spring Boot backend (runs Flyway + seeder automatically)
cd server && ./mvnw spring-boot:run

# 3. Start the Vite frontend
cd client && corepack yarn dev
```

---

## Coding Conventions

- **Commits**: Use Conventional Commits (`feat:`, `fix:`, `chore:`, `docs:`, `refactor:`)
- **Java**: Follow standard Spring Boot conventions. Entities in `model/`, repos in `repository/`, seed in `seed/`
- **Frontend**: TypeScript strict mode. React functional components with hooks.
- **Never commit**: `docker/postgres-data/`, `node_modules/`, `target/`, IDE folders
- **Tests**: Place in `server/src/test/java/` mirroring the main package structure
