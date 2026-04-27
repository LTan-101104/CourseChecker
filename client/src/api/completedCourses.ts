import { apiRequest } from "./client";

export interface CompletedCourseDTO {
  id: number;
  courseCode: string;
  grade: string | null;
  semester: string | null;
  title: string | null;
  credits: number | null;
}

export interface UpsertCompletedCoursePayload {
  courseCode: string;
  grade?: string;
  semester?: string;
}

export async function listCompletedCourses(): Promise<CompletedCourseDTO[]> {
  return apiRequest<CompletedCourseDTO[]>("/api/v1/users/me/completed-courses", {
    method: "GET",
    auth: true,
  });
}

export async function createCompletedCourse(
  payload: UpsertCompletedCoursePayload,
): Promise<CompletedCourseDTO> {
  return apiRequest<CompletedCourseDTO>("/api/v1/users/me/completed-courses", {
    method: "POST",
    auth: true,
    body: payload,
  });
}

export async function updateCompletedCourse(
  completedCourseId: number,
  payload: UpsertCompletedCoursePayload,
): Promise<CompletedCourseDTO> {
  return apiRequest<CompletedCourseDTO>(
    `/api/v1/users/me/completed-courses/${completedCourseId}`,
    {
      method: "PUT",
      auth: true,
      body: payload,
    },
  );
}

export async function deleteCompletedCourse(
  completedCourseId: number,
): Promise<void> {
  return apiRequest<void>(`/api/v1/users/me/completed-courses/${completedCourseId}`, {
    method: "DELETE",
    auth: true,
  });
}
