import { useState, useMemo } from "react";
import { Bell, BookOpen, CheckCircle2, XCircle, Plus } from "lucide-react";
import { Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { mockTranscripts } from "../data/mockTranscripts";
import { mockCourses } from "../data/mockCourses";
import { evaluateEligibility } from "../data/eligibilityEvaluator";
import type { Course } from "../types";
import "./DashboardPage.css";

const courseCreditsMap = new Map(
  mockCourses.map((c) => [c.courseCode, c.credits]),
);
const courseTitleMap = new Map(mockCourses.map((c) => [c.courseCode, c.title]));

export function DashboardPage() {
  const { studentId } = useAuth();
  const transcript = useMemo(
    () =>
      mockTranscripts[studentId] ?? { studentId, completedCourses: [] },
    [studentId],
  );

  const [query, setQuery] = useState("");
  const [selectedCourse, setSelectedCourse] = useState<Course | null>(null);
  const [showDropdown, setShowDropdown] = useState(false);

  // Stats
  const completedCount = transcript.completedCourses.length;
  const creditsEarned = useMemo(
    () =>
      transcript.completedCourses.reduce(
        (sum, c) => sum + (courseCreditsMap.get(c.courseCode) ?? 0),
        0,
      ),
    [transcript],
  );
  const notCompletedCount = mockCourses.length - completedCount;
  const eligibleCount = useMemo(
    () =>
      mockCourses.filter((c) => {
        if (transcript.completedCourses.some((cc) => cc.courseCode === c.courseCode))
          return false;
        return evaluateEligibility(c, transcript).isEligible;
      }).length,
    [transcript],
  );

  // Eligibility check
  const eligibilityResult = useMemo(
    () =>
      selectedCourse ? evaluateEligibility(selectedCourse, transcript) : null,
    [selectedCourse, transcript],
  );

  // Search suggestions
  const suggestions = useMemo(() => {
    if (!query.trim()) return [];
    const q = query.toLowerCase();
    return mockCourses
      .filter(
        (c) =>
          c.courseCode.toLowerCase().includes(q) ||
          c.title.toLowerCase().includes(q),
      )
      .slice(0, 6);
  }, [query]);

  // Recent transcript (last 5 courses)
  const recentCourses = transcript.completedCourses.slice(-5).reverse();

  // Avatar initials from studentId (use "JD" as fallback)
  const avatarInitials = studentId === "student-001" ? "JD" : studentId.slice(0, 2).toUpperCase();

  function handleSelectCourse(course: Course) {
    setSelectedCourse(course);
    setQuery(`${course.courseCode} — ${course.title}`);
    setShowDropdown(false);
  }

  return (
    <div className="dashboard-page">
      {/* Header */}
      <div className="dashboard-header">
        <div className="dashboard-header-left">
          <h1 className="dashboard-title">Welcome back, John</h1>
          <p className="dashboard-subtitle">
            Here's your course eligibility overview
          </p>
        </div>
        <div className="dashboard-header-right">
          <Bell size={20} color="#737373" />
          <div className="header-avatar">{avatarInitials}</div>
        </div>
      </div>

      {/* Stats Row */}
      <div className="dashboard-stats-row">
        <StatCard
          title="Completed Courses"
          value={completedCount}
          badge={`of ${mockCourses.length} total`}
        />
        <StatCard
          title="Eligible Courses"
          value={eligibleCount}
          badge={`of ${notCompletedCount} remaining`}
        />
        <StatCard
          title="Credits Earned"
          value={creditsEarned}
          badge="of 120 req."
        />
      </div>

      {/* Bottom Section */}
      <div className="dashboard-bottom">
        {/* Eligibility Check Card */}
        <div className="dashboard-card">
          <div className="card-header">
            <h2 className="card-title">Quick Eligibility Check</h2>
            <p className="card-desc">
              Select a target course to check if you meet the prerequisites
            </p>
          </div>

          <div className="card-body">
            {/* Search */}
            <div className="search-field-wrapper">
              <label className="search-label">Target Course</label>
              <div className="search-input-container">
                <input
                  className="search-input"
                  type="text"
                  placeholder="Search courses (e.g. COMPSCI 311)"
                  value={query}
                  onChange={(e) => {
                    setQuery(e.target.value);
                    setSelectedCourse(null);
                    setShowDropdown(true);
                  }}
                  onFocus={() => setShowDropdown(true)}
                  onBlur={() => setTimeout(() => setShowDropdown(false), 150)}
                />
                {showDropdown && suggestions.length > 0 && (
                  <ul className="search-dropdown">
                    {suggestions.map((c) => (
                      <li
                        key={c.courseCode}
                        className="search-dropdown-item"
                        onMouseDown={() => handleSelectCourse(c)}
                      >
                        <span className="dropdown-code">{c.courseCode}</span>
                        <span className="dropdown-title">{c.title}</span>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            </div>

            {/* Result Preview */}
            {selectedCourse && eligibilityResult && (
              <div
                className={`result-preview ${eligibilityResult.isEligible ? "eligible" : "not-eligible"}`}
              >
                <div className="result-row">
                  {eligibilityResult.isEligible ? (
                    <CheckCircle2 size={18} className="result-icon eligible-icon" />
                  ) : (
                    <XCircle size={18} className="result-icon not-eligible-icon" />
                  )}
                  <span className="result-status">
                    {eligibilityResult.isEligible
                      ? "You are eligible!"
                      : "Not yet eligible"}
                  </span>
                </div>
                <p className="result-course-name">
                  {selectedCourse.courseCode} — {selectedCourse.title}
                </p>
                {!eligibilityResult.isEligible &&
                  eligibilityResult.missingRequirements.length > 0 && (
                    <p className="result-missing">
                      Missing:{" "}
                      {eligibilityResult.missingRequirements
                        .flatMap((r) =>
                          r.type === "COURSE" ? [r.requiredCourseCode] : [],
                        )
                        .join(", ")}
                    </p>
                  )}
                {eligibilityResult.courseHasNoRequisiteDescription && (
                  <p className="result-no-prereq">
                    This course has no prerequisites.
                  </p>
                )}
              </div>
            )}
          </div>

          <div className="card-footer">
            <Link to="/eligibility" className="check-btn">
              Check Eligibility
            </Link>
          </div>
        </div>

        {/* Recent Transcript Card */}
        <div className="dashboard-card">
          <div className="card-header transcript-card-header">
            <h2 className="card-title">Recent Transcript</h2>
            <Link to="/transcript" className="add-course-btn" title="Add Course">
              <Plus size={16} />
              Add Course
            </Link>
          </div>

          <div className="card-body">
            {recentCourses.length === 0 ? (
              <p className="empty-transcript">No courses recorded yet.</p>
            ) : (
              <ul className="course-list">
                {recentCourses.map((cc) => (
                  <li key={cc.id} className="course-list-item">
                    <div className="course-list-left">
                      <BookOpen size={14} className="course-icon" />
                      <div className="course-info">
                        <span className="course-code">{cc.courseCode}</span>
                        <span className="course-title-small">
                          {courseTitleMap.get(cc.courseCode) ?? ""}
                        </span>
                      </div>
                    </div>
                    <div className="course-list-right">
                      <span className="course-grade">{cc.grade}</span>
                      <span className="course-semester">{cc.semester}</span>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </div>

          <div className="card-footer">
            <Link to="/transcript" className="view-all-btn">
              View Full Transcript
            </Link>
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
      <div className="stat-card-body">
        <span className="stat-value">{value}</span>
        <span className="stat-badge">{badge}</span>
      </div>
    </div>
  );
}
