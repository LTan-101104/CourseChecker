package com.example.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
import org.springframework.data.domain.Pageable;

import com.example.server.dto.CourseDetailDTO;
import com.example.server.dto.CourseSummaryDTO;
import com.example.server.exception.CourseNotFoundException;
import com.example.server.model.AndRequirement;
import com.example.server.model.Course;
import com.example.server.model.CourseRequirement;
import com.example.server.model.OrRequirement;
import com.example.server.repository.CourseRepository;

@ExtendWith(MockitoExtension.class)
class CourseCatalogServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CourseCatalogService courseCatalogService;

    @Test
    void searchCoursesReturnsMappedSummariesAndUsesConfiguredLimit() {
        Course course = new Course("COMPSCI 187", "Programming with Data Structures", 4, "desc");

        when(courseRepository.findByCourseCodeContainingIgnoreCaseOrTitleContainingIgnoreCase(
            any(),
            any(),
            any(Pageable.class)
        )).thenReturn(List.of(course));

        List<CourseSummaryDTO> results = courseCatalogService.searchCourses("data structures");

        assertThat(results).containsExactly(
            new CourseSummaryDTO("COMPSCI 187", "Programming with Data Structures", 4)
        );

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(courseRepository).findByCourseCodeContainingIgnoreCaseOrTitleContainingIgnoreCase(
            org.mockito.ArgumentMatchers.eq("data structures"),
            org.mockito.ArgumentMatchers.eq("data structures"),
            pageableCaptor.capture()
        );

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(CourseCatalogService.SEARCH_RESULT_LIMIT);
        assertThat(pageable.getSort().getOrderFor("courseCode")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("courseCode").isAscending()).isTrue();
    }

    @Test
    void searchCoursesReturnsEmptyListForBlankQuery() {
        List<CourseSummaryDTO> results = courseCatalogService.searchCourses("   ");

        assertThat(results).isEmpty();
        verify(courseRepository, never())
            .findByCourseCodeContainingIgnoreCaseOrTitleContainingIgnoreCase(any(), any(), any(Pageable.class));
    }

    @Test
    void getCourseByCodeBuildsHumanReadablePrerequisiteString() {
        Course course = new Course(
            "COMPSCI 220",
            "Programming Methodology",
            4,
            "Object-oriented design and software engineering."
        );

        AndRequirement root = new AndRequirement();
        root.addChild(new CourseRequirement("COMPSCI 187"));

        OrRequirement mathChoice = new OrRequirement();
        mathChoice.addChild(new CourseRequirement("MATH 131"));
        mathChoice.addChild(new CourseRequirement("MATH 132"));
        root.addChild(mathChoice);

        course.setPrerequisite(root);

        when(courseRepository.findByCourseCodeIgnoreCase("COMPSCI 220"))
            .thenReturn(Optional.of(course));

        CourseDetailDTO detail = courseCatalogService.getCourseByCode("COMPSCI 220");

        assertThat(detail).isEqualTo(new CourseDetailDTO(
            "COMPSCI 220",
            "Programming Methodology",
            4,
            "Object-oriented design and software engineering.",
            "COMPSCI 187 AND (MATH 131 OR MATH 132)"
        ));
    }

    @Test
    void getCourseByCodeThrowsWhenCourseDoesNotExist() {
        when(courseRepository.findByCourseCodeIgnoreCase("COMPSCI 999"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseCatalogService.getCourseByCode("COMPSCI 999"))
            .isInstanceOf(CourseNotFoundException.class)
            .hasMessage("Course not found: COMPSCI 999");
    }
}
