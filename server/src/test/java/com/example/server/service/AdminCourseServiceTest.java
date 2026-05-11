package com.example.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.server.dto.CreateCourseRequest;
import com.example.server.dto.RequirementNodeRequest;
import com.example.server.dto.RequirementNodeType;
import com.example.server.dto.UpdateCourseRequest;
import com.example.server.exception.ConflictException;
import com.example.server.exception.ValidationException;
import com.example.server.model.Course;
import com.example.server.repository.CourseRepository;

@ExtendWith(MockitoExtension.class)
class AdminCourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private RequirementTreeMapper requirementTreeMapper;

    private AdminCourseService adminCourseService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        adminCourseService = new AdminCourseService(courseRepository, requirementTreeMapper);
    }

    @Test
    void createCourseRejectsDuplicateCode() {
        when(courseRepository.existsByCourseCodeIgnoreCase("COMPSCI 220")).thenReturn(true);

        assertThatThrownBy(() -> adminCourseService.createCourse(new CreateCourseRequest(
            "COMPSCI 220",
            "Programming Methodology",
            4,
            "desc",
            null
        ))).isInstanceOf(ConflictException.class)
            .hasMessage("Course already exists");
    }

    @Test
    void createCourseRejectsSelfReferentialPrerequisite() {
        RequirementNodeRequest prerequisite = new RequirementNodeRequest(
            RequirementNodeType.COURSE,
            "COMPSCI 220",
            List.of()
        );
        when(courseRepository.existsByCourseCodeIgnoreCase("COMPSCI 220")).thenReturn(false);

        assertThatThrownBy(() -> adminCourseService.createCourse(new CreateCourseRequest(
            "COMPSCI 220",
            "Programming Methodology",
            4,
            "desc",
            prerequisite
        ))).isInstanceOf(ValidationException.class)
            .hasMessage("A course cannot require itself");
    }

    @Test
    void updateCourseRejectsMismatchedPathAndBodyCodes() {
        assertThatThrownBy(() -> adminCourseService.updateCourse(
            "COMPSCI 220",
            new UpdateCourseRequest("COMPSCI 230", "Computer Systems", 4, "desc", null)
        )).isInstanceOf(ValidationException.class)
            .hasMessage("Course code in request body must match the path");
    }

    @Test
    void updateCourseReplacesPrerequisiteTree() {
        Course course = new Course("COMPSCI 220", "Programming Methodology", 4, "desc");
        when(courseRepository.findByCourseCodeIgnoreCase("COMPSCI 220")).thenReturn(Optional.of(course));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RequirementNodeRequest prerequisite = new RequirementNodeRequest(
            RequirementNodeType.AND,
            null,
            List.of(
                new RequirementNodeRequest(RequirementNodeType.COURSE, "COMPSCI 187", List.of()),
                new RequirementNodeRequest(RequirementNodeType.COURSE, "MATH 131", List.of())
            )
        );

        var response = adminCourseService.updateCourse(
            "COMPSCI 220",
            new UpdateCourseRequest("COMPSCI 220", "Programming Methodology", 4, "updated", prerequisite)
        );

        assertThat(response.prerequisite()).isNotNull();
        assertThat(response.prerequisiteDescription()).isEqualTo("COMPSCI 187 AND MATH 131");
    }

    @Test
    void compositeNodesRequireAtLeastTwoChildren() {
        RequirementNodeRequest invalidPrerequisite = new RequirementNodeRequest(
            RequirementNodeType.OR,
            null,
            List.of(new RequirementNodeRequest(RequirementNodeType.COURSE, "MATH 131", List.of()))
        );

        when(courseRepository.existsByCourseCodeIgnoreCase("COMPSCI 220")).thenReturn(false);

        assertThatThrownBy(() -> adminCourseService.createCourse(new CreateCourseRequest(
            "COMPSCI 220",
            "Programming Methodology",
            4,
            "desc",
            invalidPrerequisite
        ))).isInstanceOf(ValidationException.class)
            .hasMessage("OR nodes require at least 2 children");
    }

    @Test
    void courseNodesRequireCourseCode() {
        RequirementNodeRequest invalidPrerequisite = new RequirementNodeRequest(
            RequirementNodeType.COURSE,
            " ",
            List.of()
        );

        when(courseRepository.existsByCourseCodeIgnoreCase("COMPSCI 220")).thenReturn(false);

        assertThatThrownBy(() -> adminCourseService.createCourse(new CreateCourseRequest(
            "COMPSCI 220",
            "Programming Methodology",
            4,
            "desc",
            invalidPrerequisite
        ))).isInstanceOf(ValidationException.class)
            .hasMessage("COURSE nodes require a courseCode");
    }

    @Test
    void courseNodesRejectChildren() {
        RequirementNodeRequest invalidPrerequisite = new RequirementNodeRequest(
            RequirementNodeType.COURSE,
            "COMPSCI 187",
            List.of(new RequirementNodeRequest(RequirementNodeType.COURSE, "MATH 131", List.of()))
        );

        when(courseRepository.existsByCourseCodeIgnoreCase("COMPSCI 220")).thenReturn(false);

        assertThatThrownBy(() -> adminCourseService.createCourse(new CreateCourseRequest(
            "COMPSCI 220",
            "Programming Methodology",
            4,
            "desc",
            invalidPrerequisite
        ))).isInstanceOf(ValidationException.class)
            .hasMessage("COURSE nodes cannot define children");
    }

    @Test
    void compositeNodesRejectCourseCode() {
        RequirementNodeRequest invalidPrerequisite = new RequirementNodeRequest(
            RequirementNodeType.AND,
            "COMPSCI 187",
            List.of(
                new RequirementNodeRequest(RequirementNodeType.COURSE, "MATH 131", List.of()),
                new RequirementNodeRequest(RequirementNodeType.COURSE, "MATH 132", List.of())
            )
        );

        when(courseRepository.existsByCourseCodeIgnoreCase("COMPSCI 220")).thenReturn(false);

        assertThatThrownBy(() -> adminCourseService.createCourse(new CreateCourseRequest(
            "COMPSCI 220",
            "Programming Methodology",
            4,
            "desc",
            invalidPrerequisite
        ))).isInstanceOf(ValidationException.class)
            .hasMessage("AND nodes cannot define courseCode");
    }
}
