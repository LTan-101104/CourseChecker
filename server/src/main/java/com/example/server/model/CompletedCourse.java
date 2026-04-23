package com.example.server.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Represents a course completed by a student.
 * Used to check prerequisite eligibility against a student's transcript.
 */
@Entity
@Table(
    name = "completed_course",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_completed_course_user_course_code",
        columnNames = {"user_id", "course_code"}
    )
)
public class CompletedCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "course_code", nullable = false)
    private String courseCode;

    private String grade;

    private String semester;

    public CompletedCourse() {}

    public CompletedCourse(User user, String courseCode, String grade, String semester) {
        this.user = user;
        this.courseCode = courseCode;
        this.grade = grade;
        this.semester = semester;
    }

    // ── Getters & Setters ────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }
}
