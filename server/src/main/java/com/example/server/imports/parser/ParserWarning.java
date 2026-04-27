package com.example.server.imports.parser;

public record ParserWarning(
    String code,
    String message,
    String courseCode,
    String rawPrerequisiteText,
    String normalizedPrerequisiteText
) {}
