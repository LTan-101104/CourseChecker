import { describe, expect, it } from "vitest";
import { evaluateEligibility } from "./eligibilityEvaluator";
import type { Course, Transcript } from "../types";

const transcript = (...courseCodes: string[]): Transcript => ({
  studentId: "student-001",
  completedCourses: courseCodes.map((courseCode, index) => ({
    id: index + 1,
    courseCode,
    grade: "A",
    semester: "Fall 2025",
    title: courseCode,
    credits: 4,
  })),
});

describe("evaluateEligibility", () => {
  it("marks courses without prerequisites as eligible", () => {
    const course: Course = {
      id: 1,
      courseCode: "COMPSCI 121",
      title: "Intro",
      credits: 4,
      description: "",
      prerequisite: null,
    };

    expect(evaluateEligibility(course, transcript())).toEqual({
      isEligible: true,
      missingRequirements: [],
      courseHasNoRequisiteDescription: true,
    });
  });

  it("collects missing requirements across AND nodes", () => {
    const course: Course = {
      id: 2,
      courseCode: "COMPSCI 240",
      title: "Reasoning Under Uncertainty",
      credits: 4,
      description: "",
      prerequisite: {
        type: "AND",
        children: [
          { type: "COURSE", requiredCourseCode: "COMPSCI 187" },
          { type: "COURSE", requiredCourseCode: "MATH 132" },
        ],
      },
    };

    const result = evaluateEligibility(course, transcript("COMPSCI 187"));

    expect(result.isEligible).toBe(false);
    expect(result.missingRequirements).toEqual([
      { type: "COURSE", requiredCourseCode: "MATH 132" },
    ]);
    expect(result.courseHasNoRequisiteDescription).toBe(false);
  });

  it("treats an OR node as satisfied when any branch is complete", () => {
    const course: Course = {
      id: 3,
      courseCode: "COMPSCI 345",
      title: "Data Management",
      credits: 3,
      description: "",
      prerequisite: {
        type: "OR",
        children: [
          { type: "COURSE", requiredCourseCode: "CICS 210" },
          { type: "COURSE", requiredCourseCode: "COMPSCI 187" },
        ],
      },
    };

    expect(evaluateEligibility(course, transcript("COMPSCI 187")).isEligible).toBe(true);
  });

  it("returns the whole OR node when no alternative is met", () => {
    const prerequisite = {
      type: "OR" as const,
      children: [
        { type: "COURSE" as const, requiredCourseCode: "COMPSCI 230" },
        { type: "COURSE" as const, requiredCourseCode: "COMPSCI 377" },
      ],
    };
    const course: Course = {
      id: 4,
      courseCode: "COMPSCI 453",
      title: "Computer Networks",
      credits: 3,
      description: "",
      prerequisite,
    };

    const result = evaluateEligibility(course, transcript("COMPSCI 220"));

    expect(result.isEligible).toBe(false);
    expect(result.missingRequirements).toEqual([prerequisite]);
  });
});
