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
import { mockCourses } from "../data/mockCourses";
import { formatRequirement } from "../utils/formatRequirement";
import type { Requirement } from "../types";
import "./CourseDetailPage.css";

/** Map course codes to course objects for prereq card lookups */
const courseMap = new Map(mockCourses.map((c) => [c.courseCode, c]));

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

/**
 * Build the visual prereq card list from a requirement tree.
 * Returns an array of render items: either a prereq card or a divider.
 */
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
      const c = courseMap.get(req.requiredCourseCode);
      return [
        {
          kind: "card",
          code: req.requiredCourseCode,
          title: c?.title ?? req.requiredCourseCode,
          badge: "Required",
          badgeType: "required",
        },
      ];
    }
    case "AND": {
      const items: PrereqItem[] = [];
      req.children.forEach((child, i) => {
        if (i > 0) {
          // If next child is an OR group, show "AND one of the following"
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
        // Label options A, B, C...
        const label = `Option ${String.fromCharCode(65 + i)}`;
        if (child.type === "COURSE") {
          const c = courseMap.get(child.requiredCourseCode);
          items.push({
            kind: "card",
            code: child.requiredCourseCode,
            title: c?.title ?? child.requiredCourseCode,
            badge: label,
            badgeType: "option",
          });
        } else {
          // Nested composite inside OR — flatten
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

  const decodedCode = decodeURIComponent(courseCode ?? "");
  const course = mockCourses.find((c) => c.courseCode === decodedCode);

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
                        key={i}
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
              <span className="info-value">{course.credits}</span>
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
