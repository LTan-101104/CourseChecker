import { useEffect, useState } from "react";
import { searchCourses, type CourseSummaryDTO } from "../api/courses";
import { ApiError } from "../api/client";

const SEARCH_DEBOUNCE_MS = 300;

export function useCourseSearch(query: string) {
  const [results, setResults] = useState<CourseSummaryDTO[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const normalized = query.trim();
    if (!normalized) {
      setResults([]);
      setError(null);
      setLoading(false);
      return;
    }

    let isCancelled = false;
    const timeoutId = setTimeout(() => {
      void (async () => {
        setLoading(true);
        try {
          const data = await searchCourses(normalized);
          if (!isCancelled) {
            setResults(data);
            setError(null);
          }
        } catch (err) {
          if (isCancelled) {
            return;
          }
          if (err instanceof ApiError) {
            setError(err.message);
          } else {
            setError("Unable to search courses");
          }
          setResults([]);
        } finally {
          if (!isCancelled) {
            setLoading(false);
          }
        }
      })();
    }, SEARCH_DEBOUNCE_MS);

    return () => {
      isCancelled = true;
      clearTimeout(timeoutId);
    };
  }, [query]);

  return { results, loading, error };
}
