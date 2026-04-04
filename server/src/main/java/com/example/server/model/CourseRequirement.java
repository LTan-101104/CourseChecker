package com.example.server.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Leaf node in the Composite Pattern prerequisite tree.
 * Represents a requirement for a single specific course.
 *
 * Example: "COMPSCI 187" is a CourseRequirement with requiredCourseCode = "COMPSCI 187".
 */
@Entity
@DiscriminatorValue("COURSE")
public class CourseRequirement extends Requirement {

    @Column(name = "required_course_code")
    private String requiredCourseCode;

    public CourseRequirement() {}

    public CourseRequirement(String requiredCourseCode) {
        this.requiredCourseCode = requiredCourseCode;
    }

    public String getRequiredCourseCode() { return requiredCourseCode; }
    public void setRequiredCourseCode(String requiredCourseCode) { this.requiredCourseCode = requiredCourseCode; }
}
