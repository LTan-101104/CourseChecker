package com.example.server.service;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.server.model.AndRequirement;
import com.example.server.model.Course;
import com.example.server.model.CourseRequirement;
import com.example.server.model.OrRequirement;
import com.example.server.model.Requirement;
import com.example.server.repository.CourseRepository;
import com.example.server.seed.CourseDefinition;
import com.example.server.seed.RequirementDefinition;

@Service
public class CourseImportService {

    public enum ImportAction {
        INSERTED,
        UPDATED
    }

    public record CourseImportResult(
        String courseCode,
        ImportAction action
    ) {}

    private final CourseRepository courseRepository;

    public CourseImportService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    public List<CourseImportResult> importCourses(List<CourseDefinition> definitions) {
        return definitions.stream().map(this::importCourse).toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CourseImportResult importCourse(CourseDefinition definition) {
        return importCourse(definition, false);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CourseImportResult importCourse(CourseDefinition definition, boolean preserveExistingPrerequisite) {
        String normalizedCode = normalizeCourseCode(definition.getCourseCode());
        Course course = courseRepository.findByCourseCodeIgnoreCase(normalizedCode).orElse(null);
        ImportAction action = course == null ? ImportAction.INSERTED : ImportAction.UPDATED;

        if (course == null) {
            course = new Course();
        }

        course.setCourseCode(normalizedCode);
        course.setTitle(definition.getTitle().trim());
        course.setCredits(definition.getCredits());
        course.setDescription(normalizeOptional(definition.getDescription()));
        if (definition.getCourseType() != null) {
            course.setCourseType(definition.getCourseType());
        }
        if (!(preserveExistingPrerequisite && course != null && definition.getPrerequisite() == null)) {
            course.setPrerequisite(buildRequirementTree(definition.getPrerequisite()));
        }

        courseRepository.save(course);
        return new CourseImportResult(normalizedCode, action);
    }

    private Requirement buildRequirementTree(RequirementDefinition def) {
        if (def == null) {
            return null;
        }

        return switch (def.getType()) {
            case COURSE -> new CourseRequirement(normalizeCourseCode(def.getCourseCode()));
            case AND -> {
                AndRequirement and = new AndRequirement();
                for (RequirementDefinition child : def.getChildren()) {
                    and.addChild(buildRequirementTree(child));
                }
                yield and;
            }
            case OR -> {
                OrRequirement or = new OrRequirement();
                for (RequirementDefinition child : def.getChildren()) {
                    or.addChild(buildRequirementTree(child));
                }
                yield or;
            }
        };
    }

    private String normalizeCourseCode(String courseCode) {
        return courseCode.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
