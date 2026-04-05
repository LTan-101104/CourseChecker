import type { CompletedCourse, Transcript } from "../types";

const completedCourses: CompletedCourse[] = [
  // student-001: eligible for COMPSCI 220
  { id: 1, studentId: "student-001", courseCode: "COMPSCI 121", grade: "A", semester: "Fall 2024" },
  { id: 2, studentId: "student-001", courseCode: "COMPSCI 187", grade: "B+", semester: "Spring 2025" },
  { id: 3, studentId: "student-001", courseCode: "MATH 131", grade: "A-", semester: "Fall 2024" },

  // student-002: NOT eligible for COMPSCI 220 (missing COMPSCI 187)
  { id: 4, studentId: "student-002", courseCode: "COMPSCI 121", grade: "B", semester: "Fall 2024" },
  { id: 5, studentId: "student-002", courseCode: "MATH 132", grade: "B+", semester: "Spring 2025" },
];

export const mockTranscripts: Record<string, Transcript> = {
  "student-001": {
    studentId: "student-001",
    completedCourses: completedCourses.filter((c) => c.studentId === "student-001"),
  },
  "student-002": {
    studentId: "student-002",
    completedCourses: completedCourses.filter((c) => c.studentId === "student-002"),
  },
};
