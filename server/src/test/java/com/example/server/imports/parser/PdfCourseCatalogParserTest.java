package com.example.server.imports.parser;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PdfCourseCatalogParserTest {

    private final PdfCourseCatalogParser parser =
        new PdfCourseCatalogParser(new RequirementExpressionParser());

    @Test
    void parseBuildsCourseDefinitionsFromSampleText() {
        String text = """
            COMPSCI 220 Programming Methodology (4 Credits)
            Object-oriented design and software engineering.
            Prerequisite: COMPSCI 187 AND (MATH 131 OR MATH 132)

            COMPSCI 230 Computer Systems Principles (4 Credits)
            Intro to architecture and systems.
            Prerequisite: COMPSCI 220
            """;

        PdfParseResult result = parser.parse(text);

        assertThat(result.records()).hasSize(2);
        assertThat(result.records().get(0).courseDefinition().getCourseCode()).isEqualTo("COMPSCI 220");
        assertThat(result.records().get(0).courseDefinition().getPrerequisite()).isNotNull();
    }
}
