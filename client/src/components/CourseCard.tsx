import { useNavigate } from "react-router-dom";
import type { CourseSummaryDTO } from "../api/courses";

export function CourseCard({ course }: { course: CourseSummaryDTO }) {
  const navigate = useNavigate();

  return (
    <div className="course-card">
      <div className="card-top">
        <span className="card-code">{course.courseCode}</span>
        <span className="card-credits">{course.credits ?? "—"} cr</span>
      </div>
      <div className="card-title">{course.title}</div>
      <div className="card-prereq">
        <span className="prereq-label">Prerequisites</span>
        <span className="prereq-value">View details for prerequisite tree</span>
      </div>
      <div className="card-actions">
        <button
          className="view-details-btn"
          onClick={() =>
            navigate(`/course/${encodeURIComponent(course.courseCode)}`)
          }
        >
          View Details
        </button>
      </div>
    </div>
  );
}
