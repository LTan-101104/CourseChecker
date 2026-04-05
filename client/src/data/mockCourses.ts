import type { Course } from "../types";

export const mockCourses: Course[] = [
  {
    id: 1,
    courseCode: "COMPSCI 121",
    title: "Introduction to Problem Solving with Computers",
    credits: 4,
    description:
      "An introductory course in computer science for non-CS majors.",
    prerequisite: null,
  },
  {
    id: 2,
    courseCode: "MATH 131",
    title: "Calculus I",
    credits: 4,
    description:
      "First course in calculus covering limits, derivatives, and integrals.",
    prerequisite: null,
  },
  {
    id: 3,
    courseCode: "MATH 132",
    title: "Calculus II",
    credits: 4,
    description:
      "Second course in calculus covering integration techniques and series.",
    prerequisite: { type: "COURSE", requiredCourseCode: "MATH 131" },
  },
  {
    id: 4,
    courseCode: "COMPSCI 187",
    title: "Programming with Data Structures",
    credits: 4,
    description: "Use, design, and analysis of data structures.",
    prerequisite: { type: "COURSE", requiredCourseCode: "COMPSCI 121" },
  },
  {
    id: 5,
    courseCode: "COMPSCI 220",
    title: "Programming Methodology",
    credits: 4,
    description:
      "Development of individual skills necessary for designing, implementing, testing and modifying larger programs.",
    prerequisite: {
      type: "AND",
      children: [
        { type: "COURSE", requiredCourseCode: "COMPSCI 187" },
        {
          type: "OR",
          children: [
            { type: "COURSE", requiredCourseCode: "MATH 131" },
            { type: "COURSE", requiredCourseCode: "MATH 132" },
          ],
        },
      ],
    },
  },
  {
    id: 6,
    courseCode: "COMPSCI 230",
    title: "Computer Systems Principles",
    credits: 4,
    description:
      "Large-scale software systems development covering operating systems, networking, and distributed computing.",
    prerequisite: { type: "COURSE", requiredCourseCode: "COMPSCI 220" },
  },
];
