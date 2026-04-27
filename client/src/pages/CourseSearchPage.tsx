import { useState, useMemo } from "react";
import { Bell } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { useCourseSearch } from "../hooks/useCourseSearch";
import { CourseCard } from "../components/CourseCard";
import "./CourseSearchPage.css";

type Filter = "All" | "COMPSCI" | "MATH";

export function CourseSearchPage() {
  const { user } = useAuth();
  const [activeFilter, setActiveFilter] = useState<Filter>("All");
  const [searchTerm, setSearchTerm] = useState("");
  const { results, loading, error } = useCourseSearch(searchTerm);

  const filters: Filter[] = ["All", "COMPSCI", "MATH"];

  const filteredCourses = useMemo(() => {
    let currentResults = results;

    if (activeFilter !== "All") {
      currentResults = currentResults.filter((c) =>
        c.courseCode.startsWith(activeFilter),
      );
    }
    return currentResults;
  }, [activeFilter, results]);

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
        {filters.map((f) => (
          <button
            key={f}
            className={`filter-badge ${activeFilter === f ? "active" : ""}`}
            onClick={() => setActiveFilter(f)}
          >
            {f === "All" ? "All Courses" : f}
          </button>
        ))}
        <span className="result-count">
          {filteredCourses.length} courses found
        </span>
      </div>

      {/* Results Grid */}
      <div className="results-grid">
        {!searchTerm.trim() && (
          <p className="result-empty">Type to search courses in the database.</p>
        )}
        {searchTerm.trim() && loading && (
          <p className="result-empty">Searching courses...</p>
        )}
        {searchTerm.trim() && !loading && error && (
          <p className="result-empty">{error}</p>
        )}
        {searchTerm.trim() && !loading && !error && filteredCourses.length === 0 && (
          <p className="result-empty">No courses found.</p>
        )}
        {filteredCourses.map((course) => (
          <CourseCard key={course.courseCode} course={course} />
        ))}
      </div>
    </div>
  );
}
