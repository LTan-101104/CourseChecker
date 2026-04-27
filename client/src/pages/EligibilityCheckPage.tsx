import { useMemo, useState } from "react";
import {
  Bell,
  CheckCircle2,
  XCircle,
  CircleAlert,
  Circle,
} from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { useCompletedCourses } from "../context/CompletedCoursesContext";
import { fetchCourseDetail, type CourseSummaryDTO } from "../api/courses";
import { ApiError } from "../api/client";
import { useCourseSearch } from "../hooks/useCourseSearch";
import { mockCourses } from "../data/mockCourses";
import { mapCourseDetail } from "../utils/courseMappers";
import type { Course, Requirement, Transcript } from "../types";
import "./EligibilityCheckPage.css";

const courseTitleMap = new Map(mockCourses.map((c) => [c.courseCode, c.title]));

// ── Helpers ──────────────────────────────────────────────

function isReqMet(req: Requirement, completed: Set<string>): boolean {
  switch (req.type) {
    case "COURSE":
      return completed.has(req.requiredCourseCode);
    case "AND":
      return req.children.every((c) => isReqMet(c, completed));
    case "OR":
      return req.children.some((c) => isReqMet(c, completed));
  }
}

function formatReqExpression(req: Requirement): string {
  switch (req.type) {
    case "COURSE":
      return req.requiredCourseCode;
    case "AND":
      return req.children.map(formatReqExpression).join(" AND ");
    case "OR":
      return `( ${req.children.map(formatReqExpression).join(" OR ")} )`;
  }
}

/** Returns the top-level requirement nodes to render as individual cards. */
function getTopLevelReqs(prereq: Requirement): Requirement[] {
  if (prereq.type === "AND") return prereq.children;
  return [prereq];
}

// ── Sub-components ────────────────────────────────────────

function CourseReqCard({
  req,
  completed,
  index,
}: {
  req: Extract<Requirement, { type: "COURSE" }>;
  completed: Set<string>;
  index: number;
}) {
  const met = completed.has(req.requiredCourseCode);
  const title = courseTitleMap.get(req.requiredCourseCode);
  return (
    <div className={`req-card ${met ? "req-met" : "req-missing"}`}>
      <div className="req-card-header">
        {met ? (
          <CheckCircle2 size={18} className="req-icon met" />
        ) : (
          <XCircle size={18} className="req-icon missing" />
        )}
        <span className="req-label">{req.requiredCourseCode}</span>
        <span className={`req-badge ${met ? "badge-met" : "badge-missing"}`}>
          {met ? "Completed" : "Missing"}
        </span>
      </div>
      {title && (
        <p className="req-desc">
          {title} —{" "}
          {met
            ? "You have completed this requirement."
            : "You have not completed this requirement."}
        </p>
      )}
      {index === 0 && !title && (
        <p className="req-desc">
          {met
            ? "You have completed this requirement."
            : "You have not completed this requirement."}
        </p>
      )}
    </div>
  );
}

function OrReqCard({
  req,
  completed,
}: {
  req: Extract<Requirement, { type: "OR" }>;
  completed: Set<string>;
}) {
  const met = req.children.some((c) => isReqMet(c, completed));

  return (
    <div className={`req-card ${met ? "req-met" : "req-missing"}`}>
      <div className="req-card-header">
        {met ? (
          <CheckCircle2 size={18} className="req-icon met" />
        ) : (
          <CircleAlert size={18} className="req-icon missing" />
        )}
        <span className={`req-label ${met ? "" : "req-label-missing"}`}>
          One of the following required
        </span>
        <span className={`req-badge ${met ? "badge-met" : "badge-missing"}`}>
          {met ? "Completed" : "Missing"}
        </span>
      </div>

      {/* Paths */}
      {req.children.map((child, i) => {
        if (child.type !== "COURSE") return null;
        const pathMet = completed.has(child.requiredCourseCode);
        const label = String.fromCharCode(65 + i); // A, B, C…
        const title = courseTitleMap.get(child.requiredCourseCode);
        return (
          <div key={child.requiredCourseCode}>
            {i > 0 && (
              <div className="or-divider">
                <div className="or-line" />
                <span className="or-label">OR</span>
                <div className="or-line" />
              </div>
            )}
            <div className="path-card">
              <div className="path-header">
                <span className="path-label">Option {label}</span>
                <span
                  className={`req-badge ${pathMet ? "badge-met" : "badge-outline"}`}
                >
                  {pathMet ? "Completed" : "Not completed"}
                </span>
              </div>
              <div className="path-course">
                <Circle size={14} className="path-icon" />
                <span className="path-course-text">
                  {child.requiredCourseCode}
                  {title ? ` — ${title}` : ""}
                </span>
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
}

// ── Main Page ─────────────────────────────────────────────

export function EligibilityCheckPage() {
  const { user } = useAuth();
  const { courses } = useCompletedCourses();
  const transcript = useMemo<Transcript>(
    () => ({
      studentId: user?.studentId ?? "guest",
      completedCourses: courses,
    }),
    [courses, user?.studentId],
  );
  const completed = useMemo(
    () => new Set(transcript.completedCourses.map((c) => c.courseCode)),
    [transcript],
  );

  const [query, setQuery] = useState("");
  const [selectedCourse, setSelectedCourse] = useState<Course | null>(null);
  const [hasChecked, setHasChecked] = useState(false);
  const [showDropdown, setShowDropdown] = useState(false);
  const [isSelectingCourse, setIsSelectingCourse] = useState(false);
  const [selectionError, setSelectionError] = useState<string | null>(null);
  const { results: searchResults, error: searchError } = useCourseSearch(query);

  const avatarInitials = user?.displayName
    ? user.displayName
        .split(" ")
        .map((part) => part.charAt(0))
        .join("")
        .slice(0, 2)
        .toUpperCase()
    : "G";

  const suggestions = useMemo(() => searchResults.slice(0, 8), [searchResults]);

  async function handleSelect(course: CourseSummaryDTO) {
    setIsSelectingCourse(true);
    setSelectionError(null);
    try {
      const detail = await fetchCourseDetail(course.courseCode);
      setSelectedCourse(mapCourseDetail(detail));
      setQuery(`${course.courseCode} — ${course.title}`);
      setShowDropdown(false);
      setHasChecked(false);
    } catch (err) {
      if (err instanceof ApiError) {
        setSelectionError(err.message);
      } else {
        setSelectionError("Unable to load course details.");
      }
      setSelectedCourse(null);
    } finally {
      setIsSelectingCourse(false);
    }
  }

  function handleCheck() {
    if (selectedCourse) setHasChecked(true);
  }

  const isEligible = useMemo(() => {
    if (!selectedCourse || !selectedCourse.prerequisite) return true;
    return isReqMet(selectedCourse.prerequisite, completed);
  }, [selectedCourse, completed]);

  const prereqExpression = useMemo(() => {
    if (!selectedCourse?.prerequisite) return null;
    return formatReqExpression(selectedCourse.prerequisite);
  }, [selectedCourse]);

  const topLevelReqs = useMemo(() => {
    if (!selectedCourse?.prerequisite) return [];
    return getTopLevelReqs(selectedCourse.prerequisite);
  }, [selectedCourse]);

  return (
    <div className="eligibility-page">
      {/* Header */}
      <div className="elig-header">
        <div className="elig-header-left">
          <h1 className="elig-title">Eligibility Check</h1>
          <p className="elig-subtitle">
            Check if you meet the prerequisites for any CS course
          </p>
        </div>
        <div className="elig-header-right">
          <Bell size={20} color="#737373" />
          <div className="header-avatar">{avatarInitials}</div>
        </div>
      </div>

      {/* Selector Card */}
      <div className="selector-card">
        <div className="sel-row">
          {/* Search combobox */}
          <div className="sel-field-wrapper">
            <label className="sel-label">Target Course</label>
            <div className="sel-input-container">
              <input
                className="sel-input"
                type="text"
                placeholder="Search for a course (e.g. COMPSCI 311)"
                value={query}
                onChange={(e) => {
                  setQuery(e.target.value);
                  setSelectedCourse(null);
                  setHasChecked(false);
                  setSelectionError(null);
                  setShowDropdown(true);
                }}
                onFocus={() => setShowDropdown(true)}
                onBlur={() => setTimeout(() => setShowDropdown(false), 150)}
              />
              {showDropdown && suggestions.length > 0 && (
                <ul className="sel-dropdown">
                  {suggestions.map((c) => (
                    <li
                      key={c.courseCode}
                      className="sel-dropdown-item"
                      onMouseDown={() => {
                        void handleSelect(c);
                      }}
                    >
                      <span className="dropdown-code">{c.courseCode}</span>
                      <span className="dropdown-title">{c.title}</span>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>

          {/* Check button */}
          <button
            className="check-btn"
            onClick={handleCheck}
            disabled={!selectedCourse || isSelectingCourse}
          >
            Check Eligibility
          </button>
        </div>
        {query.trim() && searchError && (
          <p className="elig-subtitle">{searchError}</p>
        )}
        {selectionError && <p className="elig-subtitle">{selectionError}</p>}
      </div>

      {/* Result Section */}
      {hasChecked && selectedCourse && (
        <div className="result-section">
          {/* Left Column — Result Card */}
          <div className="left-col">
            <div className="result-card">
              {/* Banner */}
              <div
                className={`result-banner ${isEligible ? "banner-eligible" : "banner-ineligible"}`}
              >
                {isEligible ? (
                  <CheckCircle2 size={20} className="banner-icon eligible" />
                ) : (
                  <XCircle size={20} className="banner-icon ineligible" />
                )}
                <div className="banner-text">
                  <span className="banner-title">
                    {isEligible ? "Eligible" : "Not Eligible"}
                  </span>
                  <span className="banner-desc">
                    {isEligible
                      ? "You meet all prerequisites for this course"
                      : "You do not meet all prerequisites for this course"}
                  </span>
                </div>
              </div>

              {/* Course Info */}
              <div className="course-info">
                <div className="ci-header">
                  <span className="ci-code">{selectedCourse.courseCode}</span>
                  <span className="ci-badge">
                    {selectedCourse.credits || "?"} credits
                  </span>
                </div>
                <p className="ci-title">{selectedCourse.title}</p>
                {selectedCourse.description && (
                  <p className="ci-desc">{selectedCourse.description}</p>
                )}
              </div>

              <div className="card-divider" />

              {/* Prereq Summary */}
              {selectedCourse.prerequisite ? (
                <div className="prereq-summary">
                  <span className="prereq-label">PREREQUISITE STRUCTURE</span>
                  <div className="prereq-expr">{prereqExpression}</div>
                </div>
              ) : (
                <div className="prereq-summary">
                  <span className="prereq-label">PREREQUISITE STRUCTURE</span>
                  <div className="prereq-expr">No prerequisites required</div>
                </div>
              )}
            </div>
          </div>

          {/* Right Column — Prereq Breakdown */}
          <div className="right-col">
            <h2 className="right-col-title">
              {!selectedCourse.prerequisite
                ? "No Prerequisites"
                : isEligible
                  ? "All Prerequisites Met"
                  : "Missing Prerequisites"}
            </h2>

            {selectedCourse.prerequisite === null ? (
              <div className="no-prereq-card">
                <CheckCircle2 size={18} className="req-icon met" />
                <p className="no-prereq-text">
                  This course has no prerequisites — you're always eligible to
                  enroll.
                </p>
              </div>
            ) : (
              topLevelReqs.map((req, i) =>
                req.type === "COURSE" ? (
                  <CourseReqCard
                    key={req.requiredCourseCode}
                    req={req}
                    completed={completed}
                    index={i}
                  />
                ) : req.type === "OR" ? (
                  <OrReqCard key={i} req={req} completed={completed} />
                ) : (
                  // Nested AND inside top-level AND is rendered as flat cards
                  req.children.map((child, j) =>
                    child.type === "COURSE" ? (
                      <CourseReqCard
                        key={`${i}-${j}-${child.requiredCourseCode}`}
                        req={child}
                        completed={completed}
                        index={j}
                      />
                    ) : child.type === "OR" ? (
                      <OrReqCard
                        key={`${i}-${j}`}
                        req={child}
                        completed={completed}
                      />
                    ) : null,
                  )
                ),
              )
            )}
          </div>
        </div>
      )}
    </div>
  );
}
