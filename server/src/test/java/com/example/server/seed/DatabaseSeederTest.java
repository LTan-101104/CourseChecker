package com.example.server.seed;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.server.model.User;
import com.example.server.repository.CompletedCourseRepository;
import com.example.server.repository.CourseRepository;
import com.example.server.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class DatabaseSeederTest {

    @Mock
    private CourseDataProvider courseDataProvider;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CompletedCourseRepository completedCourseRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DatabaseSeeder databaseSeeder;

    @Test
    void runSeedsCoursesUsersAndCompletedCourses() {
        when(courseDataProvider.getCourseDefinitions()).thenReturn(List.of(
            new CourseDefinition("COMPSCI 121", "Intro", 4, "desc", null)
        ));
        when(courseRepository.existsByCourseCode("COMPSCI 121")).thenReturn(false);
        when(courseDataProvider.getUserDefinitions()).thenReturn(List.of(
            new CourseDataProvider.UserDefinition("student-123", "Ada", "ada@umass.edu", null)
        ));
        when(userRepository.findByStudentId("student-123")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123!")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(courseDataProvider.getCompletedCourseDefinitions()).thenReturn(List.of(
            new CourseDataProvider.CompletedCourseDefinition("student-123", "COMPSCI 121", "A", "Fall 2024")
        ));
        when(userRepository.findAll()).thenReturn(List.of(
            new User("student-123", "Ada", "ada@umass.edu", "encoded-password") {{
                setId(1L);
            }}
        ));
        when(completedCourseRepository.existsByUserStudentIdAndCourseCode("student-123", "COMPSCI 121"))
            .thenReturn(false);

        databaseSeeder.run();

        verify(courseRepository).save(any());
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        verify(completedCourseRepository).save(any());
        org.assertj.core.api.Assertions.assertThat(userCaptor.getValue().getPasswordHash())
            .isEqualTo("encoded-password");
    }

    @Test
    void runFailsWhenTranscriptReferencesMissingUser() {
        when(courseDataProvider.getCourseDefinitions()).thenReturn(List.of());
        when(courseDataProvider.getUserDefinitions()).thenReturn(List.of());
        when(courseDataProvider.getCompletedCourseDefinitions()).thenReturn(List.of(
            new CourseDataProvider.CompletedCourseDefinition("missing-student", "COMPSCI 121", "A", "Fall 2024")
        ));
        when(userRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> databaseSeeder.run())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("missing-student");
    }

    @Test
    void existingUsersKeepRealPasswordHashWhenSeedHashMissing() {
        User existingUser = new User("student-123", "Ada", "ada@umass.edu", "real-hash");
        existingUser.setId(1L);

        when(courseDataProvider.getCourseDefinitions()).thenReturn(List.of());
        when(courseDataProvider.getUserDefinitions()).thenReturn(List.of(
            new CourseDataProvider.UserDefinition("student-123", "Ada", "ada@umass.edu", null)
        ));
        when(userRepository.findByStudentId("student-123")).thenReturn(Optional.of(existingUser));
        when(courseDataProvider.getCompletedCourseDefinitions()).thenReturn(List.of());

        databaseSeeder.run();

        verify(userRepository).save(existingUser);
        org.assertj.core.api.Assertions.assertThat(existingUser.getPasswordHash()).isEqualTo("real-hash");
    }
}
