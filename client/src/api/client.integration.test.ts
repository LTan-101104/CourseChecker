import { afterEach, describe, expect, it, vi } from "vitest";
import { apiRequest, ApiError, setAuthTokenProvider } from "./client";

describe("apiRequest", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
    setAuthTokenProvider(null);
  });

  it("sends JSON bodies and bearer tokens for authenticated requests", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ ok: true }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);
    setAuthTokenProvider(() => "jwt-token");

    await expect(
      apiRequest<{ ok: boolean }>("/api/v1/users/me", {
        method: "PUT",
        auth: true,
        body: { displayName: "Ada" },
      }),
    ).resolves.toEqual({ ok: true });

    const [, init] = fetchMock.mock.calls[0];
    const headers = init.headers as Headers;
    expect(headers.get("Authorization")).toBe("Bearer jwt-token");
    expect(headers.get("Content-Type")).toBe("application/json");
    expect(init.body).toBe(JSON.stringify({ displayName: "Ada" }));
  });

  it("maps structured error responses to ApiError", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            message: "Validation failed",
            details: { courseCode: "must not be blank" },
          }),
          {
            status: 400,
            headers: { "Content-Type": "application/json" },
          },
        ),
      ),
    );

    await expect(apiRequest("/api/v1/admin/courses")).rejects.toMatchObject({
      name: "ApiError",
      message: "Validation failed",
      status: 400,
      details: { courseCode: "must not be blank" },
    } satisfies Partial<ApiError>);
  });

  it("returns undefined for no-content responses", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status: 204 })));

    await expect(apiRequest<void>("/api/v1/users/me/completed-courses/1")).resolves.toBeUndefined();
  });
});
