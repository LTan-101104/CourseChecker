package com.example.server.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Represents a course completed by a student.
 * Used to check prerequisite eligibility against a student's transcript.
 */
@Entity
@Table(name = "completed_course")
public class CompletedCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private String studentId;

    @Column(name = "course_code", nullable = false)
    private String courseCode;

    private String grade;

    private String semester;

    public CompletedCourse() {}

    public CompletedCourse(String studentId, String courseCode, String grade, String semester) {
        this.studentId = studentId;
        this.courseCode = courseCode;
        this.grade = grade;
        this.semester = semester;
    }

    // ── Getters & Setters ────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }
}
