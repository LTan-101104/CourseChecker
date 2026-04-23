package com.example.server.imports.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.example.server.seed.CourseDefinition;
import com.example.server.seed.RequirementDefinition;

@Component
public class PdfCourseCatalogParser {

    private static final Pattern HEADER_PATTERN = Pattern.compile(
        "^(?<code>[A-Z]{2,}\\s*\\d{3}[A-Z]?)\\s*[-:]?\\s*(?<title>.+?)\\s*\\((?<credits>\\d+)\\s*CREDITS?\\)\\s*$"
    );

    private final RequirementExpressionParser requirementExpressionParser;

    public PdfCourseCatalogParser(RequirementExpressionParser requirementExpressionParser) {
        this.requirementExpressionParser = requirementExpressionParser;
    }

    public PdfParseResult parse(String text) {
        List<ParsedCourseRecord> records = new ArrayList<>();
        List<ParserWarning> warnings = new ArrayList<>();

        List<CourseBlock> blocks = splitIntoBlocks(text);
        for (CourseBlock block : blocks) {
            List<String> recordWarnings = new ArrayList<>();
            RequirementDefinition prerequisite = null;
            if (block.prerequisiteText != null && !block.prerequisiteText.isBlank()) {
                try {
                    prerequisite = requirementExpressionParser.parse(block.prerequisiteText);
                } catch (IllegalArgumentException exception) {
                    recordWarnings.add("Could not parse prerequisite expression");
                    warnings.add(new ParserWarning(
                        "PREREQ_PARSE",
                        "Course " + block.courseCode + ": " + exception.getMessage()
                    ));
                }
            }

            CourseDefinition courseDefinition = new CourseDefinition(
                block.courseCode,
                block.title,
                block.credits,
                block.description,
                prerequisite
            );
            records.add(new ParsedCourseRecord(courseDefinition, block.prerequisiteText, recordWarnings));
        }

        return new PdfParseResult(records, warnings);
    }

    private List<CourseBlock> splitIntoBlocks(String text) {
        List<CourseBlock> blocks = new ArrayList<>();
        String[] lines = text.split("\\R");
        CourseBlockBuilder current = null;

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isBlank()) {
                continue;
            }

            Matcher matcher = HEADER_PATTERN.matcher(line.toUpperCase());
            if (matcher.matches()) {
                if (current != null) {
                    blocks.add(current.build());
                }
                current = new CourseBlockBuilder(
                    normalizeCourseCode(matcher.group("code")),
                    line.substring(0, line.lastIndexOf('(')).replaceFirst("^[A-Z]{2,}\\s*\\d{3}[A-Z]?\\s*[-:]?\\s*", "").trim(),
                    Integer.valueOf(matcher.group("credits"))
                );
                continue;
            }

            if (current == null) {
                continue;
            }

            if (line.toUpperCase().startsWith("PREREQUISITE")
                || line.toUpperCase().startsWith("PREREQUISITES")
                || line.toUpperCase().startsWith("PREREQ")) {
                current.setPrerequisiteText(line);
            } else {
                current.appendDescription(line);
            }
        }

        if (current != null) {
            blocks.add(current.build());
        }

        return blocks;
    }

    private String normalizeCourseCode(String value) {
        return value.replaceAll("\\s+", " ").trim();
    }

    private record CourseBlock(
        String courseCode,
        String title,
        Integer credits,
        String description,
        String prerequisiteText
    ) {}

    private static class CourseBlockBuilder {
        private final String courseCode;
        private final String title;
        private final Integer credits;
        private final StringBuilder description = new StringBuilder();
        private String prerequisiteText;

        private CourseBlockBuilder(String courseCode, String title, Integer credits) {
            this.courseCode = courseCode;
            this.title = title;
            this.credits = credits;
        }

        private void appendDescription(String line) {
            if (!description.isEmpty()) {
                description.append(' ');
            }
            description.append(line);
        }

        private void setPrerequisiteText(String prerequisiteText) {
            this.prerequisiteText = prerequisiteText;
        }

        private CourseBlock build() {
            return new CourseBlock(
                courseCode,
                title,
                credits,
                description.isEmpty() ? null : description.toString(),
                prerequisiteText
            );
        }
    }
}
