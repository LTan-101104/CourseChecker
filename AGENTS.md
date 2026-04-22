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
cd client && yarn dev
```

---

## Coding Conventions

- **Commits**: Use Conventional Commits (`feat:`, `fix:`, `chore:`, `docs:`, `refactor:`)
- **Java**: Follow standard Spring Boot conventions. Entities in `model/`, repos in `repository/`, seed in `seed/`
- **Frontend**: TypeScript strict mode. React functional components with hooks.
- **Never commit**: `docker/postgres-data/`, `node_modules/`, `target/`, IDE folders
- **Tests**: Place in `server/src/test/java/` mirroring the main package structure
