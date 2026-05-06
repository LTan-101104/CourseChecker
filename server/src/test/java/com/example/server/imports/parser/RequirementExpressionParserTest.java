package com.example.server.imports.parser;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.example.server.seed.RequirementDefinition;

class RequirementExpressionParserTest {

    private final RequirementExpressionParser parser = new RequirementExpressionParser();

    @Test
    void parseSupportsAndOrWithParentheses() {
        RequirementDefinition root =
            parser.parse("Prerequisite: COMPSCI 187 AND (MATH 131 OR MATH 132)");

        assertThat(root).isNotNull();
        assertThat(root.getType()).isEqualTo(RequirementDefinition.Type.AND);
        assertThat(root.getChildren()).hasSize(2);
        assertThat(root.getChildren().get(1).getType()).isEqualTo(RequirementDefinition.Type.OR);
    }

    @Test
    void parseIgnoresGradeAndPreviouslyClauses() {
        RequirementDefinition root =
            parser.parse("CICS 110 (previously INFO 190S) or COMPSCI 121 with a grade of C or better");

        assertThat(root.getType()).isEqualTo(RequirementDefinition.Type.OR);
        assertThat(root.getChildren()).extracting(RequirementDefinition::getCourseCode)
            .containsExactly("CICS 110", "COMPSCI 121");
    }

    @Test
    void parseInfersMissingSubjectWithinGroupedAlternatives() {
        RequirementDefinition root =
            parser.parse("COMPSCI 240 (or 250) and MATH 132");

        assertThat(root.getType()).isEqualTo(RequirementDefinition.Type.AND);
        RequirementDefinition alternative = root.getChildren().get(0);
        assertThat(alternative.getType()).isEqualTo(RequirementDefinition.Type.OR);
        assertThat(alternative.getChildren()).extracting(RequirementDefinition::getCourseCode)
            .containsExactly("COMPSCI 240", "COMPSCI 250");
    }

    @Test
    void parseSupportsMajorSpecificBranchesSeparatedBySemicolon() {
        RequirementDefinition root = parser.parse(
            "CS Majors: COMPSCI 220 and COMPSCI 230; INFORM Majors: INFO 248 and CICS 210"
        );

        assertThat(root.getType()).isEqualTo(RequirementDefinition.Type.OR);
        assertThat(root.getChildren()).hasSize(2);
        assertThat(root.getChildren()).allMatch(child -> child.getType() == RequirementDefinition.Type.AND);
    }

    @Test
    void parseDetailedFlagsUnsupportedNonCourseOnlyPrerequisites() {
        RequirementExpressionParser.ParseResult result = parser.parseDetailed(
            "R1 (or a score of 15 or higher on the math placement test Part A)"
        );

        assertThat(result.outcome()).isEqualTo(PrerequisiteParseOutcome.UNSUPPORTED);
        assertThat(result.requirement()).isNull();
        assertThat(result.message()).contains("supported course-only expression");
    }

    @Test
    void parseDetailedTreatsBlankTextAsNotPresent() {
        RequirementExpressionParser.ParseResult result = parser.parseDetailed("   ");

        assertThat(result.outcome()).isEqualTo(PrerequisiteParseOutcome.NOT_PRESENT);
        assertThat(result.requirement()).isNull();
        assertThat(result.normalizedExpression()).isNull();
    }

    @Test
    void parseDetailedFlagsUnbalancedParenthesesAsMalformed() {
        RequirementExpressionParser.ParseResult result = parser.parseDetailed("COMPSCI 187 AND (MATH 131 OR");

        assertThat(result.outcome()).isEqualTo(PrerequisiteParseOutcome.MALFORMED);
        assertThat(result.requirement()).isNull();
        assertThat(result.message()).contains("Malformed prerequisite expression");
    }

    @Test
    void parseExpandsAmpersandCourseShorthand() {
        RequirementDefinition root = parser.parse("Prerequisites: COMPSCI 230 & 240");

        assertThat(root.getType()).isEqualTo(RequirementDefinition.Type.AND);
        assertThat(root.getChildren()).extracting(RequirementDefinition::getCourseCode)
            .containsExactly("COMPSCI 230", "COMPSCI 240");
    }
}
