import { apiRequest } from "./client";

export type CourseType = "CS" | "CICS" | "MATH" | "STATS" | "OTHER";

export interface CourseSummaryDTO {
  courseCode: string;
  title: string;
  credits: number | null;
  courseType: CourseType;
}

export interface RequirementNodeDTO {
  type: "COURSE" | "AND" | "OR";
  courseCode: string | null;
  children: RequirementNodeDTO[];
}

export interface CourseDetailDTO {
  courseCode: string;
  title: string;
  credits: number | null;
  description: string | null;
  prerequisite: RequirementNodeDTO | null;
  prerequisiteDescription: string | null;
  courseType: CourseType;
}

export async function searchCourses(
  query: string,
  type?: CourseType,
): Promise<CourseSummaryDTO[]> {
  const normalized = query.trim();
  if (!normalized && !type) {
    return [];
  }

  const params = new URLSearchParams({ q: normalized });
  if (type) {
    params.set("type", type);
  }
  return apiRequest<CourseSummaryDTO[]>(`/api/v1/courses/search?${params.toString()}`, {
    method: "GET",
  });
}

export async function fetchCourseDetail(
  courseCode: string,
): Promise<CourseDetailDTO> {
  return apiRequest<CourseDetailDTO>(
    `/api/v1/courses/${encodeURIComponent(courseCode)}`,
    {
      method: "GET",
    },
  );
}
