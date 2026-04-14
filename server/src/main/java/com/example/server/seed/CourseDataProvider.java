package com.example.server.seed;

import java.util.List;

/**
 * Contract for providing course + prerequisite data for database seeding.
 *
 * <p>Implementations:
 * <ul>
 *   <li>{@link MockCourseDataProvider} — hardcoded test data</li>
 *   <li>(Future) WebCrawlerCourseDataProvider — scrapes UMass website</li>
 * </ul>
 *
 * <p>The data layer is entirely decoupled from the database insertion logic
 * in {@link DatabaseSeeder}. To swap data sources, simply create a new
 * implementation and mark it as {@code @Primary} or use Spring profiles.
 */
public interface CourseDataProvider {

    /**
     * Returns a list of course definitions with their prerequisite trees.
     */
    List<CourseDefinition> getCourseDefinitions();

    /**
     * Returns canonical user data used by seed transcript rows.
     */
    List<UserDefinition> getUserDefinitions();

    /**
     * Returns mock student transcript data for testing.
     */
    List<CompletedCourseDefinition> getCompletedCourseDefinitions();

    /**
     * Simple record for a seeded user.
     */
    record UserDefinition(
        String studentId,
        String displayName,
        String email,
        String passwordHash
    ) {}

    /**
     * Simple record for a completed course entry in seed data.
     */
    record CompletedCourseDefinition(
        String studentId,
        String courseCode,
        String grade,
        String semester
    ) {}
}
