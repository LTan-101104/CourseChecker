package com.example.server.dto;

import com.example.server.model.CourseType;

public record CourseSummaryDTO(
    String courseCode,
    String title,
    Integer credits,
    CourseType courseType
) {}
