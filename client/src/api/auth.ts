import { apiRequest } from "./client";

export interface CurrentUser {
  id: number;
  studentId: string;
  displayName: string;
  email: string;
}

interface AuthResponse {
  token: string;
  user: CurrentUser;
}

export interface LoginPayload {
  email: string;
  password: string;
}

export interface RegisterPayload {
  studentId: string;
  displayName: string;
  email: string;
  password: string;
}

export async function login(payload: LoginPayload): Promise<AuthResponse> {
  return apiRequest<AuthResponse>("/api/v1/auth/login", {
    method: "POST",
    body: payload,
  });
}

export async function register(
  payload: RegisterPayload,
): Promise<AuthResponse> {
  return apiRequest<AuthResponse>("/api/v1/auth/register", {
    method: "POST",
    body: payload,
  });
}

export async function fetchCurrentUser(): Promise<CurrentUser> {
  return apiRequest<CurrentUser>("/api/v1/auth/me", {
    method: "GET",
    auth: true,
  });
}
