import { beforeAll, describe, expect, it } from "vitest";
import { login } from "../api/auth";
import { fetchCourseDetail, searchCourses } from "../api/courses";

const API_BASE_URL = "http://localhost:8080";

beforeAll(async () => {
  try {
    const response = await fetch(
      `${API_BASE_URL}/api/v1/courses/search?q=COMPSCI`,
    );
    if (!response.ok) {
      throw new Error(`Backend responded with HTTP ${response.status}`);
    }
  } catch (error) {
    const reason = error instanceof Error ? error.message : String(error);
    throw new Error(
      `Cross-stack tests require the backend running on ${API_BASE_URL} with the dev profile seeded.\n` +
        `Start it with:\n` +
        `  docker compose up -d\n` +
        `  cd server && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev\n\n` +
        `Underlying error: ${reason}`,
    );
  }
});

describe("frontend <-> backend integration", () => {
  it("searchCourses() returns seeded courses with the expected shape", async () => {
    const results = await searchCourses("COMPSCI");

    expect(Array.isArray(results)).toBe(true);
    expect(results.length).toBeGreaterThan(0);

    const first = results[0];
    expect(first.courseCode).toMatch(/^COMPSCI\s/);
    expect(typeof first.title).toBe("string");
    expect(first.title.length).toBeGreaterThan(0);
    expect(["CS", "CICS", "MATH", "STATS", "OTHER"]).toContain(
      first.courseType,
    );
  });

  it("login() authenticates a seeded user and returns a token", async () => {
    const response = await login({
      email: "jdoe@umass.edu",
      password: "password123!",
    });

    expect(typeof response.token).toBe("string");
    expect(response.token.length).toBeGreaterThan(0);
    expect(response.user.email).toBe("jdoe@umass.edu");
    expect(response.user.studentId).toBe("student-001");
  });

  it("fetchCourseDetail() returns the nested prerequisite tree (Composite pattern)", async () => {
    const detail = await fetchCourseDetail("COMPSCI 250");

    expect(detail.courseCode).toBe("COMPSCI 250");
    expect(typeof detail.title).toBe("string");
    expect(detail.title.length).toBeGreaterThan(0);

    expect(detail.prerequisite).not.toBeNull();
    const root = detail.prerequisite!;
    expect(root.type).toBe("AND");
    expect(Array.isArray(root.children)).toBe(true);
    expect(root.children.length).toBeGreaterThanOrEqual(2);

    const leafCodes = root.children
      .filter((child) => child.type === "COURSE")
      .map((child) => child.courseCode);
    expect(leafCodes).toContain("COMPSCI 187");
    expect(leafCodes).toContain("MATH 132");
  });
});
