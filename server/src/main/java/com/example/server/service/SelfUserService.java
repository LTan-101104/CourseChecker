package com.example.server.service;

import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.server.dto.UpdateSelfUserRequest;
import com.example.server.dto.UserResponse;
import com.example.server.exception.ConflictException;
import com.example.server.model.User;
import com.example.server.repository.UserRepository;

@Service
@Transactional
public class SelfUserService {

    private final AuthService authService;
    private final UserRepository userRepository;

    public SelfUserService(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        return toResponse(authService.loadUser(userId));
    }

    public UserResponse updateCurrentUser(Long userId, UpdateSelfUserRequest request) {
        User user = authService.loadUser(userId);
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        String normalizedStudentId = request.studentId().trim();

        if (userRepository.existsByEmailIgnoreCaseAndIdNot(normalizedEmail, userId)) {
            throw new ConflictException("Email already exists");
        }
        if (userRepository.existsByStudentIdAndIdNot(normalizedStudentId, userId)) {
            throw new ConflictException("Student ID already exists");
        }

        user.setEmail(normalizedEmail);
        user.setStudentId(normalizedStudentId);
        user.setDisplayName(request.displayName().trim());

        return toResponse(userRepository.save(user));
    }

    public void deleteCurrentUser(Long userId) {
        userRepository.delete(authService.loadUser(userId));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getStudentId(),
            user.getDisplayName(),
            user.getEmail()
        );
    }
}
