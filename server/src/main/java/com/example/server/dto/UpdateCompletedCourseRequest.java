package com.example.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCompletedCourseRequest(
    @NotBlank @Size(max = 50) String courseCode,
    @Size(max = 10) String grade,
    @Size(max = 50) String semester
) {}
