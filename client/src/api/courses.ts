import { apiRequest } from "./client";

export interface CourseSummaryDTO {
  courseCode: string;
  title: string;
  credits: number | null;
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
}

export async function searchCourses(query: string): Promise<CourseSummaryDTO[]> {
  const normalized = query.trim();
  if (!normalized) {
    return [];
  }

  const params = new URLSearchParams({ q: normalized });
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
