import type { CompletedCourse, Transcript } from "../types";

const completedCourses: CompletedCourse[] = [
  // student-001
  { id: 1,  studentId: "student-001", courseCode: "COMPSCI 121", grade: "A",  semester: "Fall 2023" },
  { id: 2,  studentId: "student-001", courseCode: "COMPSCI 187", grade: "B+", semester: "Spring 2024" },
  { id: 3,  studentId: "student-001", courseCode: "COMPSCI 220", grade: "A-", semester: "Fall 2024" },
  { id: 4,  studentId: "student-001", courseCode: "COMPSCI 230", grade: "B",  semester: "Fall 2024" },
  { id: 5,  studentId: "student-001", courseCode: "COMPSCI 240", grade: "A",  semester: "Spring 2025" },
  { id: 6,  studentId: "student-001", courseCode: "COMPSCI 250", grade: "B+", semester: "Spring 2025" },
  { id: 7,  studentId: "student-001", courseCode: "COMPSCI 311", grade: "A-", semester: "Fall 2025" },
  { id: 8,  studentId: "student-001", courseCode: "COMPSCI 326", grade: "A",  semester: "Fall 2025" },
  { id: 9,  studentId: "student-001", courseCode: "MATH 131",    grade: "A-", semester: "Fall 2023" },
  { id: 10, studentId: "student-001", courseCode: "MATH 132",    grade: "B+", semester: "Spring 2024" },
  { id: 11, studentId: "student-001", courseCode: "MATH 233",    grade: "A",  semester: "Fall 2024" },
  { id: 12, studentId: "student-001", courseCode: "MATH 235",    grade: "B",  semester: "Spring 2025" },

  // student-002: NOT eligible for COMPSCI 220 (missing COMPSCI 187)
  { id: 13, studentId: "student-002", courseCode: "COMPSCI 121", grade: "B",  semester: "Fall 2024" },
  { id: 14, studentId: "student-002", courseCode: "MATH 132",    grade: "B+", semester: "Spring 2025" },
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
