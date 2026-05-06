import { afterEach, describe, expect, it, vi } from "vitest";
import { searchCourses } from "./courses";

describe("searchCourses", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("does not call the API for an empty search without a type filter", async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);

    await expect(searchCourses("   ")).resolves.toEqual([]);

    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("includes trimmed query and optional course type in the request", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify([]), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    await searchCourses(" calculus ", "MATH");

    const [url] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8080/api/v1/courses/search?q=calculus&type=MATH");
  });
});
