/* eslint-disable react-refresh/only-export-components */
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import {
  createCompletedCourse,
  deleteCompletedCourse,
  listCompletedCourses,
  type CompletedCourseDTO,
  type UpsertCompletedCoursePayload,
} from "../api/completedCourses";
import { ApiError } from "../api/client";
import type { CompletedCourse } from "../types";
import { useAuth } from "./AuthContext";

interface CompletedCoursesContextValue {
  courses: CompletedCourse[];
  loading: boolean;
  error: string | null;
  refresh: () => Promise<void>;
  addCourse: (payload: UpsertCompletedCoursePayload) => Promise<void>;
  removeCourse: (id: number) => Promise<void>;
}

const CompletedCoursesContext =
  createContext<CompletedCoursesContextValue | null>(null);

function mapCompletedCourse(dto: CompletedCourseDTO): CompletedCourse {
  return {
    id: dto.id,
    courseCode: dto.courseCode,
    grade: dto.grade ?? "—",
    semester: dto.semester ?? "—",
  };
}

export function CompletedCoursesProvider({ children }: { children: ReactNode }) {
  const { isAuthenticated, user, isInitializing } = useAuth();
  const [courses, setCourses] = useState<CompletedCourse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    if (!isAuthenticated) {
      setCourses([]);
      setError(null);
      return;
    }

    setLoading(true);
    try {
      const data = await listCompletedCourses();
      setCourses(data.map(mapCompletedCourse));
      setError(null);
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Failed to load transcript");
      }
    } finally {
      setLoading(false);
    }
  }, [isAuthenticated]);

  useEffect(() => {
    if (isInitializing) {
      return;
    }
    void refresh();
  }, [isInitializing, refresh, user?.id]);

  const addCourse = useCallback(
    async (payload: UpsertCompletedCoursePayload) => {
      await createCompletedCourse(payload);
      await refresh();
    },
    [refresh],
  );

  const removeCourse = useCallback(
    async (id: number) => {
      await deleteCompletedCourse(id);
      await refresh();
    },
    [refresh],
  );

  const value = useMemo<CompletedCoursesContextValue>(
    () => ({
      courses,
      loading,
      error,
      refresh,
      addCourse,
      removeCourse,
    }),
    [addCourse, courses, error, loading, refresh, removeCourse],
  );

  return (
    <CompletedCoursesContext.Provider value={value}>
      {children}
    </CompletedCoursesContext.Provider>
  );
}

export function useCompletedCourses(): CompletedCoursesContextValue {
  const context = useContext(CompletedCoursesContext);
  if (!context) {
    throw new Error(
      "useCompletedCourses must be used within a CompletedCoursesProvider",
    );
  }
  return context;
}
