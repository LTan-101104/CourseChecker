package com.example.server.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.server.dto.AuthResponse;
import com.example.server.dto.CurrentUserResponse;
import com.example.server.dto.LoginRequest;
import com.example.server.dto.RegisterRequest;
import com.example.server.exception.ConflictException;
import com.example.server.exception.ResourceNotFoundException;
import com.example.server.exception.UnauthorizedException;
import com.example.server.model.User;
import com.example.server.repository.UserRepository;
import com.example.server.security.AuthenticatedUser;
import com.example.server.security.JwtService;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        String studentId = normalizeStudentId(request.studentId());
        String email = normalizeEmail(request.email());

        if (userRepository.existsByStudentId(studentId)) {
            throw new ConflictException("Student ID already exists");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email already exists");
        }

        User user = new User(
            studentId,
            request.displayName().trim(),
            email,
            passwordEncoder.encode(request.password())
        );

        User savedUser = userRepository.save(user);
        return toAuthResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
            .orElseThrow(() -> new UnauthorizedException("Bad credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Bad credentials");
        }

        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse currentUser(AuthenticatedUser principal) {
        return toCurrentUserResponse(loadUser(principal.id()));
    }

    @Transactional(readOnly = true)
    public User loadUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private AuthResponse toAuthResponse(User user) {
        return new AuthResponse(jwtService.generateToken(user), toCurrentUserResponse(user));
    }

    public CurrentUserResponse toCurrentUserResponse(User user) {
        return new CurrentUserResponse(
            user.getId(),
            user.getStudentId(),
            user.getDisplayName(),
            user.getEmail()
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private String normalizeStudentId(String studentId) {
        return studentId.trim();
    }
}
