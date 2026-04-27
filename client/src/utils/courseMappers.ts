import type { CourseDetailDTO, RequirementNodeDTO } from "../api/courses";
import type { Course, Requirement } from "../types";

function mapRequirementNode(node: RequirementNodeDTO): Requirement {
  switch (node.type) {
    case "COURSE":
      return {
        type: "COURSE",
        requiredCourseCode: node.courseCode ?? "",
      };
    case "AND":
      return {
        type: "AND",
        children: node.children.map(mapRequirementNode),
      };
    case "OR":
      return {
        type: "OR",
        children: node.children.map(mapRequirementNode),
      };
  }
}

export function mapCourseDetail(dto: CourseDetailDTO): Course {
  return {
    id: 0,
    courseCode: dto.courseCode,
    title: dto.title,
    credits: dto.credits ?? 0,
    description: dto.description ?? "",
    prerequisite: dto.prerequisite ? mapRequirementNode(dto.prerequisite) : null,
  };
}
