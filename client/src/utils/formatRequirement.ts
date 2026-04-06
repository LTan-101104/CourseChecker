import type { Requirement } from "../types";

//! helper function to generate requirement string based on Requirement
export function formatRequirement(req: Requirement): string {
  switch (req.type) {
    case "COURSE":
      return req.requiredCourseCode;
    case "AND":
      return req.children.map(formatRequirement).join(" AND ");
    case "OR": {
      const inner = req.children.map(formatRequirement).join(" OR ");
      return `(${inner})`;
    }
  }
}