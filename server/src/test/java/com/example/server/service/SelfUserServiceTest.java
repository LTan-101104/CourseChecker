package com.example.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.server.dto.UpdateSelfUserRequest;
import com.example.server.dto.UserResponse;
import com.example.server.exception.ConflictException;
import com.example.server.model.User;
import com.example.server.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class SelfUserServiceTest {

    @Mock
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SelfUserService selfUserService;

    @Test
    void updateCurrentUserRejectsDuplicateEmail() {
        User user = new User("student-123", "Ada Lovelace", "ada@umass.edu", "hash");
        user.setId(1L);

        when(authService.loadUser(1L)).thenReturn(user);
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("grace@umass.edu", 1L)).thenReturn(true);

        assertThatThrownBy(() -> selfUserService.updateCurrentUser(
            1L,
            new UpdateSelfUserRequest("student-123", "Ada Lovelace", "grace@umass.edu")
        )).isInstanceOf(ConflictException.class)
            .hasMessage("Email already exists");
    }

    @Test
    void updateCurrentUserSavesNormalizedFields() {
        User user = new User("student-123", "Ada Lovelace", "ada@umass.edu", "hash");
        user.setId(1L);

        when(authService.loadUser(1L)).thenReturn(user);
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("grace@umass.edu", 1L)).thenReturn(false);
        when(userRepository.existsByStudentIdAndIdNot("student-999", 1L)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = selfUserService.updateCurrentUser(
            1L,
            new UpdateSelfUserRequest("student-999", "Grace Hopper", "Grace@Umass.edu")
        );

        assertThat(response.studentId()).isEqualTo("student-999");
        assertThat(response.email()).isEqualTo("grace@umass.edu");
        assertThat(response.displayName()).isEqualTo("Grace Hopper");
    }

    @Test
    void deleteCurrentUserDeletesLoadedUser() {
        User user = new User("student-123", "Ada Lovelace", "ada@umass.edu", "hash");
        user.setId(1L);
        when(authService.loadUser(1L)).thenReturn(user);

        selfUserService.deleteCurrentUser(1L);

        verify(userRepository).delete(user);
    }
}
