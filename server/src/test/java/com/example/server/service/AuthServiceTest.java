package com.example.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.server.dto.AuthResponse;
import com.example.server.dto.LoginRequest;
import com.example.server.dto.RegisterRequest;
import com.example.server.exception.ConflictException;
import com.example.server.exception.UnauthorizedException;
import com.example.server.model.User;
import com.example.server.repository.UserRepository;
import com.example.server.security.AuthenticatedUser;
import com.example.server.security.JwtService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerCreatesUserWithEncodedPasswordAndToken() {
        RegisterRequest request = new RegisterRequest(
            "student-123",
            "Ada Lovelace",
            "Ada@Umass.edu",
            "password123!"
        );

        when(userRepository.existsByStudentId("student-123")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("ada@umass.edu")).thenReturn(false);
        when(passwordEncoder.encode("password123!")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(5L);
            return user;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("ada@umass.edu");
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("encoded-password");
        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.user().displayName()).isEqualTo("Ada Lovelace");
    }

    @Test
    void registerRejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest(
            "student-123",
            "Ada Lovelace",
            "ada@umass.edu",
            "password123!"
        );
        when(userRepository.existsByStudentId("student-123")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("ada@umass.edu")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(ConflictException.class)
            .hasMessage("Email already exists");
    }

    @Test
    void registerRejectsDuplicateStudentId() {
        RegisterRequest request = new RegisterRequest(
            "student-123",
            "Ada Lovelace",
            "ada@umass.edu",
            "password123!"
        );
        when(userRepository.existsByStudentId("student-123")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(ConflictException.class)
            .hasMessage("Student ID already exists");
    }

    @Test
    void loginReturnsTokenForValidCredentials() {
        User user = new User("student-123", "Ada Lovelace", "ada@umass.edu", "encoded-password");
        user.setId(10L);

        when(userRepository.findByEmailIgnoreCase("ada@umass.edu")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123!", "encoded-password")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthResponse response = authService.login(new LoginRequest("ada@umass.edu", "password123!"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.user().id()).isEqualTo(10L);
    }

    @Test
    void loginRejectsBadPassword() {
        User user = new User("student-123", "Ada Lovelace", "ada@umass.edu", "encoded-password");

        when(userRepository.findByEmailIgnoreCase("ada@umass.edu")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("ada@umass.edu", "wrong-password")))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessage("Bad credentials");
    }

    @Test
    void currentUserMapsLoadedUser() {
        User user = new User("student-123", "Ada Lovelace", "ada@umass.edu", "encoded-password");
        user.setId(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        assertThat(authService.currentUser(new AuthenticatedUser(7L, "student-123", "Ada Lovelace", "ada@umass.edu")))
            .extracting("email", "studentId")
            .containsExactly("ada@umass.edu", "student-123");
    }
}
