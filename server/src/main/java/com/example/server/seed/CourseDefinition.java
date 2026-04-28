package com.example.server.seed;

import com.example.server.model.CourseType;

/**
 * Simple DTO representing a course definition for seed data.
 * Decoupled from the JPA {@link com.example.server.model.Course} entity
 * so that data providers (mock or web crawler) don't depend on JPA.
 */
public class CourseDefinition {

    private final String courseCode;
    private final String title;
    private final Integer credits;
    private final String description;
    private final RequirementDefinition prerequisite;
    private final CourseType courseType;

    public CourseDefinition(String courseCode, String title, Integer credits,
                            String description, RequirementDefinition prerequisite,
                            CourseType courseType) {
        this.courseCode = courseCode;
        this.title = title;
        this.credits = credits;
        this.description = description;
        this.prerequisite = prerequisite;
        this.courseType = courseType;
    }

    public String getCourseCode() { return courseCode; }
    public String getTitle() { return title; }
    public Integer getCredits() { return credits; }
    public String getDescription() { return description; }
    public RequirementDefinition getPrerequisite() { return prerequisite; }
    public CourseType getCourseType() { return courseType; }
}
