# UMass Amherst BS CS Prerequisite Eligibility Checker

A web application that parses complex, nested AND/OR prerequisite logic to determine if a UMass Amherst CS student can enroll in a target course based on their transcript.

## Architecture

```mermaid
graph LR
    A["Vite + React<br/>(TypeScript)<br/>:5173"] -->|REST API| B["Spring Boot 4<br/>(Java 17 + JPA)<br/>:8080"]
    B -->|JDBC| C["PostgreSQL<br/>(Alpine)<br/>:5433"]
```

| Layer     | Directory  | Tech Stack                            |
|-----------|-----------|----------------------------------------|
| Frontend  | `client/` | Vite 7, React 19, TypeScript           |
| Backend   | `server/` | Spring Boot 4.0.3, JPA/Hibernate, Flyway |
| Database  | Docker    | PostgreSQL Alpine via `docker-compose`  |

## Data Model — Composite Pattern

The prerequisite system uses the **Composite Design Pattern** to model arbitrarily nested AND/OR logic as a tree:

```mermaid
classDiagram
    class Course {
        +Long id
        +String courseCode
        +String title
        +Integer credits
        +String description
        +Requirement prerequisite
    }

    class Requirement {
        <<abstract>>
        +Long id
    }

    class CourseRequirement {
        +String requiredCourseCode
    }

    class AndRequirement {
        +List~Requirement~ children
    }

    class OrRequirement {
        +List~Requirement~ children
    }

    Course --> Requirement : prerequisite
    Requirement <|-- CourseRequirement
    Requirement <|-- AndRequirement
    Requirement <|-- OrRequirement
    AndRequirement --> Requirement : children
    OrRequirement --> Requirement : children
```

### Example

**COMPSCI 220** requires: `COMPSCI 187 AND (MATH 131 OR MATH 132)`

```
Course(COMPSCI 220)
  └── prerequisite → AND
                      ├── COURSE("COMPSCI 187")
                      └── OR
                            ├── COURSE("MATH 131")
                            └── COURSE("MATH 132")
```

## Prerequisites

- **Java 17+** (for Spring Boot)
- **Node.js 18+** and **Yarn** (for the Vite frontend)
- **Docker** and **Docker Compose** (for PostgreSQL)

## Getting Started

### 1. Start the Database

```bash
docker compose up -d
```

This starts a lightweight PostgreSQL Alpine container with:
- **Database:** `coursechecker_db`
- **User:** `coursechecker`
- **Password:** `coursechecker`
- **Port:** `5433` (mapped from container's 5432 to avoid conflicts with native Postgres)

### 2. Start the Backend

```bash
cd server
./mvnw spring-boot:run
```

On first startup, Spring Boot will:
1. Run **Flyway migrations** to create the schema
2. Run the **DatabaseSeeder** to populate mock course data

The API is available at `http://localhost:8080`.

### 3. Start the Frontend

```bash
cd client
yarn install   # first time only
yarn dev
```

The UI is available at `http://localhost:5173`.

## Project Structure

```
CourseChecker/
├── client/                     # Vite + React frontend
│   ├── src/
│   ├── package.json
│   └── vite.config.ts
├── server/                     # Spring Boot backend
│   ├── src/main/java/com/example/server/
│   │   ├── model/              # JPA entities (Composite Pattern)
│   │   │   ├── Course.java
│   │   │   ├── Requirement.java        # abstract base
│   │   │   ├── CourseRequirement.java  # leaf
│   │   │   ├── AndRequirement.java     # composite
│   │   │   ├── OrRequirement.java      # composite
│   │   │   └── CompletedCourse.java    # transcript
│   │   ├── repository/         # Spring Data JPA repos
│   │   └── seed/               # Decoupled seed data system
│   │       ├── CourseDataProvider.java      # interface
│   │       ├── MockCourseDataProvider.java  # mock data
│   │       ├── DatabaseSeeder.java          # insertion logic
│   │       ├── CourseDefinition.java        # DTO
│   │       └── RequirementDefinition.java   # recursive DTO
│   ├── src/main/resources/
│   │   ├── application.yaml
│   │   └── db/migration/       # Flyway SQL migrations
│   └── pom.xml
├── docker-compose.yml
├── claude.md                   # Agent memory
└── README.md
```

## Database Management

```bash
# Start
docker compose up -d

# Stop (preserves data)
docker compose down

# Reset (destroys all data)
docker compose down -v && rm -rf docker/postgres-data
```

## Seed Data System

The seed system is designed for **data source swappability**:

- `CourseDataProvider` (interface) — defines the data contract
- `MockCourseDataProvider` — current implementation with hardcoded test data
- `DatabaseSeeder` — handles DB insertion (decoupled from data source)

To replace mock data with a web crawler, create a new `CourseDataProvider` implementation and annotate it with `@Primary`.

## Contributing

Use **Conventional Commits** for all commit messages:

```
feat: add prerequisite checking endpoint
fix: correct OR logic in requirement evaluation
chore: update dependencies
docs: improve setup instructions
```
