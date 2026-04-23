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
        Pattern.compile("^[A-Z]{2,}\\s*\\d{3}[A-Z]?");

    public RequirementDefinition parse(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        String normalized = normalize(rawValue);
        TokenStream tokenStream = new TokenStream(tokenize(normalized));
        RequirementDefinition result = parseOr(tokenStream);
        if (!tokenStream.isEnd()) {
            throw new IllegalArgumentException("Unable to parse prerequisite expression: " + rawValue);
        }
        return result;
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
                i += courseMatcher.group().length();
                continue;
            }

            i++;
        }
        return tokens;
    }

    private String normalize(String value) {
        return value.toUpperCase(Locale.ROOT)
            .replace("PREREQUISITE:", " ")
            .replace("PREREQUISITES:", " ")
            .replace("PREREQ:", " ")
            .replace("ONE OF", " ")
            .replace(",", " AND ")
            .replace(";", " AND ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private String normalizeCourseCode(String value) {
        return value.replaceAll("\\s+", " ").trim();
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
