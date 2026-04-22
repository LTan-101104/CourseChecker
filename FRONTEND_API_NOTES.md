# Frontend API Notes

This document describes the course catalog endpoints exposed by the Spring Boot backend for the UMass Amherst BS CS Prerequisite Eligibility Checker.

## Base URL

- Local backend base URL: `http://localhost:8080/api/v1/courses`

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
- Implement search input debouncing with about a `300ms` delay before firing `GET /search`.
- Debouncing is important because it prevents spamming the backend on every keystroke and helps keep autocomplete within the `500ms` latency non-functional requirement.
- Avoid calling the search endpoint for blank or whitespace-only input.
