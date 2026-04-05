import { useState, useMemo } from "react";
import { Bell, Plus, Trash2, ChevronsUpDown } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { mockTranscripts } from "../data/mockTranscripts";
import { mockCourses } from "../data/mockCourses";
import type { CompletedCourse } from "../types";
import "./TranscriptPage.css";
//TODO: need to replace many of these when having proper backend API
const PAGE_SIZE = 9;

const courseTitleMap = new Map(mockCourses.map((c) => [c.courseCode, c.title]));
const courseCreditsMap = new Map(
  mockCourses.map((c) => [c.courseCode, c.credits]),
);

export function TranscriptPage() {
  const { studentId } = useAuth();
  const transcript = mockTranscripts[studentId];

  const [courses, setCourses] = useState<CompletedCourse[]>(
    transcript?.completedCourses ?? [],
  );
  const [page, setPage] = useState(0); // page state for page navigation
  const [selectedCourse, setSelectedCourse] = useState("");

  // Stats
  const totalCourses = courses.length;
  const totalCredits = courses.reduce(
    (sum, c) => sum + (courseCreditsMap.get(c.courseCode) ?? 0),
    0,
  );
  const csCourses = courses.filter((c) =>
    c.courseCode.startsWith("COMPSCI"),
  ).length;
  const mathCourses = courses.filter((c) =>
    c.courseCode.startsWith("MATH"),
  ).length;

  // Pagination
  const totalPages = Math.ceil(courses.length / PAGE_SIZE);
  const pagedCourses = courses.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE);

  // Courses available to add (not already in transcript)
  const addableCourses = useMemo(
    () =>
      mockCourses.filter(
        (c) => !courses.some((cc) => cc.courseCode === c.courseCode),
      ),
    [courses],
  );

  function handleAdd() {
    if (!selectedCourse) return;
    const newCourse: CompletedCourse = {
      id: Date.now(),
      studentId,
      courseCode: selectedCourse,
      grade: "—",
      semester: "Spring 2026",
    };
    setCourses([...courses, newCourse]);
    setSelectedCourse("");
  }

  function handleDelete(id: number) {
    const updated = courses.filter((c) => c.id !== id);
    setCourses(updated);
    // If current page is now empty, go back
    const newTotalPages = Math.ceil(updated.length / PAGE_SIZE);
    if (page >= newTotalPages && page > 0) setPage(newTotalPages - 1);
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
          <div className="header-avatar">JD</div>
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
          <select
            className="add-course-select"
            value={selectedCourse}
            onChange={(e) => setSelectedCourse(e.target.value)}
          >
            <option value="">
              Search and add a course to your transcript...
            </option>
            {addableCourses.map((c) => (
              <option key={c.courseCode} value={c.courseCode}>
                {c.courseCode} — {c.title}
              </option>
            ))}
          </select>
          <ChevronsUpDown size={16} className="select-icon" />
        </div>
        <button
          className="add-btn"
          onClick={handleAdd}
          disabled={!selectedCourse}
        >
          <Plus size={16} />
          Add Course
        </button>
      </div>

      {/* Table */}
      <div className="transcript-table-container">
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
                <td className="col-title">
                  {courseTitleMap.get(c.courseCode) ?? c.courseCode}
                </td>
                <td className="col-credits">
                  {courseCreditsMap.get(c.courseCode) ?? "—"}
                </td>
                <td className="col-semester cell-semester">{c.semester}</td>
                <td className="col-action">
                  <button
                    className="delete-btn"
                    onClick={() => handleDelete(c.id)}
                    aria-label={`Remove ${c.courseCode}`}
                  >
                    <Trash2 size={16} />
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {/* Footer */}
        <div className="table-footer">
          <span className="footer-text">
            Showing {pagedCourses.length} of {courses.length} courses
          </span>
          <div className="footer-buttons">
            <button
              className="footer-btn"
              disabled={page === 0}
              onClick={() => setPage(page - 1)}
            >
              Previous
            </button>
            <button
              className="footer-btn"
              disabled={page >= totalPages - 1}
              onClick={() => setPage(page + 1)}
            >
              Next
            </button>
          </div>
        </div>
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
