package com.example.server.imports.parser;

import java.util.List;

import com.example.server.seed.CourseDefinition;

public record ParsedCourseRecord(
    CourseDefinition courseDefinition,
    String prerequisiteText,
    List<String> warnings
) {}
