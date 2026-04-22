package com.example.server.dto;

public record CourseSummaryDTO(
    String courseCode,
    String title,
    Integer credits
) {}
