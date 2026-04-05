import type { Course } from "../types";
import { formatRequirement } from "../utils/formatRequirement";
export function CourseCard({ course }: { course: Course }) {
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
