package com.example.server.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.server.dto.CourseDetailDTO;
import com.example.server.dto.CourseSummaryDTO;
import com.example.server.exception.CourseNotFoundException;
import com.example.server.model.Course;
import com.example.server.repository.CourseRepository;

@Service
@Transactional(readOnly = true)
public class CourseCatalogService {

    static final int SEARCH_RESULT_LIMIT = 10;

    private final CourseRepository courseRepository;
    private final RequirementTreeMapper requirementTreeMapper;

    public CourseCatalogService(
        CourseRepository courseRepository,
        RequirementTreeMapper requirementTreeMapper
    ) {
        this.courseRepository = courseRepository;
        this.requirementTreeMapper = requirementTreeMapper;
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
            requirementTreeMapper.toDescription(course.getPrerequisite())
        );
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
