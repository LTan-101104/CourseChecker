package com.example.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.example.server.dto.CourseDetailDTO;
import com.example.server.dto.RequirementNodeResponse;
import com.example.server.dto.RequirementNodeType;
import com.example.server.dto.CourseSummaryDTO;
import com.example.server.exception.CourseNotFoundException;
import com.example.server.model.AndRequirement;
import com.example.server.model.Course;
import com.example.server.model.CourseRequirement;
import com.example.server.model.CourseType;
import com.example.server.model.OrRequirement;
import com.example.server.repository.CourseRepository;

@ExtendWith(MockitoExtension.class)
class CourseCatalogServiceTest {

    @Mock
    private CourseRepository courseRepository;

    private CourseCatalogService courseCatalogService;

    @InjectMocks
    private RequirementTreeMapper requirementTreeMapper;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        courseCatalogService = new CourseCatalogService(courseRepository, requirementTreeMapper);
    }

    @Test
    void searchCoursesReturnsMappedSummariesAndUsesConfiguredLimit() {
        Course course = new Course("COMPSCI 187", "Programming with Data Structures", 4, "desc");
        course.setCourseType(CourseType.CS);

        when(courseRepository.findByCourseCodeContainingIgnoreCaseOrTitleContainingIgnoreCase(
            any(),
            any(),
            any(Pageable.class)
        )).thenReturn(List.of(course));

        List<CourseSummaryDTO> results = courseCatalogService.searchCourses("data structures", null);

        assertThat(results).containsExactly(
            new CourseSummaryDTO("COMPSCI 187", "Programming with Data Structures", 4, CourseType.CS)
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
    @Tag("unit")
    void searchCoursesUsesTypeFilterWhenProvided() {
        Course course = new Course("MATH 132", "Calculus II", 4, "desc");
        course.setCourseType(CourseType.MATH);

        when(courseRepository.findByTypeAndQuery(
            ArgumentMatchers.eq(CourseType.MATH),
            ArgumentMatchers.eq("calculus"),
            any(Pageable.class)
        )).thenReturn(List.of(course));

        List<CourseSummaryDTO> results = courseCatalogService.searchCourses(" calculus ", CourseType.MATH);

        assertThat(results).containsExactly(
            new CourseSummaryDTO("MATH 132", "Calculus II", 4, CourseType.MATH)
        );
        verify(courseRepository, never())
            .findByCourseCodeContainingIgnoreCaseOrTitleContainingIgnoreCase(any(), any(), any(Pageable.class));
    }

    @Test
    @Tag("unit")
    void searchCoursesAllowsBlankQueryWhenTypeFilterIsProvided() {
        Course course = new Course("STATS 315", "Statistics I", 3, "desc");
        course.setCourseType(CourseType.STATS);

        when(courseRepository.findByTypeAndQuery(
            ArgumentMatchers.eq(CourseType.STATS),
            ArgumentMatchers.eq(""),
            any(Pageable.class)
        )).thenReturn(List.of(course));

        List<CourseSummaryDTO> results = courseCatalogService.searchCourses("   ", CourseType.STATS);

        assertThat(results).containsExactly(
            new CourseSummaryDTO("STATS 315", "Statistics I", 3, CourseType.STATS)
        );
    }

    @Test
    void searchCoursesReturnsEmptyListForBlankQuery() {
        List<CourseSummaryDTO> results = courseCatalogService.searchCourses("   ", null);

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
        course.setCourseType(CourseType.CS);

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
            new RequirementNodeResponse(
                RequirementNodeType.AND,
                null,
                List.of(
                    new RequirementNodeResponse(
                        RequirementNodeType.COURSE,
                        "COMPSCI 187",
                        List.of()
                    ),
                    new RequirementNodeResponse(
                        RequirementNodeType.OR,
                        null,
                        List.of(
                            new RequirementNodeResponse(
                                RequirementNodeType.COURSE,
                                "MATH 131",
                                List.of()
                            ),
                            new RequirementNodeResponse(
                                RequirementNodeType.COURSE,
                                "MATH 132",
                                List.of()
                            )
                        )
                    )
                )
            ),
            "COMPSCI 187 AND (MATH 131 OR MATH 132)",
            CourseType.CS
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
