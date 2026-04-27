package com.example.server.imports.parser;

import java.util.List;

import com.example.server.seed.CourseDefinition;

public record ParsedCourseRecord(
    CourseDefinition courseDefinition,
    String prerequisiteText,
    String normalizedPrerequisiteText,
    PrerequisiteParseOutcome prerequisiteParseOutcome,
    List<ParserWarning> warnings
) {}
