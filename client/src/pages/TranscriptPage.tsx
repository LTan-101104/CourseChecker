import { useMemo, useState } from "react";
import { Bell, Plus, Trash2, Search } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { type CourseSummaryDTO } from "../api/courses";
import { ApiError } from "../api/client";
import { useCompletedCourses } from "../context/CompletedCoursesContext";
import { useCourseSearch } from "../hooks/useCourseSearch";
import "./TranscriptPage.css";

const PAGE_SIZE = 9;

export function TranscriptPage() {
  const { user } = useAuth();
  const { courses, loading, error, addCourse, removeCourse } =
    useCompletedCourses();
  const [page] = useState(0);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [showAddDropdown, setShowAddDropdown] = useState(false);
  const [addQuery, setAddQuery] = useState("");
  const [selectedAddCourse, setSelectedAddCourse] =
    useState<CourseSummaryDTO | null>(null);
  const {
    results: addSearchResults,
    loading: isSearchingAddCourses,
    error: addSearchError,
  } = useCourseSearch(addQuery);

  const addableCourses = useMemo(
    () =>
      addSearchResults.filter(
        (candidate) =>
          !courses.some((course) => course.courseCode === candidate.courseCode),
      ),
    [addSearchResults, courses],
  );

  // Stats
  const totalCourses = courses.length;
  const totalCredits = courses.reduce((sum, c) => sum + (c.credits ?? 0), 0);
  const csCourses = courses.filter((c) =>
    c.courseCode.startsWith("COMPSCI"),
  ).length;
  const mathCourses = courses.filter((c) =>
    c.courseCode.startsWith("MATH"),
  ).length;

  // Pagination
  const totalPages = Math.ceil(courses.length / PAGE_SIZE);
  const currentPage = Math.min(page, Math.max(totalPages - 1, 0));
  const pagedCourses = courses.slice(
    currentPage * PAGE_SIZE,
    (currentPage + 1) * PAGE_SIZE,
  );

  async function handleAdd() {
    if (!selectedAddCourse) return;

    try {
      setSubmitError(null);
      await addCourse({
        courseCode: selectedAddCourse.courseCode,
        grade: "—",
        semester: "Spring 2026",
      });
      setSelectedAddCourse(null);
      setAddQuery("");
      setShowAddDropdown(false);
    } catch (err) {
      if (err instanceof ApiError) {
        setSubmitError(err.message);
      } else {
        setSubmitError("Unable to add course");
      }
    }
  }

  async function handleDelete(id: number) {
    try {
      setSubmitError(null);
      await removeCourse(id);
    } catch (err) {
      if (err instanceof ApiError) {
        setSubmitError(err.message);
      } else {
        setSubmitError("Unable to delete course");
      }
    }
  }

  return (
    <div className="transcript-page">
      {/* Header */}
      <div className="transcript-header">
        <div className="transcript-header-left">
          <h1 className="page-title">My Transcript</h1>
          <p className="page-subtitle">
            Manage your completed courses to check eligibility
          </p>
        </div>
        <div className="transcript-header-right">
          <Bell size={20} color="#737373" />
          <div className="header-avatar">
            {user?.displayName?.charAt(0).toUpperCase() ?? "G"}
          </div>
        </div>
      </div>

      {/* Stat Cards */}
      <div className="stats-row">
        <StatCard
          title="Total Courses"
          value={totalCourses}
          badge="CS & Math"
        />
        <StatCard title="Total Credits" value={totalCredits} badge="of 120" />
        <StatCard title="CS Courses" value={csCourses} badge="completed" />
        <StatCard title="Math Courses" value={mathCourses} badge="completed" />
      </div>

      {/* Add Course Row */}
      <div className="add-course-row">
        <div className="add-course-select-wrapper">
          <input
            className="add-course-select"
            type="text"
            value={addQuery}
            placeholder="Search and add a course to your transcript..."
            onChange={(e) => {
              setAddQuery(e.target.value);
              setSelectedAddCourse(null);
              setSubmitError(null);
              setShowAddDropdown(true);
            }}
            onFocus={() => setShowAddDropdown(true)}
            onBlur={() => setTimeout(() => setShowAddDropdown(false), 150)}
          />
          <Search size={16} className="select-icon" />
          {showAddDropdown && addableCourses.length > 0 && (
            <ul className="add-course-dropdown">
              {addableCourses.map((course) => (
                <li
                  key={course.courseCode}
                  className="add-course-dropdown-item"
                  onMouseDown={() => {
                    setSelectedAddCourse(course);
                    setAddQuery(`${course.courseCode} — ${course.title}`);
                    setShowAddDropdown(false);
                  }}
                >
                  <span className="dropdown-code">{course.courseCode}</span>
                  <span className="dropdown-title">{course.title}</span>
                </li>
              ))}
            </ul>
          )}
        </div>
        <button
          className="add-btn"
          onClick={handleAdd}
          disabled={!selectedAddCourse || loading}
        >
          <Plus size={16} />
          Add Course
        </button>
      </div>

      {(error || submitError || addSearchError) && (
        <p className="page-subtitle">
          {submitError ?? addSearchError ?? error}
        </p>
      )}
      {addQuery.trim() && isSearchingAddCourses && (
        <p className="page-subtitle">Searching courses...</p>
      )}

      {/* Table */}
      <div className="transcript-table-container">
        <div className="transcript-table-scroll">
          <table className="transcript-table">
            <thead>
              <tr>
                <th className="col-code">Course Code</th>
                <th className="col-title">Course Title</th>
                <th className="col-credits">Credits</th>
                <th className="col-semester">Semester</th>
                <th className="col-action">Action</th>
              </tr>
            </thead>
            <tbody>
              {pagedCourses.map((c) => (
                <tr key={c.id}>
                  <td className="col-code cell-code">{c.courseCode}</td>
                  <td className="col-title">{c.title ?? c.courseCode}</td>
                  <td className="col-credits">{c.credits ?? "—"}</td>
                  <td className="col-semester cell-semester">{c.semester}</td>
                  <td className="col-action">
                    <button
                      className="delete-btn"
                      onClick={() => {
                        void handleDelete(c.id);
                      }}
                      aria-label={`Remove ${c.courseCode}`}
                    >
                      <Trash2 size={16} />
                    </button>
                  </td>
                </tr>
              ))}
              {!loading && pagedCourses.length === 0 && (
                <tr>
                  <td className="col-title" colSpan={5}>
                    No transcript courses yet.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Footer
        <div className="table-footer">
          <span className="footer-text">
            Showing {pagedCourses.length} of {courses.length} courses
          </span>
          <div className="footer-buttons">
            <button
              className="footer-btn"
              disabled={currentPage === 0}
              onClick={() => setPage(currentPage - 1)}
            >
              Previous
            </button>
            <button
              className="footer-btn"
              disabled={currentPage >= totalPages - 1}
              onClick={() => setPage(currentPage + 1)}
            >
              Next
            </button>
          </div>
        </div> */}
      </div>
    </div>
  );
}

function StatCard({
  title,
  value,
  badge,
}: {
  title: string;
  value: number;
  badge: string;
}) {
  return (
    <div className="stat-card">
      <div className="stat-card-header">{title}</div>
      <div className="stat-card-content">
        <span className="stat-value">{value}</span>
        <span className="stat-badge">{badge}</span>
      </div>
    </div>
  );
}
