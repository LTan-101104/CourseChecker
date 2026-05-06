package com.example.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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
import com.example.server.exception.ResourceNotFoundException;
import com.example.server.model.CompletedCourse;
import com.example.server.model.Course;
import com.example.server.model.User;
import com.example.server.repository.CompletedCourseRepository;
import com.example.server.repository.CourseRepository;

@ExtendWith(MockitoExtension.class)
class CompletedCourseServiceTest {

    @Mock
    private AuthService authService;

    @Mock
    private CompletedCourseRepository completedCourseRepository;

    @Mock
    private CourseRepository courseRepository;

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
        when(courseRepository.findByCourseCode("COMPSCI 220")).thenReturn(Optional.of(
            new Course("COMPSCI 220", "Programming Methodology", 4, "desc")
        ));

        var response = completedCourseService.createForUser(
            1L,
            new CreateCompletedCourseRequest("compsci 220", "A", " Fall 2025 ")
        );

        ArgumentCaptor<CompletedCourse> captor = ArgumentCaptor.forClass(CompletedCourse.class);
        verify(completedCourseRepository).save(captor.capture());
        assertThat(captor.getValue().getCourseCode()).isEqualTo("COMPSCI 220");
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.semester()).isEqualTo("Fall 2025");
        assertThat(response.title()).isEqualTo("Programming Methodology");
        assertThat(response.credits()).isEqualTo(4);
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
    void updateForUserRejectsDuplicateNewCourseCode() {
        User owner = new User("student-123", "Ada Lovelace", "ada@umass.edu", "hash");
        owner.setId(1L);

        CompletedCourse completedCourse = new CompletedCourse(owner, "COMPSCI 121", "A", "Fall 2024");
        completedCourse.setId(5L);

        when(completedCourseRepository.findById(5L)).thenReturn(Optional.of(completedCourse));
        when(completedCourseRepository.existsByUserIdAndCourseCode(1L, "COMPSCI 187")).thenReturn(true);

        assertThatThrownBy(() -> completedCourseService.updateForUser(
            1L,
            5L,
            new UpdateCompletedCourseRequest("compsci 187", "A", "Fall 2025")
        )).isInstanceOf(ConflictException.class)
            .hasMessage("Completed course already exists for this user");
    }

    @Test
    void updateForUserAllowsSameCourseCodeAndReturnsMissingCatalogMetadataAsNull() {
        User owner = new User("student-123", "Ada Lovelace", "ada@umass.edu", "hash");
        owner.setId(1L);

        CompletedCourse completedCourse = new CompletedCourse(owner, "COMPSCI 121", "B", "Fall 2024");
        completedCourse.setId(5L);

        when(completedCourseRepository.findById(5L)).thenReturn(Optional.of(completedCourse));
        when(completedCourseRepository.save(any(CompletedCourse.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(courseRepository.findByCourseCode("COMPSCI 121")).thenReturn(Optional.empty());

        var response = completedCourseService.updateForUser(
            1L,
            5L,
            new UpdateCompletedCourseRequest(" compsci 121 ", "A", " Spring 2026 ")
        );

        assertThat(response.courseCode()).isEqualTo("COMPSCI 121");
        assertThat(response.grade()).isEqualTo("A");
        assertThat(response.semester()).isEqualTo("Spring 2026");
        assertThat(response.title()).isNull();
        assertThat(response.credits()).isNull();
    }

    @Test
    void deleteForUserThrowsWhenCompletedCourseDoesNotExist() {
        when(completedCourseRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> completedCourseService.deleteForUser(1L, 404L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Completed course not found");
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
        when(courseRepository.findByCourseCodeIn(Set.of("COMPSCI 121"))).thenReturn(List.of(
            new Course("COMPSCI 121", "Intro", 4, "desc")
        ));

        assertThat(completedCourseService.listForUser(1L))
            .singleElement()
            .extracting("courseCode", "grade", "title", "credits")
            .containsExactly("COMPSCI 121", "A", "Intro", 4);
    }

    @Test
    void listForUserReturnsNullCatalogMetadataWhenCatalogCourseIsMissing() {
        User owner = new User("student-123", "Ada Lovelace", "ada@umass.edu", "hash");
        owner.setId(1L);

        when(completedCourseRepository.findByUserIdOrderByCourseCodeAsc(1L)).thenReturn(List.of(
            new CompletedCourse(owner, "SPECIAL 190", "P", "Fall 2024")
        ));
        when(courseRepository.findByCourseCodeIn(Set.of("SPECIAL 190"))).thenReturn(List.of());

        assertThat(completedCourseService.listForUser(1L))
            .singleElement()
            .extracting("courseCode", "title", "credits")
            .containsExactly("SPECIAL 190", null, null);
    }
}
