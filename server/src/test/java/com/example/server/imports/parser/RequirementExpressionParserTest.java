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
}
