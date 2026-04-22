package com.example.server.dto;

public record CourseDetailDTO(
    String courseCode,
    String title,
    Integer credits,
    String description,
    String prerequisiteDescription
) {}
