package com.example.server.imports.parser;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.example.server.seed.RequirementDefinition;

@Tag("stress")
class RequirementExpressionParserStressTest {

    private final RequirementExpressionParser parser = new RequirementExpressionParser();

    @Test
    @Timeout(5)
    void repeatedlyParsesNestedCourseOnlyExpressions() {
        for (int i = 0; i < 1_000; i++) {
            String expression = "Prerequisites: COMPSCI " + (100 + i % 400)
                + " AND (MATH 131 OR 132) AND (COMPSCI 220 OR 230)";

            RequirementExpressionParser.ParseResult result = parser.parseDetailed(expression);

            assertThat(result.outcome()).isEqualTo(PrerequisiteParseOutcome.PARSED);
            assertThat(result.requirement().getType()).isEqualTo(RequirementDefinition.Type.AND);
            assertThat(countCourseLeaves(result.requirement())).isEqualTo(5);
        }
    }

    private int countCourseLeaves(RequirementDefinition requirement) {
        if (requirement.getType() == RequirementDefinition.Type.COURSE) {
            return 1;
        }

        return requirement.getChildren().stream()
            .mapToInt(this::countCourseLeaves)
            .sum();
    }
}
