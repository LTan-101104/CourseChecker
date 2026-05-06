import { describe, expect, it } from "vitest";
import { evaluateEligibility } from "./eligibilityEvaluator";
import type { Course, Requirement, Transcript } from "../types";

describe("evaluateEligibility stress", () => {
  it("evaluates a large prerequisite tree within a bounded time", () => {
    const prerequisite: Requirement = {
      type: "AND",
      children: Array.from({ length: 1_000 }, (_, index) => ({
        type: "COURSE",
        requiredCourseCode: `COMPSCI ${100 + index}`,
      })),
    };
    const course: Course = {
      id: 1,
      courseCode: "COMPSCI 999",
      title: "Large Requirement",
      credits: 4,
      description: "",
      prerequisite,
    };
    const transcript: Transcript = {
      studentId: "student-001",
      completedCourses: Array.from({ length: 999 }, (_, index) => ({
        id: index + 1,
        courseCode: `COMPSCI ${100 + index}`,
        grade: "A",
        semester: "Fall 2025",
        title: null,
        credits: null,
      })),
    };

    const result = evaluateEligibility(course, transcript);

    expect(result.isEligible).toBe(false);
    expect(result.missingRequirements).toEqual([
      { type: "COURSE", requiredCourseCode: "COMPSCI 1099" },
    ]);
  }, 1_000);
});
