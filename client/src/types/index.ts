// ── Requirement (Composite Pattern) ─────────────────────

export type RequirementType = "COURSE" | "AND" | "OR";

export interface CourseRequirement {
  type: "COURSE";
  requiredCourseCode: string;
}

export interface AndRequirement {
  type: "AND";
  children: Requirement[];
}

export interface OrRequirement {
  type: "OR";
  children: Requirement[];
}

export type Requirement = CourseRequirement | AndRequirement | OrRequirement;

// ── Course ──────────────────────────────────────────────

export interface Course {
  id: number;
  courseCode: string;
  title: string;
  credits: number;
  description: string;
  prerequisite: Requirement | null;
}

// ── Transcript ──────────────────────────────────────────

export interface CompletedCourse {
  id: number;
  studentId: string;
  courseCode: string;
  grade: string;
  semester: string;
}

export interface Transcript {
  studentId: string;
  completedCourses: CompletedCourse[];
}

// ── Eligibility ─────────────────────────────────────────

export interface EvaluationResult {
  isEligible: boolean;
  missingRequirements: Requirement[];
  courseHasNoRequisiteDescription: boolean;
}
