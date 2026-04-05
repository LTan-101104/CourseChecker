import { useState, useMemo } from "react";
import { Bell } from "lucide-react";
import { mockCourses } from "../data/mockCourses";
import type { Course, Requirement } from "../types";
import "./CourseSearchPage.css";

type Filter = "All" | "COMPSCI" | "MATH";

//! helper function to generate requirement string based on Requirement
function formatRequirement(req: Requirement): string {
  switch (req.type) {
    case "COURSE":
      return req.requiredCourseCode;
    case "AND":
      return req.children.map(formatRequirement).join(" AND ");
    case "OR": {
      const inner = req.children.map(formatRequirement).join(" OR ");
      return `(${inner})`;
    }
  }
}

export function CourseSearchPage() {
  // const [query, setQuery] = useState("");
  const [activeFilter, setActiveFilter] = useState<Filter>("All");
  const [searchTerm, setSearchTerm] = useState("");

  const filters: Filter[] = ["All", "COMPSCI", "MATH"];

  const filteredCourses = useMemo(() => {
    let results = mockCourses;

    if (activeFilter !== "All") {
      results = results.filter((c) => c.courseCode.startsWith(activeFilter));
    }

    if (searchTerm) {
      const term = searchTerm.toLowerCase();
      results = results.filter(
        (c) =>
          c.courseCode.toLowerCase().includes(term) ||
          c.title.toLowerCase().includes(term),
      );
    }

    return results;
  }, [activeFilter, searchTerm]);

  function handleOnChange(e: React.ChangeEvent<HTMLInputElement>) {
    setSearchTerm(e.target.value);
  }

  // Split into 3 columns
  const columns: Course[][] = [[], [], []];
  filteredCourses.forEach((course, i) => {
    columns[i % 3].push(course);
  });

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
          <div className="header-avatar">JD</div>
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
            onChange={(e) => handleOnChange(e)}
          />
        </div>
        {/* <button className="search-btn" onClick={handleSearch}>
          <Search size={16} />
          Search
        </button> */}
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
        {columns.map((col, colIndex) => (
          <div className="results-column" key={colIndex}>
            {col.map((course) => (
              <CourseCard key={course.id} course={course} />
            ))}
          </div>
        ))}
      </div>
    </div>
  );
}

function CourseCard({ course }: { course: Course }) {
  return (
    <div className="course-card">
      <div className="card-top">
        <span className="card-code">{course.courseCode}</span>
        <span className="card-credits">{course.credits} cr</span>
      </div>
      <div className="card-title">{course.title}</div>
      <div className="card-prereq">
        <span className="prereq-label">Prerequisites</span>
        <span className="prereq-value">
          {course.prerequisite
            ? formatRequirement(course.prerequisite)
            : "None"}
        </span>
      </div>
      <div className="card-actions">
        <button className="view-details-btn">View Details</button>
      </div>
    </div>
  );
}
