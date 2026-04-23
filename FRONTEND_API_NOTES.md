# Frontend API Notes

This document describes the course catalog endpoints exposed by the Spring Boot backend for the UMass Amherst BS CS Prerequisite Eligibility Checker.

## Base URL

- Local backend base URL: `http://localhost:8080`

## Frontend Routing + Auth Policy

- Public routes:
  - `/auth`
  - `/search`
  - `/course/:courseCode`
- Protected routes (JWT required):
  - `/`
  - `/eligibility`
  - `/transcript`
- Redirect rules:
  - unauthenticated users trying to open protected routes are redirected to `/auth`
  - authenticated users who visit `/auth` are redirected to `/`

## Authentication Endpoints

### Register

- Endpoint: `POST /api/v1/auth/register`
- Purpose: create account and return a JWT + current user payload

Request:

```json
{
  "studentId": "A12345678",
  "displayName": "Jane Doe",
  "email": "jane@umass.edu",
  "password": "password123"
}
```

### Login

- Endpoint: `POST /api/v1/auth/login`
- Purpose: authenticate and return a JWT + current user payload

Request:

```json
{
  "email": "jane@umass.edu",
  "password": "password123"
}
```

### Current User

- Endpoint: `GET /api/v1/auth/me`
- Purpose: validate token and return current authenticated user
- Required header: `Authorization: Bearer <jwt>`

Common auth response shape:

```json
{
  "token": "<jwt>",
  "user": {
    "id": 1,
    "studentId": "A12345678",
    "displayName": "Jane Doe",
    "email": "jane@umass.edu"
  }
}
```

## Transcript (Completed Courses) Endpoints

- Base path: `/api/v1/users/me/completed-courses`
- All endpoints require `Authorization: Bearer <jwt>`

### List Completed Courses

- `GET /api/v1/users/me/completed-courses`

### Create Completed Course

- `POST /api/v1/users/me/completed-courses`

Request:

```json
{
  "courseCode": "COMPSCI 220",
  "grade": "A-",
  "semester": "Spring 2026"
}
```

### Update Completed Course

- `PUT /api/v1/users/me/completed-courses/{completedCourseId}`
- Request body is the same as create.

### Delete Completed Course

- `DELETE /api/v1/users/me/completed-courses/{completedCourseId}`

## 1. Search Courses

- Endpoint: `GET /api/v1/courses/search?q={query}`
- Purpose: autocomplete and catalog lookup by partial `courseCode` or partial `title`
- Notes:
  - matching is case-insensitive
  - blank queries return an empty list
  - results are capped to the top 10 matches for autocomplete responsiveness

### Example Request

```http
GET http://localhost:8080/api/v1/courses/search?q=COMPSCI%201
```

### Example Response

```json
[
  {
    "courseCode": "COMPSCI 187",
    "title": "Programming with Data Structures",
    "credits": 4
  },
  {
    "courseCode": "COMPSCI 220",
    "title": "Programming Methodology",
    "credits": 4
  }
]
```

## 2. Get Full Course Details

- Endpoint: `GET /api/v1/courses/{courseCode}`
- Purpose: fetch full details for one course by exact course code
- Notes:
  - `courseCode` should be URL-encoded when it contains spaces
  - example: `COMPSCI 220` becomes `COMPSCI%20220`
  - if a course is not found, the API returns HTTP `404`

### Example Request

```http
GET http://localhost:8080/api/v1/courses/COMPSCI%20220
```

### Example Response

```json
{
  "courseCode": "COMPSCI 220",
  "title": "Programming Methodology",
  "credits": 4,
  "description": "Object-oriented design and software engineering.",
  "prerequisiteDescription": "COMPSCI 187 AND (MATH 131 OR MATH 132)"
}
```

## DTO Shapes

### `CourseSummaryDTO`

```ts
type CourseSummaryDTO = {
  courseCode: string;
  title: string;
  credits: number | null;
};
```

### `CourseDetailDTO`

```ts
type CourseDetailDTO = {
  courseCode: string;
  title: string;
  credits: number | null;
  description: string | null;
  prerequisiteDescription: string | null;
};
```

## Frontend Integration Guidance

- For React or Vue apps running in Vite, call the search endpoint from the browser against `http://localhost:8080`.
- The backend allows cross-origin requests from `http://localhost:5173` and `http://127.0.0.1:5173`.
- For protected routes and transcript CRUD, include the bearer token in `Authorization` headers.
- Implement search input debouncing with about a `300ms` delay before firing `GET /search`.
- Debouncing is important because it prevents spamming the backend on every keystroke and helps keep autocomplete within the `500ms` latency non-functional requirement.
- Avoid calling the search endpoint for blank or whitespace-only input.
