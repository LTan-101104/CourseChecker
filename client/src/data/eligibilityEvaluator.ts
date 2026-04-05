import type { Course, EvaluationResult, Requirement, Transcript } from "../types";

/**
 * Evaluates whether a student's transcript satisfies a course's prerequisites.
 * Mirrors the EligibilityEvaluator from the model diagram.
 */
export function evaluateEligibility(
  course: Course,
  transcript: Transcript
): EvaluationResult {
  if (!course.prerequisite) {
    return {
      isEligible: true,
      missingRequirements: [],
      courseHasNoRequisiteDescription: true,
    };
  }

  const completedCodes = new Set(
    transcript.completedCourses.map((c) => c.courseCode)
  );

  const missing = findMissing(course.prerequisite, completedCodes);

  return {
    isEligible: missing.length === 0,
    missingRequirements: missing,
    courseHasNoRequisiteDescription: false,
  };
}

/**
 * Recursively walks the prerequisite tree and returns the unsatisfied
 * Requirement nodes, preserving their tree structure.
 */
function findMissing(
  req: Requirement,
  completed: Set<string>
): Requirement[] {
  switch (req.type) {
    case "COURSE": {
      return completed.has(req.requiredCourseCode) ? [] : [req];
    }
    case "AND": {
      // All children must be met — collect every unsatisfied subtree
      const unsatisfied = req.children.flatMap((child) =>
        findMissing(child, completed)
      );
      return unsatisfied;
    }
    case "OR": {
      // Any one child is enough — if at least one is fully met, nothing missing
      const anyMet = req.children.some(
        (child) => findMissing(child, completed).length === 0
      );
      if (anyMet) return [];
      // None met — return the whole OR node so the UI can show "need one of..."
      return [req];
    }
  }
}
