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
        assertThat(result.records().get(0).prerequisiteParseOutcome()).isEqualTo(PrerequisiteParseOutcome.PARSED);
    }

    @Test
    void parseRecordsStructuredWarningsForUnsupportedPrerequisites() {
        String text = """
            No programming experience required. Prerequisites: R1 (or a score of 15 or higher on the math placement test Part A). 4 credits.
            Faculty NameINSTRUCTOR(S):
            CICS 110  Foundations of Programming
            """;

        PdfParseResult result = parser.parse(text);

        assertThat(result.records()).hasSize(1);
        ParsedCourseRecord record = result.records().get(0);
        assertThat(record.prerequisiteParseOutcome()).isEqualTo(PrerequisiteParseOutcome.UNSUPPORTED);
        assertThat(record.normalizedPrerequisiteText()).isNotBlank();
        assertThat(record.warnings()).singleElement().satisfies(warning -> {
            assertThat(warning.code()).isEqualTo("PREREQ_UNSUPPORTED");
            assertThat(warning.courseCode()).isEqualTo("CICS 110");
            assertThat(warning.rawPrerequisiteText()).contains("math placement test");
        });
    }

    @Test
    void parseHandlesRealCatalogStyleBlocks() {
        String text = """
            This course will expose students to programming practices beyond the introductory level. (Gen. Ed. R2) Prerequisite: CICS 110 (previously INFO 190S) or COMPSCI 121 with a grade of C or better. 4 credits.
            Faculty NameINSTRUCTOR(S):
            CICS 160  Object-Oriented Programming

            An introduction to the design, analysis, and implementation of data structures. Prerequisites: CICS 210 (OR COMPSCI 187) , and either COMPSCI 250 or MATH 455, all with a grade of C or better. 4 credits.
            Faculty NameINSTRUCTOR(S):
            COMPSCI 311  Introduction to Algorithms
            """;

        PdfParseResult result = parser.parse(text);

        assertThat(result.records()).hasSize(2);
        assertThat(result.records()).allMatch(record -> record.courseDefinition().getPrerequisite() != null);
        assertThat(result.warnings()).isEmpty();
    }
}
