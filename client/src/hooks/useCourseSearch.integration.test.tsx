import { act, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { useCourseSearch } from "./useCourseSearch";
import { searchCourses } from "../api/courses";

vi.mock("../api/courses", async (importOriginal) => {
  const actual = await importOriginal<typeof import("../api/courses")>();
  return {
    ...actual,
    searchCourses: vi.fn(),
  };
});

function SearchProbe({ query, type }: { query: string; type?: "CS" | "MATH" }) {
  const { results, loading, error } = useCourseSearch(query, type);

  return (
    <div>
      <output aria-label="loading">{String(loading)}</output>
      <output aria-label="error">{error ?? ""}</output>
      <output aria-label="count">{results.length}</output>
    </div>
  );
}

describe("useCourseSearch", () => {
  afterEach(() => {
    vi.useRealTimers();
    vi.clearAllMocks();
  });

  it("debounces course search requests and exposes results", async () => {
    vi.useFakeTimers();
    vi.mocked(searchCourses).mockResolvedValue([
      {
        courseCode: "COMPSCI 187",
        title: "Programming with Data Structures",
        credits: 4,
        courseType: "CS",
      },
    ]);

    render(<SearchProbe query=" compsci " />);

    expect(searchCourses).not.toHaveBeenCalled();

    await act(async () => {
      await vi.advanceTimersByTimeAsync(300);
    });

    expect(screen.getByLabelText("count")).toHaveTextContent("1");
    expect(searchCourses).toHaveBeenCalledWith("compsci", undefined);
    expect(screen.getByLabelText("error")).toHaveTextContent("");
  });

  it("allows type-only searches", async () => {
    vi.useFakeTimers();
    vi.mocked(searchCourses).mockResolvedValue([]);

    render(<SearchProbe query=" " type="MATH" />);

    await act(async () => {
      await vi.advanceTimersByTimeAsync(300);
    });

    expect(searchCourses).toHaveBeenCalledWith("", "MATH");
  });
});
