package com.example.server.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.server.dto.CourseDetailDTO;
import com.example.server.dto.CourseSummaryDTO;
import com.example.server.exception.CourseNotFoundException;
import com.example.server.model.AndRequirement;
import com.example.server.model.Course;
import com.example.server.model.CourseRequirement;
import com.example.server.model.OrRequirement;
import com.example.server.model.Requirement;
import com.example.server.repository.CourseRepository;

@Service
@Transactional(readOnly = true)
public class CourseCatalogService {

    static final int SEARCH_RESULT_LIMIT = 10;

    private final CourseRepository courseRepository;

    public CourseCatalogService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    public List<CourseSummaryDTO> searchCourses(String query) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isEmpty()) {
            return List.of();
        }

        PageRequest pageRequest = PageRequest.of(
            0,
            SEARCH_RESULT_LIMIT,
            Sort.by(Sort.Order.asc("courseCode"))
        );

        return courseRepository
            .findByCourseCodeContainingIgnoreCaseOrTitleContainingIgnoreCase(
                normalizedQuery,
                normalizedQuery,
                pageRequest
            )
            .stream()
            .map(this::toSummaryDTO)
            .toList();
    }

    public CourseDetailDTO getCourseByCode(String courseCode) {
        String normalizedCourseCode = normalize(courseCode);

        Course course = courseRepository
            .findByCourseCodeIgnoreCase(normalizedCourseCode)
            .orElseThrow(() -> new CourseNotFoundException(normalizedCourseCode));

        return toDetailDTO(course);
    }

    private CourseSummaryDTO toSummaryDTO(Course course) {
        return new CourseSummaryDTO(
            course.getCourseCode(),
            course.getTitle(),
            course.getCredits()
        );
    }

    private CourseDetailDTO toDetailDTO(Course course) {
        return new CourseDetailDTO(
            course.getCourseCode(),
            course.getTitle(),
            course.getCredits(),
            course.getDescription(),
            toPrerequisiteDescription(course.getPrerequisite())
        );
    }

    private String toPrerequisiteDescription(Requirement requirement) {
        if (requirement == null) {
            return null;
        }

        return formatRequirement(requirement, null);
    }

    private String formatRequirement(
        Requirement requirement,
        Class<? extends Requirement> parentType
    ) {
        if (requirement instanceof CourseRequirement courseRequirement) {
            return courseRequirement.getRequiredCourseCode();
        }

        if (requirement instanceof AndRequirement andRequirement) {
            return formatComposite(andRequirement.getChildren(), " AND ", AndRequirement.class, parentType);
        }

        if (requirement instanceof OrRequirement orRequirement) {
            return formatComposite(orRequirement.getChildren(), " OR ", OrRequirement.class, parentType);
        }

        throw new IllegalArgumentException(
            "Unsupported requirement type: " + requirement.getClass().getName()
        );
    }

    private String formatComposite(
        List<Requirement> children,
        String delimiter,
        Class<? extends Requirement> currentType,
        Class<? extends Requirement> parentType
    ) {
        String text = children.stream()
            .map(child -> formatRequirement(child, currentType))
            .filter(childText -> !childText.isBlank())
            .collect(java.util.stream.Collectors.joining(delimiter));

        if (parentType != null && !parentType.equals(currentType)) {
            return "(" + text + ")";
        }

        return text;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
