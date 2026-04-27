package com.example.server.dto;

import com.example.server.model.CourseType;

public record CourseDetailDTO(
    String courseCode,
    String title,
    Integer credits,
    String description,
    RequirementNodeResponse prerequisite,
    String prerequisiteDescription,
    CourseType courseType
) {}
