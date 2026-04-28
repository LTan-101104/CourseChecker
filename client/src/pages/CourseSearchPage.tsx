import { useState } from "react";
import { Bell } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { useCourseSearch } from "../hooks/useCourseSearch";
import { CourseCard } from "../components/CourseCard";
import type { CourseType } from "../api/courses";
import "./CourseSearchPage.css";

type Filter = "All" | CourseType;

const FILTER_LABELS: Record<Filter, string> = {
  All: "All Courses",
  CS: "COMPSCI",
  CICS: "CICS",
  MATH: "MATH",
  STATS: "STATISTICS",
  OTHER: "Other",
};

const FILTERS: Filter[] = ["All", "CS", "CICS", "MATH", "STATS"];

export function CourseSearchPage() {
  const { user } = useAuth();
  const [activeFilter, setActiveFilter] = useState<Filter>("All");
  const [searchTerm, setSearchTerm] = useState("");

  const activeType = activeFilter === "All" ? undefined : activeFilter;
  const { results, loading, error } = useCourseSearch(searchTerm, activeType);

  const hasQuery = !!searchTerm.trim() || activeFilter !== "All";

  return (
    <div className="search-page">
      {/* Header */}
      <div className="search-header">
        <div className="search-header-left">
          <h1 className="page-title">Course Search</h1>
          <p className="page-subtitle">
            Browse and explore UMass CS courses and their prerequisites
          </p>
        </div>
        <div className="search-header-right">
          <Bell size={20} color="#737373" />
          <div className="header-avatar">
            {user?.displayName?.charAt(0).toUpperCase() ?? "G"}
          </div>
        </div>
      </div>

      {/* Search Row */}
      <div className="search-row">
        <div className="search-input-wrapper">
          <input
            className="search-input"
            type="text"
            placeholder="Search by course code or title..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
      </div>

      {/* Filters */}
      <div className="filters-row">
        <span className="filter-label">Filters:</span>
        {FILTERS.map((f) => (
          <button
            key={f}
            className={`filter-badge ${activeFilter === f ? "active" : ""}`}
            onClick={() => setActiveFilter(f)}
          >
            {FILTER_LABELS[f]}
          </button>
        ))}
        <span className="result-count">{results.length} courses found</span>
      </div>

      {/* Results Grid */}
      <div className="results-grid">
        {!hasQuery && (
          <p className="result-empty">Type to search courses in the database.</p>
        )}
        {hasQuery && loading && (
          <p className="result-empty">Searching courses...</p>
        )}
        {hasQuery && !loading && error && (
          <p className="result-empty">{error}</p>
        )}
        {hasQuery && !loading && !error && results.length === 0 && (
          <p className="result-empty">No courses found.</p>
        )}
        {results.map((course) => (
          <CourseCard key={course.courseCode} course={course} />
        ))}
      </div>
    </div>
  );
}
