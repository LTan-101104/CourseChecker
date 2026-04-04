package com.example.server.seed;

import java.util.List;

/**
 * Defines the shape of a prerequisite requirement for seed data.
 * This is a recursive tree structure that mirrors the Composite Pattern
 * but is decoupled from JPA entities.
 *
 * <p>Use the static factory methods to build trees:
 * <pre>
 *   // COMPSCI 187 AND (MATH 131 OR MATH 132)
 *   RequirementDefinition prereq = RequirementDefinition.and(
 *       RequirementDefinition.course("COMPSCI 187"),
 *       RequirementDefinition.or(
 *           RequirementDefinition.course("MATH 131"),
 *           RequirementDefinition.course("MATH 132")
 *       )
 *   );
 * </pre>
 */
public class RequirementDefinition {

    public enum Type { COURSE, AND, OR }

    private final Type type;
    private final String courseCode;       // only for COURSE type
    private final List<RequirementDefinition> children; // only for AND/OR types

    private RequirementDefinition(Type type, String courseCode, List<RequirementDefinition> children) {
        this.type = type;
        this.courseCode = courseCode;
        this.children = children;
    }

    // ── Factory Methods ─────────────────────────────────────

    public static RequirementDefinition course(String courseCode) {
        return new RequirementDefinition(Type.COURSE, courseCode, List.of());
    }

    public static RequirementDefinition and(RequirementDefinition... children) {
        return new RequirementDefinition(Type.AND, null, List.of(children));
    }

    public static RequirementDefinition or(RequirementDefinition... children) {
        return new RequirementDefinition(Type.OR, null, List.of(children));
    }

    // ── Getters ─────────────────────────────────────────────

    public Type getType() { return type; }
    public String getCourseCode() { return courseCode; }
    public List<RequirementDefinition> getChildren() { return children; }
}
