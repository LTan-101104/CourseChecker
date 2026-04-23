package com.example.server.dto;

public record CourseResponse(
    String courseCode,
    String title,
    Integer credits,
    String description,
    RequirementNodeResponse prerequisite,
    String prerequisiteDescription
) {}
