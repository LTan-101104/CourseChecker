package com.example.server.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "import_course_result")
public class ImportCourseResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private ImportJob job;

    @Column(name = "course_code")
    private String courseCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImportCourseAction action;

    @Column(columnDefinition = "TEXT")
    private String title;

    @Column(name = "description_excerpt", columnDefinition = "TEXT")
    private String descriptionExcerpt;

    @Column(name = "prerequisite_text", columnDefinition = "TEXT")
    private String prerequisiteText;

    @Column(name = "warning_message", columnDefinition = "TEXT")
    private String warningMessage;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ImportJob getJob() { return job; }
    public void setJob(ImportJob job) { this.job = job; }
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
    public ImportCourseAction getAction() { return action; }
    public void setAction(ImportCourseAction action) { this.action = action; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescriptionExcerpt() { return descriptionExcerpt; }
    public void setDescriptionExcerpt(String descriptionExcerpt) { this.descriptionExcerpt = descriptionExcerpt; }
    public String getPrerequisiteText() { return prerequisiteText; }
    public void setPrerequisiteText(String prerequisiteText) { this.prerequisiteText = prerequisiteText; }
    public String getWarningMessage() { return warningMessage; }
    public void setWarningMessage(String warningMessage) { this.warningMessage = warningMessage; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
