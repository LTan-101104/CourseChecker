const DEFAULT_API_BASE_URL = "http://localhost:8080";

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL?.trim() || DEFAULT_API_BASE_URL;

let authTokenProvider: (() => string | null) | null = null;

export interface ApiErrorDetails {
  [key: string]: string;
}

interface ErrorResponseBody {
  message?: string;
  error?: string;
  details?: ApiErrorDetails;
}

export class ApiError extends Error {
  readonly status: number;
  readonly details: ApiErrorDetails;

  constructor(message: string, status: number, details: ApiErrorDetails = {}) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.details = details;
  }
}

export function setAuthTokenProvider(provider: (() => string | null) | null) {
  authTokenProvider = provider;
}

interface RequestOptions extends Omit<RequestInit, "body"> {
  auth?: boolean;
  body?: unknown;
}

export async function apiRequest<T>(
  path: string,
  options: RequestOptions = {},
): Promise<T> {
  const { auth = false, headers, body, ...rest } = options;
  const requestHeaders = new Headers(headers);

  if (body !== undefined && !requestHeaders.has("Content-Type")) {
    requestHeaders.set("Content-Type", "application/json");
  }

  if (auth) {
    const token = authTokenProvider?.();
    if (token) {
      requestHeaders.set("Authorization", `Bearer ${token}`);
    }
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...rest,
    headers: requestHeaders,
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  if (response.ok) {
    if (response.status === 204) {
      return undefined as T;
    }
    return (await response.json()) as T;
  }

  let message = `Request failed with status ${response.status}`;
  let details: ApiErrorDetails = {};

  try {
    const errorBody = (await response.json()) as ErrorResponseBody;
    if (errorBody.message) {
      message = errorBody.message;
    } else if (errorBody.error) {
      message = errorBody.error;
    }
    if (errorBody.details) {
      details = errorBody.details;
    }
  } catch {
    // Ignore JSON parse errors and use the default message.
  }

  throw new ApiError(message, response.status, details);
}
