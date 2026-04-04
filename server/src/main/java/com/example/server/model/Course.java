package com.example.server.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Represents a course offered at UMass Amherst.
 * Each course may have a root {@link Requirement} that models its prerequisite tree.
 */
@Entity
@Table(name = "course")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_code", nullable = false, unique = true)
    private String courseCode;

    @Column(nullable = false)
    private String title;

    private Integer credits;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * The root of the prerequisite tree (AND/OR/COURSE node).
     * Null if the course has no prerequisites.
     */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "prerequisite_id")
    private Requirement prerequisite;

    public Course() {}

    public Course(String courseCode, String title, Integer credits, String description) {
        this.courseCode = courseCode;
        this.title = title;
        this.credits = credits;
        this.description = description;
    }

    // ── Getters & Setters ────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Integer getCredits() { return credits; }
    public void setCredits(Integer credits) { this.credits = credits; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Requirement getPrerequisite() { return prerequisite; }
    public void setPrerequisite(Requirement prerequisite) { this.prerequisite = prerequisite; }
}
