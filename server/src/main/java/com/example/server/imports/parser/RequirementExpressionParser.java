package com.example.server.imports.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.example.server.seed.RequirementDefinition;

@Component
public class RequirementExpressionParser {

    private static final Pattern COURSE_PATTERN =
        Pattern.compile("^[A-Z&-]{2,}\\s*\\d{3}[A-Z]?");
    private static final Pattern IMPLIED_COURSE_PATTERN =
        Pattern.compile("^\\d{3}[A-Z]?");
    private static final Pattern COURSE_WITH_AMPERSAND_PATTERN =
        Pattern.compile("([A-Z&-]{2,})\\s*(\\d{3}[A-Z]?)\\s*&\\s*(\\d{3}[A-Z]?)");
    private static final Pattern COURSE_WITH_IMPLIED_OR_PATTERN =
        Pattern.compile("([A-Z&-]{2,})\\s*(\\d{3}[A-Z]?)\\s*\\(\\s*OR\\s*(\\d{3}[A-Z]?)\\s*\\)");

    public RequirementDefinition parse(String rawValue) {
        ParseResult result = parseDetailed(rawValue);
        if (result.outcome() == PrerequisiteParseOutcome.NOT_PRESENT) {
            return null;
        }
        if (result.outcome() != PrerequisiteParseOutcome.PARSED) {
            throw new IllegalArgumentException(result.message());
        }
        return result.requirement();
    }

    public ParseResult parseDetailed(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return new ParseResult(null, null, PrerequisiteParseOutcome.NOT_PRESENT, "No prerequisite text present");
        }

        String normalized = normalize(rawValue);
        if (normalized.isBlank()) {
            return new ParseResult(
                null,
                normalized,
                PrerequisiteParseOutcome.UNSUPPORTED,
                "Prerequisite text did not contain a supported course-only expression"
            );
        }

        TokenStream tokenStream = new TokenStream(tokenize(normalized));
        if (tokenStream.isEnd()) {
            return new ParseResult(
                null,
                normalized,
                PrerequisiteParseOutcome.UNSUPPORTED,
                "Prerequisite text did not contain a supported course-only expression"
            );
        }
        if (tokenStream.tokens().stream().noneMatch(token -> token.type() == TokenType.COURSE)) {
            return new ParseResult(
                null,
                normalized,
                PrerequisiteParseOutcome.UNSUPPORTED,
                "Prerequisite text did not contain a supported course-only expression"
            );
        }

        try {
            RequirementDefinition result = parseOr(tokenStream);
            if (!tokenStream.isEnd()) {
                return new ParseResult(
                    null,
                    normalized,
                    PrerequisiteParseOutcome.MALFORMED,
                    "Malformed prerequisite expression after normalization: " + normalized
                );
            }
            return new ParseResult(result, normalized, PrerequisiteParseOutcome.PARSED, null);
        } catch (IllegalArgumentException exception) {
            return new ParseResult(
                null,
                normalized,
                PrerequisiteParseOutcome.MALFORMED,
                "Malformed prerequisite expression after normalization: " + normalized
            );
        }
    }

    private RequirementDefinition parseOr(TokenStream stream) {
        RequirementDefinition left = parseAnd(stream);
        List<RequirementDefinition> parts = new ArrayList<>();
        parts.add(left);
        while (stream.match(TokenType.OR)) {
            parts.add(parseAnd(stream));
        }

        if (parts.size() == 1) {
            return parts.get(0);
        }
        return RequirementDefinition.or(parts.toArray(RequirementDefinition[]::new));
    }

    private RequirementDefinition parseAnd(TokenStream stream) {
        RequirementDefinition left = parsePrimary(stream);
        List<RequirementDefinition> parts = new ArrayList<>();
        parts.add(left);

        while (stream.match(TokenType.AND) || stream.canStartPrimary()) {
            parts.add(parsePrimary(stream));
        }

        if (parts.size() == 1) {
            return parts.get(0);
        }
        return RequirementDefinition.and(parts.toArray(RequirementDefinition[]::new));
    }

    private RequirementDefinition parsePrimary(TokenStream stream) {
        if (stream.match(TokenType.LPAREN)) {
            RequirementDefinition inner = parseOr(stream);
            stream.expect(TokenType.RPAREN);
            return inner;
        }

        Token courseToken = stream.expect(TokenType.COURSE);
        return RequirementDefinition.course(courseToken.value());
    }

    private List<Token> tokenize(String value) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        String lastSubject = null;
        while (i < value.length()) {
            char current = value.charAt(i);
            if (Character.isWhitespace(current)) {
                i++;
                continue;
            }
            if (current == '(') {
                tokens.add(new Token(TokenType.LPAREN, "("));
                i++;
                continue;
            }
            if (current == ')') {
                tokens.add(new Token(TokenType.RPAREN, ")"));
                i++;
                continue;
            }

            String remaining = value.substring(i);
            if (remaining.startsWith("AND ") || remaining.equals("AND")) {
                tokens.add(new Token(TokenType.AND, "AND"));
                i += 3;
                continue;
            }
            if (remaining.startsWith("OR ") || remaining.equals("OR")) {
                tokens.add(new Token(TokenType.OR, "OR"));
                i += 2;
                continue;
            }

            Matcher courseMatcher = COURSE_PATTERN.matcher(remaining);
            if (courseMatcher.find()) {
                String course = normalizeCourseCode(courseMatcher.group());
                tokens.add(new Token(TokenType.COURSE, course));
                lastSubject = extractSubject(course);
                i += courseMatcher.group().length();
                continue;
            }

            Matcher impliedCourseMatcher = IMPLIED_COURSE_PATTERN.matcher(remaining);
            if (lastSubject != null && impliedCourseMatcher.find()) {
                String course = lastSubject + " " + impliedCourseMatcher.group();
                tokens.add(new Token(TokenType.COURSE, normalizeCourseCode(course)));
                i += impliedCourseMatcher.group().length();
                continue;
            }

            i++;
        }
        return tokens;
    }

    private String normalize(String value) {
        String normalized = " " + value.toUpperCase(Locale.ROOT) + " ";
        normalized = normalized
            .replace('\n', ' ')
            .replace("PREREQUISITE:", " ")
            .replace("PREREQUISITES:", " ")
            .replace("PREREQ:", " ")
            .replace("PREREQS:", " ")
            .replace("ONE OF THE FOLLOWING COURSES", " ")
            .replace("ONE OF THE FOLLOWING", " ")
            .replace("ONE OF", " ")
            .replace("EITHER", " ")
            .replace("BOTH", " ")
            .replace("OR ENGLISH WRITING WAIVER", " ")
            .replace("COURSES:", " ")
            .replace("COURSE:", " ");
        normalized = COURSE_WITH_AMPERSAND_PATTERN.matcher(normalized).replaceAll("$1 $2 AND $1 $3");
        normalized = COURSE_WITH_IMPLIED_OR_PATTERN.matcher(normalized).replaceAll("($1 $2 OR $1 $3)");
        normalized = normalized.replaceAll(
            "\\b(?:ALL\\s+)?WITH\\s+(?:A\\s+)?(?:MINIMUM\\s+)?GRADE\\s+OF\\s+[ABCDF][+-]?\\s+OR\\s+BETTER\\b",
            " "
        );
        normalized = normalized.replaceAll(
            "\\b(?:ALL\\s+)?WITH\\s+(?:A\\s+)?(?:MINIMUM\\s+)?GRADE\\s+OF\\s+[ABCDF][+-]?\\b",
            " "
        );
        normalized = normalized.replaceAll("\\((GEN\\.[^)]*)\\)", " ");
        normalized = normalized.replaceAll("\\((BASIC MATH SKILLS[^)]*)\\)", " ");
        normalized = normalized.replaceAll("\\((PART [A-Z][^)]*)\\)", " ");
        normalized = normalized.replaceAll("\\((PREVIOUSLY[^)]*)\\)", " ");
        normalized = normalized.replaceAll("PREVIOUSLY\\s+[A-Z&-]{2,}\\s*\\d{3}[A-Z]?", " ");
        normalized = normalized.replaceAll("\\bCS\\s+MAJORS?\\s*:", " ");
        normalized = normalized.replaceAll("\\bINFORM\\s+MAJORS?\\s*:", " ");
        normalized = normalized.replaceAll("\\bCICS\\s+MAJORS?\\s*:", " ");
        normalized = normalized.replaceAll("\\bNON[- ]CS\\s+MAJORS?\\s*:", " ");
        normalized = normalized.replaceAll("\\bR1\\b", " ");
        normalized = normalized.replaceAll("SCORE OF \\d+ OR HIGHER ON THE MATH PLACEMENT TEST(?: PART [A-Z])?", " ");
        normalized = normalized.replaceAll("COMPLETION OF THE\\s+", " ");
        normalized = normalized.replaceAll("GRADE OF [ABCDF][+-]?", " ");
        normalized = normalized.replaceAll("\\bMATH PLACEMENT TEST\\b", " ");
        normalized = normalized.replaceAll("\\bPART [A-Z]\\b", " ");
        normalized = normalized.replaceAll("\\bGEN\\.? ED\\.? [A-Z0-9]+\\b", " ");
        normalized = normalized.replaceAll("(?<=\\d)\\s*;\\s*(?=[A-Z\\d])", " OR ");
        normalized = normalized.replace(';', ' ');
        normalized = normalized.replace(',', ' ');
        normalized = normalized.replaceAll("\\s+/\\s+", " OR ");
        normalized = normalized.replaceAll("\\(\\s*(OR|AND)\\s+", "(");
        normalized = normalized.replaceAll("\\(\\s*\\)", " ");
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return normalized;
    }

    private String normalizeCourseCode(String value) {
        return value.replaceAll("\\s+", " ").trim();
    }

    private String extractSubject(String courseCode) {
        int firstSpace = courseCode.indexOf(' ');
        return firstSpace < 0 ? null : courseCode.substring(0, firstSpace);
    }

    private enum TokenType {
        COURSE,
        AND,
        OR,
        LPAREN,
        RPAREN
    }

    private record Token(TokenType type, String value) {
    }

    public record ParseResult(
        RequirementDefinition requirement,
        String normalizedExpression,
        PrerequisiteParseOutcome outcome,
        String message
    ) {
    }

    private static class TokenStream {
        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        private boolean isEnd() {
            return index >= tokens.size();
        }

        private boolean match(TokenType tokenType) {
            if (isEnd() || tokens.get(index).type() != tokenType) {
                return false;
            }
            index++;
            return true;
        }

        private List<Token> tokens() {
            return tokens;
        }

        private boolean canStartPrimary() {
            if (isEnd()) {
                return false;
            }
            TokenType type = tokens.get(index).type();
            return type == TokenType.COURSE || type == TokenType.LPAREN;
        }

        private Token expect(TokenType tokenType) {
            if (isEnd() || tokens.get(index).type() != tokenType) {
                throw new IllegalArgumentException("Malformed prerequisite expression");
            }
            return tokens.get(index++);
        }
    }
}
