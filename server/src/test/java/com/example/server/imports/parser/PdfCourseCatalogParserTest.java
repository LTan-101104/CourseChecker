package com.example.server.imports.parser;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PdfCourseCatalogParserTest {

    private final PdfCourseCatalogParser parser =
        new PdfCourseCatalogParser(new RequirementExpressionParser());

    @Test
    void parseBuildsCourseDefinitionsFromSampleText() {
        String text = """
            Object-oriented design and software engineering.
            Prerequisite: COMPSCI 187 AND (MATH 131 OR MATH 132). 4 credits.
            Faculty NameINSTRUCTOR(S):
            COMPSCI 220  Programming Methodology

            Intro to architecture and systems.
            Prerequisite: COMPSCI 220. 4 credits.
            Faculty NameINSTRUCTOR(S):
            COMPSCI 230  Computer Systems Principles
            """;

        PdfParseResult result = parser.parse(text);

        assertThat(result.records()).hasSize(2);
        assertThat(result.records().get(0).courseDefinition().getCourseCode()).isEqualTo("COMPSCI 220");
        assertThat(result.records().get(0).courseDefinition().getTitle()).isEqualTo("Programming Methodology");
        assertThat(result.records().get(0).courseDefinition().getCredits()).isEqualTo(4);
        assertThat(result.records().get(0).courseDefinition().getPrerequisite()).isNotNull();
    }
}
