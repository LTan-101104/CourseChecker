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
        "^(?<code>[A-Z]{2,}\\s*\\d{3}[A-Z]?)\\s{2,}(?<title>.+?)\\s*$"
    );
    private static final Pattern CREDITS_PATTERN = Pattern.compile("(?i)(?<credits>\\d+)\\s+credits?\\.");
    private static final Pattern PREREQ_PATTERN = Pattern.compile(
        "(?i)prerequisite[s]?\\s*:\\s*(?<prereq>.*?)(?:\\.\\s*(?:\\d+\\s+credits?\\.)?|$)"
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
            List<ParserWarning> recordWarnings = new ArrayList<>();
            RequirementDefinition prerequisite = null;
            String normalizedPrerequisiteText = null;
            PrerequisiteParseOutcome prerequisiteParseOutcome = PrerequisiteParseOutcome.NOT_PRESENT;
            if (block.prerequisiteText != null && !block.prerequisiteText.isBlank()) {
                RequirementExpressionParser.ParseResult parseResult =
                    requirementExpressionParser.parseDetailed(block.prerequisiteText);
                normalizedPrerequisiteText = parseResult.normalizedExpression();
                prerequisiteParseOutcome = parseResult.outcome();
                if (parseResult.outcome() == PrerequisiteParseOutcome.PARSED) {
                    prerequisite = parseResult.requirement();
                } else {
                    String warningCode = parseResult.outcome() == PrerequisiteParseOutcome.UNSUPPORTED
                        ? "PREREQ_UNSUPPORTED"
                        : "PREREQ_MALFORMED";
                    ParserWarning warning = new ParserWarning(
                        warningCode,
                        parseResult.message(),
                        block.courseCode,
                        block.prerequisiteText,
                        parseResult.normalizedExpression()
                    );
                    recordWarnings.add(warning);
                    warnings.add(warning);
                }
            }

            CourseDefinition courseDefinition = new CourseDefinition(
                block.courseCode,
                block.title,
                block.credits,
                block.description,
                prerequisite,
                null
            );
            records.add(new ParsedCourseRecord(
                courseDefinition,
                block.prerequisiteText,
                normalizedPrerequisiteText,
                prerequisiteParseOutcome,
                recordWarnings
            ));
        }

        return new PdfParseResult(records, warnings);
    }

    private List<CourseBlock> splitIntoBlocks(String text) {
        List<CourseBlock> blocks = new ArrayList<>();
        String[] lines = text.split("\\R");
        List<String> pendingLines = new ArrayList<>();

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isBlank()) {
                continue;
            }

            Matcher matcher = HEADER_PATTERN.matcher(line);
            if (matcher.matches()) {
                CourseBlockBuilder current = new CourseBlockBuilder(
                    normalizeCourseCode(matcher.group("code")),
                    matcher.group("title").trim()
                );
                current.consumePendingLines(pendingLines);
                blocks.add(current.build());
                pendingLines.clear();
                continue;
            }

            pendingLines.add(line);
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
        private final StringBuilder description = new StringBuilder();
        private Integer credits;
        private String prerequisiteText;

        private CourseBlockBuilder(String courseCode, String title) {
            this.courseCode = courseCode;
            this.title = title;
        }

        private void consumePendingLines(List<String> pendingLines) {
            for (String line : pendingLines) {
                if (line.endsWith("INSTRUCTOR(S):") || line.contains("INSTRUCTOR(S):")) {
                    continue;
                }
                appendDescription(line);
            }
            String normalizedDescription = description.toString();
            Matcher creditsMatcher = CREDITS_PATTERN.matcher(normalizedDescription);
            if (creditsMatcher.find()) {
                this.credits = Integer.valueOf(creditsMatcher.group("credits"));
            }
            Matcher prereqMatcher = PREREQ_PATTERN.matcher(normalizedDescription);
            if (prereqMatcher.find()) {
                this.prerequisiteText = prereqMatcher.group("prereq").trim();
            }
            trimPreamble();
        }

        private void trimPreamble() {
            String normalized = description.toString()
                .replaceFirst("^Course Descriptions\\s+\\d{4}\\s+Spring.*?UMassAmherst\\s*", "")
                .trim();
            description.setLength(0);
            description.append(normalized);
        }

        private void appendDescription(String line) {
            if (!description.isEmpty()) {
                description.append(' ');
            }
            description.append(line);
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
