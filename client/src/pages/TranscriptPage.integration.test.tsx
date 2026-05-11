import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { TranscriptPage } from "./TranscriptPage";
import { useAuth } from "../context/AuthContext";
import { useCompletedCourses } from "../context/CompletedCoursesContext";
import { useCourseSearch } from "../hooks/useCourseSearch";

const addCourse = vi.fn();
const removeCourse = vi.fn();

vi.mock("../context/AuthContext", () => ({
  useAuth: vi.fn(),
}));

vi.mock("../context/CompletedCoursesContext", () => ({
  useCompletedCourses: vi.fn(),
}));

vi.mock("../hooks/useCourseSearch", () => ({
  useCourseSearch: vi.fn(),
}));

describe("TranscriptPage", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it("adds a selected search result to the transcript", async () => {
    vi.mocked(useAuth).mockReturnValue({
      token: "jwt-token",
      user: {
        id: 1,
        studentId: "student-001",
        displayName: "Ada Lovelace",
        email: "ada@umass.edu",
      },
      isAuthenticated: true,
      isInitializing: false,
      authError: null,
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
      bootstrapSession: vi.fn(),
    });
    vi.mocked(useCompletedCourses).mockReturnValue({
      courses: [
        {
          id: 1,
          courseCode: "COMPSCI 187",
          title: "Programming with Data Structures",
          credits: 4,
          grade: "A",
          semester: "Fall 2025",
        },
      ],
      loading: false,
      error: null,
      refresh: vi.fn(),
      addCourse,
      removeCourse,
    });
    vi.mocked(useCourseSearch).mockImplementation((query: string) => ({
      loading: false,
      error: null,
      results: query.trim()
        ? [
            {
              courseCode: "MATH 132",
              title: "Calculus II",
              credits: 4,
              courseType: "MATH",
            },
            {
              courseCode: "COMPSCI 187",
              title: "Programming with Data Structures",
              credits: 4,
              courseType: "CS",
            },
          ]
        : [],
    }));

    const user = userEvent.setup();
    render(<TranscriptPage />);

    await user.type(
      screen.getByPlaceholderText("Search and add a course to your transcript..."),
      "math",
    );
    await user.click(screen.getByText("MATH 132"));
    await user.click(screen.getByRole("button", { name: /add course/i }));

    await waitFor(() =>
      expect(addCourse).toHaveBeenCalledWith({
        courseCode: "MATH 132",
        grade: "—",
        semester: "Spring 2026",
      }),
    );
    expect(screen.queryByText("COMPSCI 187", { selector: ".dropdown-code" })).not.toBeInTheDocument();
  });
});
