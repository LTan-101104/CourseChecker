package com.example.server.dto.imports;

public record ImportCourseResultResponse(
    String courseCode,
    String action,
    String title,
    String descriptionExcerpt,
    String prerequisiteText,
    String warningMessage,
    ImportWarningResponse warning,
    String errorMessage
) {}
