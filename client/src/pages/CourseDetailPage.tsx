import { useEffect, useMemo, useState } from "react";
import { useParams, useNavigate, Link } from "react-router-dom";
import {
  ChevronRight,
  ArrowLeft,
  Bell,
  ShieldCheck,
  Plus,
  ArrowRight,
} from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { fetchCourseDetail } from "../api/courses";
import { ApiError } from "../api/client";
import { mapCourseDetail } from "../utils/courseMappers";
import { formatRequirement } from "../utils/formatRequirement";
import type { Course, Requirement } from "../types";
import "./CourseDetailPage.css";

/** Count total leaf course requirements in a prerequisite tree */
function countPrereqs(req: Requirement): number {
  switch (req.type) {
    case "COURSE":
      return 1;
    case "AND":
    case "OR":
      return req.children.reduce((sum, child) => sum + countPrereqs(child), 0);
  }
}

/** Derive the course level from its code (e.g. "COMPSCI 311" → "300-Level") */
function getCourseLevel(courseCode: string): string {
  const num = courseCode.match(/\d+/)?.[0];
  if (!num) return "Unknown";
  const hundreds = Math.floor(parseInt(num) / 100) * 100;
  return `${hundreds}-Level`;
}

type PrereqItem =
  | {
      kind: "card";
      code: string;
      title: string;
      badge: string;
      badgeType: "required" | "option";
    }
  | { kind: "divider"; text: string };

function buildPrereqItems(req: Requirement): PrereqItem[] {
  switch (req.type) {
    case "COURSE": {
      return [
        {
          kind: "card",
          code: req.requiredCourseCode,
          title: req.requiredCourseCode,
          badge: "Required",
          badgeType: "required",
        },
      ];
    }
    case "AND": {
      const items: PrereqItem[] = [];
      req.children.forEach((child, i) => {
        if (i > 0) {
          const text = child.type === "OR" ? "AND one of the following" : "AND";
          items.push({ kind: "divider", text });
        }
        items.push(...buildPrereqItems(child));
      });
      return items;
    }
    case "OR": {
      const items: PrereqItem[] = [];
      req.children.forEach((child, i) => {
        if (i > 0) {
          items.push({ kind: "divider", text: "OR" });
        }
        const label = `Option ${String.fromCharCode(65 + i)}`;
        if (child.type === "COURSE") {
          items.push({
            kind: "card",
            code: child.requiredCourseCode,
            title: child.requiredCourseCode,
            badge: label,
            badgeType: "option",
          });
        } else {
          items.push(...buildPrereqItems(child));
        }
      });
      return items;
    }
  }
}

export function CourseDetailPage() {
  const { user } = useAuth();
  const { courseCode } = useParams<{ courseCode: string }>();
  const navigate = useNavigate();
  const decodedCode = useMemo(() => decodeURIComponent(courseCode ?? ""), [courseCode]);

  const [course, setCourse] = useState<Course | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!decodedCode) {
      setCourse(null);
      setIsLoading(false);
      setError("Course code is missing.");
      return;
    }

    let isCancelled = false;
    void (async () => {
      setIsLoading(true);
      setError(null);
      try {
        const detail = await fetchCourseDetail(decodedCode);
        if (!isCancelled) {
          setCourse(mapCourseDetail(detail));
        }
      } catch (err) {
        if (isCancelled) {
          return;
        }
        if (err instanceof ApiError) {
          setError(err.message);
        } else {
          setError("Failed to load course details.");
        }
        setCourse(null);
      } finally {
        if (!isCancelled) {
          setIsLoading(false);
        }
      }
    })();

    return () => {
      isCancelled = true;
    };
  }, [decodedCode]);

  if (isLoading) {
    return (
      <div className="detail-page">
        <p>Loading course details...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="detail-page">
        <p>{error}</p>
      </div>
    );
  }

  if (!course) {
    return (
      <div className="detail-page">
        <p>Course not found.</p>
      </div>
    );
  }

  const prereqItems = course.prerequisite
    ? buildPrereqItems(course.prerequisite)
    : [];
  const prereqCount = course.prerequisite
    ? countPrereqs(course.prerequisite)
    : 0;

  return (
    <div className="detail-page">
      {/* Top Bar */}
      <div className="detail-topbar">
        <div className="breadcrumb">
          <Link to="/search" className="breadcrumb-link">
            Course Search
          </Link>
          <span className="breadcrumb-sep">
            <ChevronRight size={14} />
          </span>
          <span className="breadcrumb-current">{course.courseCode}</span>
        </div>
        <div className="topbar-right">
          <Bell size={20} color="#737373" />
          <div className="header-avatar">
            {user?.displayName?.charAt(0).toUpperCase() ?? "G"}
          </div>
        </div>
      </div>

      {/* Content */}
      <div className="detail-content">
        {/* Left Column */}
        <div className="detail-left">
          <div className="course-header">
            <div className="title-row">
              <button className="back-btn" onClick={() => navigate("/search")}>
                <ArrowLeft size={20} color="#0a0a0a" />
              </button>
              <h1 className="detail-course-code">{course.courseCode}</h1>
            </div>
            <p className="detail-course-title">{course.title}</p>
          </div>

          <div className="detail-divider" />

          {/* Description */}
          <div className="detail-section">
            <span className="detail-section-label">DESCRIPTION</span>
            <p className="detail-description">{course.description}</p>
          </div>

          <div className="detail-divider" />

          {/* Prerequisites */}
          <div className="prereq-section">
            <span className="detail-section-label">PREREQUISITES</span>

            {course.prerequisite ? (
              <>
                <div className="prereq-expression">
                  {formatRequirement(course.prerequisite)}
                </div>
                <div className="prereq-cards">
                  {prereqItems.map((item, i) =>
                    item.kind === "divider" ? (
                      <div key={i} className="prereq-divider">
                        <div className="prereq-divider-line" />
                        <span className="prereq-divider-text">{item.text}</span>
                        <div className="prereq-divider-line" />
                      </div>
                    ) : (
                      <div
                        key={`${item.code}-${i}`}
                        className="prereq-card prereq-card-clickable"
                        onClick={() =>
                          navigate(
                            `/course/${encodeURIComponent(item.code)}`,
                          )
                        }
                      >
                        <div className="prereq-card-left">
                          <span className="prereq-card-code">{item.code}</span>
                          <span className="prereq-card-title">
                            {item.title}
                          </span>
                        </div>
                        <div className="prereq-card-right">
                          <span
                            className={
                              item.badgeType === "required"
                                ? "prereq-badge-required"
                                : "prereq-badge-option"
                            }
                          >
                            {item.badge}
                          </span>
                          <ArrowRight size={14} color="#737373" />
                        </div>
                      </div>
                    ),
                  )}
                </div>
              </>
            ) : (
              <p className="no-prereq-text">
                This course has no prerequisites.
              </p>
            )}
          </div>
        </div>

        {/* Right Column */}
        <div className="detail-right">
          {/* Quick Actions */}
          <div className="action-card">
            <span className="action-card-title">Quick Actions</span>
            <button className="action-btn-primary">
              <ShieldCheck size={20} />
              Check My Eligibility
            </button>
            <button className="action-btn-secondary">
              <Plus size={20} />
              Add to Transcript
            </button>
          </div>

          {/* Course Information */}
          <div className="info-card">
            <span className="info-card-title">Course Information</span>
            <div className="info-row">
              <span className="info-label">Credits</span>
              <span className="info-value">{course.credits || "—"}</span>
            </div>
            <div className="info-divider" />
            <div className="info-row">
              <span className="info-label">Level</span>
              <span className="info-value">
                {getCourseLevel(course.courseCode)}
              </span>
            </div>
            <div className="info-divider" />
            <div className="info-row">
              <span className="info-label">Prerequisites</span>
              <span className="info-value">
                {prereqCount > 0 ? `${prereqCount} required` : "None"}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
