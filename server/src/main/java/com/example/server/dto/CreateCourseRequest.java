package com.example.server.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateCourseRequest(
    @NotBlank @Size(max = 50) String courseCode,
    @NotBlank @Size(max = 255) String title,
    @PositiveOrZero Integer credits,
    String description,
    @Valid RequirementNodeRequest prerequisite
) {}
