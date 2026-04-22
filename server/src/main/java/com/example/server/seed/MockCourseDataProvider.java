package com.example.server.seed;

import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Hardcoded mock data for development and testing.
 *
 * <p>Seeds the following course catalog:
 * <ul>
 *   <li>COMPSCI 121 — no prerequisites</li>
 *   <li>COMPSCI 187 — prerequisite: COMPSCI 121</li>
 *   <li>MATH 131 — no prerequisites</li>
 *   <li>MATH 132 — prerequisite: MATH 131</li>
 *   <li>COMPSCI 220 — prerequisite: COMPSCI 187 AND (MATH 131 OR MATH 132)</li>
 *   <li>COMPSCI 230 — prerequisite: COMPSCI 220</li>
 * </ul>
 *
 * <p>Replace this class with a {@code WebCrawlerCourseDataProvider} that
 * implements {@link CourseDataProvider} to scrape live data from the
 * UMass Spire/course catalog.
 */
@Component
public class MockCourseDataProvider implements CourseDataProvider {

    @Override
    public List<UserDefinition> getUserDefinitions() {
        return List.of(
            new UserDefinition("student-001", "John Doe", "jdoe@umass.edu", null),
            new UserDefinition("student-002", "Jane Smith", "jsmith@umass.edu", null)
        );
    }

    @Override
    public List<CourseDefinition> getCourseDefinitions() {
        return List.of(

            // ── Introductory courses (no prerequisites) ─────────

            new CourseDefinition(
                "COMPSCI 121",
                "Introduction to Problem Solving with Computers",
                4,
                "An introductory course in computer science for non-CS majors.",
                null
            ),

            new CourseDefinition(
                "MATH 131",
                "Calculus I",
                4,
                "First course in calculus covering limits, derivatives, and integrals.",
                null
            ),

            new CourseDefinition(
                "MATH 132",
                "Calculus II",
                4,
                "Second course in calculus covering integration techniques and series.",
                RequirementDefinition.course("MATH 131")
            ),

            // ── Intermediate courses ────────────────────────────

            new CourseDefinition(
                "COMPSCI 187",
                "Programming with Data Structures",
                4,
                "Use, design, and analysis of data structures.",
                RequirementDefinition.course("COMPSCI 121")
            ),

            // ── Complex nested prerequisite ─────────────────────
            // COMPSCI 220 requires: COMPSCI 187 AND (MATH 131 OR MATH 132)

            new CourseDefinition(
                "COMPSCI 220",
                "Programming Methodology",
                4,
                "Development of individual skills necessary for designing, implementing, "
                    + "testing and modifying larger programs.",
                RequirementDefinition.and(
                    RequirementDefinition.course("COMPSCI 187"),
                    RequirementDefinition.or(
                        RequirementDefinition.course("MATH 131"),
                        RequirementDefinition.course("MATH 132")
                    )
                )
            ),

            // ── Deeper prerequisite chain ───────────────────────

            new CourseDefinition(
                "COMPSCI 230",
                "Computer Systems Principles",
                4,
                "Large-scale software systems development covering operating systems, "
                    + "networking, and distributed computing.",
                RequirementDefinition.course("COMPSCI 220")
            )
        );
    }

    @Override
    public List<CompletedCourseDefinition> getCompletedCourseDefinitions() {
        return List.of(
            // Student "student-001" has completed enough for COMPSCI 220
            new CompletedCourseDefinition("student-001", "COMPSCI 121", "A",  "Fall 2024"),
            new CompletedCourseDefinition("student-001", "COMPSCI 187", "B+", "Spring 2025"),
            new CompletedCourseDefinition("student-001", "MATH 131",   "A-", "Fall 2024"),

            // Student "student-002" is missing COMPSCI 187
            new CompletedCourseDefinition("student-002", "COMPSCI 121", "B",  "Fall 2024"),
            new CompletedCourseDefinition("student-002", "MATH 132",   "B+", "Spring 2025")
        );
    }
}
