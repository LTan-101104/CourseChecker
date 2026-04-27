package com.example.server.dto;

public record CompletedCourseResponse(
    Long id,
    String courseCode,
    String grade,
    String semester,
    String title,
    Integer credits
) {}
