package com.example.server.service;

import static org.assertj.core.api.Assertions.assertThat;
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

import com.example.server.dto.CreateCompletedCourseRequest;
import com.example.server.dto.UpdateCompletedCourseRequest;
import com.example.server.exception.ConflictException;
import com.example.server.exception.ForbiddenException;
import com.example.server.model.CompletedCourse;
import com.example.server.model.User;
import com.example.server.repository.CompletedCourseRepository;

@ExtendWith(MockitoExtension.class)
class CompletedCourseServiceTest {

    @Mock
    private AuthService authService;

    @Mock
    private CompletedCourseRepository completedCourseRepository;

    @InjectMocks
    private CompletedCourseService completedCourseService;

    @Test
    void createForUserRejectsDuplicateCourseCode() {
        User user = new User("student-123", "Ada Lovelace", "ada@umass.edu", "hash");
        user.setId(1L);
        when(authService.loadUser(1L)).thenReturn(user);
        when(completedCourseRepository.existsByUserIdAndCourseCode(1L, "COMPSCI 220")).thenReturn(true);

        assertThatThrownBy(() -> completedCourseService.createForUser(
            1L,
            new CreateCompletedCourseRequest("compsci 220", "A", "Fall 2025")
        )).isInstanceOf(ConflictException.class)
            .hasMessage("Completed course already exists for this user");
    }

    @Test
    void createForUserNormalizesAndPersistsCourse() {
        User user = new User("student-123", "Ada Lovelace", "ada@umass.edu", "hash");
        user.setId(1L);
        when(authService.loadUser(1L)).thenReturn(user);
        when(completedCourseRepository.existsByUserIdAndCourseCode(1L, "COMPSCI 220")).thenReturn(false);
        when(completedCourseRepository.save(any(CompletedCourse.class))).thenAnswer(invocation -> {
            CompletedCourse completedCourse = invocation.getArgument(0);
            completedCourse.setId(10L);
            return completedCourse;
        });

        var response = completedCourseService.createForUser(
            1L,
            new CreateCompletedCourseRequest("compsci 220", "A", " Fall 2025 ")
        );

        ArgumentCaptor<CompletedCourse> captor = ArgumentCaptor.forClass(CompletedCourse.class);
        verify(completedCourseRepository).save(captor.capture());
        assertThat(captor.getValue().getCourseCode()).isEqualTo("COMPSCI 220");
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.semester()).isEqualTo("Fall 2025");
    }

    @Test
    void updateForUserRejectsOtherOwners() {
        User owner = new User("student-999", "Other User", "other@umass.edu", "hash");
        owner.setId(999L);

        CompletedCourse completedCourse = new CompletedCourse(owner, "COMPSCI 121", "A", "Fall 2024");
        completedCourse.setId(5L);

        when(completedCourseRepository.findById(5L)).thenReturn(Optional.of(completedCourse));

        assertThatThrownBy(() -> completedCourseService.updateForUser(
            1L,
            5L,
            new UpdateCompletedCourseRequest("COMPSCI 220", "A", "Fall 2025")
        )).isInstanceOf(ForbiddenException.class)
            .hasMessage("You do not have access to this completed course");
    }

    @Test
    void deleteForUserDeletesOwnedCourse() {
        User owner = new User("student-123", "Ada Lovelace", "ada@umass.edu", "hash");
        owner.setId(1L);

        CompletedCourse completedCourse = new CompletedCourse(owner, "COMPSCI 121", "A", "Fall 2024");
        completedCourse.setId(5L);

        when(completedCourseRepository.findById(5L)).thenReturn(Optional.of(completedCourse));

        completedCourseService.deleteForUser(1L, 5L);

        verify(completedCourseRepository).delete(completedCourse);
    }

    @Test
    void listForUserMapsRepositoryRows() {
        User owner = new User("student-123", "Ada Lovelace", "ada@umass.edu", "hash");
        owner.setId(1L);

        when(completedCourseRepository.findByUserIdOrderByCourseCodeAsc(1L)).thenReturn(List.of(
            new CompletedCourse(owner, "COMPSCI 121", "A", "Fall 2024")
        ));

        assertThat(completedCourseService.listForUser(1L))
            .singleElement()
            .extracting("courseCode", "grade")
            .containsExactly("COMPSCI 121", "A");
    }
}
