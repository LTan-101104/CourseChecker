package com.example.server.service;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.server.dto.CourseResponse;
import com.example.server.dto.CreateCourseRequest;
import com.example.server.dto.RequirementNodeRequest;
import com.example.server.dto.RequirementNodeType;
import com.example.server.dto.UpdateCourseRequest;
import com.example.server.exception.ConflictException;
import com.example.server.exception.CourseNotFoundException;
import com.example.server.exception.ValidationException;
import com.example.server.model.Course;
import com.example.server.repository.CourseRepository;

@Service
@Transactional
public class AdminCourseService {

    private final CourseRepository courseRepository;
    private final RequirementTreeMapper requirementTreeMapper;

    public AdminCourseService(
        CourseRepository courseRepository,
        RequirementTreeMapper requirementTreeMapper
    ) {
        this.courseRepository = courseRepository;
        this.requirementTreeMapper = requirementTreeMapper;
    }

    public CourseResponse createCourse(CreateCourseRequest request) {
        String normalizedCourseCode = normalizeCourseCode(request.courseCode());
        if (courseRepository.existsByCourseCodeIgnoreCase(normalizedCourseCode)) {
            throw new ConflictException("Course already exists");
        }

        validateRequirementTree(normalizedCourseCode, request.prerequisite());

        Course course = new Course(
            normalizedCourseCode,
            request.title().trim(),
            request.credits(),
            normalizeOptional(request.description())
        );
        course.setPrerequisite(requirementTreeMapper.toEntity(normalizeRequestTree(request.prerequisite())));

        return toResponse(courseRepository.save(course));
    }

    public CourseResponse updateCourse(String courseCode, UpdateCourseRequest request) {
        String normalizedPathCode = normalizeCourseCode(courseCode);
        String normalizedBodyCode = normalizeCourseCode(request.courseCode());
        if (!normalizedPathCode.equals(normalizedBodyCode)) {
            throw new ValidationException("Course code in request body must match the path");
        }

        validateRequirementTree(normalizedPathCode, request.prerequisite());

        Course course = courseRepository.findByCourseCodeIgnoreCase(normalizedPathCode)
            .orElseThrow(() -> new CourseNotFoundException(normalizedPathCode));

        course.setCourseCode(normalizedBodyCode);
        course.setTitle(request.title().trim());
        course.setCredits(request.credits());
        course.setDescription(normalizeOptional(request.description()));
        course.setPrerequisite(requirementTreeMapper.toEntity(normalizeRequestTree(request.prerequisite())));

        return toResponse(courseRepository.save(course));
    }

    public void deleteCourse(String courseCode) {
        Course course = courseRepository.findByCourseCodeIgnoreCase(normalizeCourseCode(courseCode))
            .orElseThrow(() -> new CourseNotFoundException(normalizeCourseCode(courseCode)));
        courseRepository.delete(course);
    }

    private CourseResponse toResponse(Course course) {
        return new CourseResponse(
            course.getCourseCode(),
            course.getTitle(),
            course.getCredits(),
            course.getDescription(),
            requirementTreeMapper.toResponse(course.getPrerequisite()),
            requirementTreeMapper.toDescription(course.getPrerequisite())
        );
    }

    private void validateRequirementTree(String courseCode, RequirementNodeRequest node) {
        if (node == null) {
            return;
        }

        if (node.type() == RequirementNodeType.COURSE) {
            if (node.courseCode() == null || node.courseCode().isBlank()) {
                throw new ValidationException("COURSE nodes require a courseCode");
            }
            if (node.children() != null && !node.children().isEmpty()) {
                throw new ValidationException("COURSE nodes cannot define children");
            }
            if (normalizeCourseCode(node.courseCode()).equals(courseCode)) {
                throw new ValidationException("A course cannot require itself");
            }
            return;
        }

        if (node.courseCode() != null && !node.courseCode().isBlank()) {
            throw new ValidationException(node.type() + " nodes cannot define courseCode");
        }
        if (node.children() == null || node.children().size() < 2) {
            throw new ValidationException(node.type() + " nodes require at least 2 children");
        }

        for (RequirementNodeRequest child : node.children()) {
            validateRequirementTree(courseCode, child);
        }
    }

    private RequirementNodeRequest normalizeRequestTree(RequirementNodeRequest node) {
        if (node == null) {
            return null;
        }

        List<RequirementNodeRequest> children = node.children() == null
            ? List.of()
            : node.children().stream().map(this::normalizeRequestTree).toList();

        String normalizedCourseCode = node.courseCode() == null ? null : normalizeCourseCode(node.courseCode());
        return new RequirementNodeRequest(node.type(), normalizedCourseCode, children);
    }

    private String normalizeCourseCode(String courseCode) {
        return courseCode.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
