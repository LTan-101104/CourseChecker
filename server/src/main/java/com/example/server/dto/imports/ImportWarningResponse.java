package com.example.server.dto.imports;

public record ImportWarningResponse(
    String code,
    String detail,
    String rawPrerequisiteText,
    String normalizedPrerequisiteText
) {}
