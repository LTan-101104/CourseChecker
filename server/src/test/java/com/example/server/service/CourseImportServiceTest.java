package com.example.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.server.model.AndRequirement;
import com.example.server.model.Course;
import com.example.server.model.CourseRequirement;
import com.example.server.model.CourseType;
import com.example.server.repository.CourseRepository;
import com.example.server.seed.CourseDefinition;
import com.example.server.seed.RequirementDefinition;
import com.example.server.service.CourseImportService.ImportAction;

@ExtendWith(MockitoExtension.class)
class CourseImportServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CourseImportService courseImportService;

    @Test
    void importCourseInsertsWhenCourseDoesNotExist() {
        CourseDefinition definition = new CourseDefinition(
            "compsci 220",
            " Programming Methodology ",
            4,
            " Intro to Java ",
            RequirementDefinition.course("compsci 187"),
            CourseType.CS
        );

        when(courseRepository.findByCourseCodeIgnoreCase("COMPSCI 220")).thenReturn(Optional.empty());
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CourseImportService.CourseImportResult result = courseImportService.importCourse(definition);

        assertThat(result.action()).isEqualTo(ImportAction.INSERTED);
        assertThat(result.courseCode()).isEqualTo("COMPSCI 220");
    }

    @Test
    void importCourseUpdatesExistingCourseAndReplacesPrerequisiteTree() {
        Course existing = new Course("COMPSCI 220", "Old Title", 3, "Old Description");
        existing.setPrerequisite(new CourseRequirement("MATH 131"));

        CourseDefinition definition = new CourseDefinition(
            "COMPSCI 220",
            "Programming Methodology",
            4,
            "New Description",
            RequirementDefinition.and(
                RequirementDefinition.course("compsci 187"),
                RequirementDefinition.or(
                    RequirementDefinition.course("math 131"),
                    RequirementDefinition.course("math 132")
                )
            ),
            CourseType.CS
        );

        when(courseRepository.findByCourseCodeIgnoreCase("COMPSCI 220")).thenReturn(Optional.of(existing));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CourseImportService.CourseImportResult result = courseImportService.importCourse(definition);

        assertThat(result.action()).isEqualTo(ImportAction.UPDATED);
        assertThat(existing.getPrerequisite()).isInstanceOf(AndRequirement.class);
        AndRequirement root = (AndRequirement) existing.getPrerequisite();
        assertThat(root.getChildren()).hasSize(2);
    }

    @Test
    void importCoursePreservesExistingPrerequisiteWhenRequested() {
        Course existing = new Course("COMPSCI 220", "Old Title", 3, "Old Description");
        CourseRequirement originalPrerequisite = new CourseRequirement("MATH 131");
        existing.setPrerequisite(originalPrerequisite);

        CourseDefinition definition = new CourseDefinition(
            "COMPSCI 220",
            "Programming Methodology",
            4,
            "New Description",
            null,
            CourseType.CS
        );

        when(courseRepository.findByCourseCodeIgnoreCase("COMPSCI 220")).thenReturn(Optional.of(existing));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CourseImportService.CourseImportResult result = courseImportService.importCourse(definition, true);

        assertThat(result.action()).isEqualTo(ImportAction.UPDATED);
        assertThat(existing.getPrerequisite()).isEqualTo(originalPrerequisite);
    }

    @Test
    void importCourseClearsExistingPrerequisiteWhenPreserveIsFalse() {
        Course existing = new Course("COMPSCI 220", "Old Title", 3, "Old Description");
        existing.setPrerequisite(new CourseRequirement("MATH 131"));

        CourseDefinition definition = new CourseDefinition(
            "COMPSCI 220",
            "Programming Methodology",
            4,
            "New Description",
            null,
            CourseType.CS
        );

        when(courseRepository.findByCourseCodeIgnoreCase("COMPSCI 220")).thenReturn(Optional.of(existing));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        courseImportService.importCourse(definition);

        assertThat(existing.getPrerequisite()).isNull();
    }

    @Test
    void importCourseKeepsExistingCourseTypeWhenDefinitionTypeIsMissing() {
        Course existing = new Course("COMPSCI 220", "Old Title", 3, "Old Description");
        existing.setCourseType(CourseType.CS);

        CourseDefinition definition = new CourseDefinition(
            "COMPSCI 220",
            "Programming Methodology",
            4,
            "New Description",
            null,
            null
        );

        when(courseRepository.findByCourseCodeIgnoreCase("COMPSCI 220")).thenReturn(Optional.of(existing));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        courseImportService.importCourse(definition);

        assertThat(existing.getCourseType()).isEqualTo(CourseType.CS);
    }
}
