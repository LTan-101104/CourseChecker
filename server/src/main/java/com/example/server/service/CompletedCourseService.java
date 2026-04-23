package com.example.server.service;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.server.dto.CompletedCourseResponse;
import com.example.server.dto.CreateCompletedCourseRequest;
import com.example.server.dto.UpdateCompletedCourseRequest;
import com.example.server.exception.ConflictException;
import com.example.server.exception.ForbiddenException;
import com.example.server.exception.ResourceNotFoundException;
import com.example.server.model.CompletedCourse;
import com.example.server.model.User;
import com.example.server.repository.CompletedCourseRepository;

@Service
@Transactional
public class CompletedCourseService {

    private final AuthService authService;
    private final CompletedCourseRepository completedCourseRepository;

    public CompletedCourseService(
        AuthService authService,
        CompletedCourseRepository completedCourseRepository
    ) {
        this.authService = authService;
        this.completedCourseRepository = completedCourseRepository;
    }

    @Transactional(readOnly = true)
    public List<CompletedCourseResponse> listForUser(Long userId) {
        return completedCourseRepository.findByUserIdOrderByCourseCodeAsc(userId).stream()
            .map(this::toResponse)
            .toList();
    }

    public CompletedCourseResponse createForUser(Long userId, CreateCompletedCourseRequest request) {
        User user = authService.loadUser(userId);
        String normalizedCourseCode = normalizeCourseCode(request.courseCode());

        if (completedCourseRepository.existsByUserIdAndCourseCode(userId, normalizedCourseCode)) {
            throw new ConflictException("Completed course already exists for this user");
        }

        CompletedCourse completedCourse = new CompletedCourse(
            user,
            normalizedCourseCode,
            normalizeOptional(request.grade()),
            normalizeOptional(request.semester())
        );

        return toResponse(completedCourseRepository.save(completedCourse));
    }

    public CompletedCourseResponse updateForUser(
        Long userId,
        Long completedCourseId,
        UpdateCompletedCourseRequest request
    ) {
        CompletedCourse completedCourse = loadOwnedCompletedCourse(completedCourseId, userId);
        String normalizedCourseCode = normalizeCourseCode(request.courseCode());

        if (!completedCourse.getCourseCode().equals(normalizedCourseCode)
            && completedCourseRepository.existsByUserIdAndCourseCode(userId, normalizedCourseCode)) {
            throw new ConflictException("Completed course already exists for this user");
        }

        completedCourse.setCourseCode(normalizedCourseCode);
        completedCourse.setGrade(normalizeOptional(request.grade()));
        completedCourse.setSemester(normalizeOptional(request.semester()));

        return toResponse(completedCourseRepository.save(completedCourse));
    }

    public void deleteForUser(Long userId, Long completedCourseId) {
        CompletedCourse completedCourse = loadOwnedCompletedCourse(completedCourseId, userId);
        completedCourseRepository.delete(completedCourse);
    }

    private CompletedCourse loadOwnedCompletedCourse(Long completedCourseId, Long userId) {
        CompletedCourse completedCourse = completedCourseRepository.findById(completedCourseId)
            .orElseThrow(() -> new ResourceNotFoundException("Completed course not found"));

        if (!completedCourse.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You do not have access to this completed course");
        }

        return completedCourse;
    }

    private CompletedCourseResponse toResponse(CompletedCourse completedCourse) {
        return new CompletedCourseResponse(
            completedCourse.getId(),
            completedCourse.getCourseCode(),
            completedCourse.getGrade(),
            completedCourse.getSemester()
        );
    }

    private String normalizeCourseCode(String courseCode) {
        return courseCode.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
