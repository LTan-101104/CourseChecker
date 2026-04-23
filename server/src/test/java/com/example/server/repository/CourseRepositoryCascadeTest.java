package com.example.server.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import com.example.server.model.AndRequirement;
import com.example.server.model.Course;
import com.example.server.model.CourseRequirement;
import com.example.server.model.OrRequirement;

import jakarta.persistence.EntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class CourseRepositoryCascadeTest {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private RequirementRepository requirementRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void deletingCourseRemovesEntirePrerequisiteTree() {
        AndRequirement root = new AndRequirement();
        root.addChild(new CourseRequirement("COMPSCI 187"));

        OrRequirement mathChoice = new OrRequirement();
        mathChoice.addChild(new CourseRequirement("MATH 131"));
        mathChoice.addChild(new CourseRequirement("MATH 132"));
        root.addChild(mathChoice);

        Course course = new Course("COMPSCI 220", "Programming Methodology", 4, "desc");
        course.setPrerequisite(root);

        Course savedCourse = courseRepository.saveAndFlush(course);
        entityManager.clear();

        assertThat(requirementRepository.count()).isEqualTo(5);

        courseRepository.deleteById(savedCourse.getId());
        courseRepository.flush();
        entityManager.clear();

        assertThat(courseRepository.findById(savedCourse.getId())).isEmpty();
        assertThat(requirementRepository.count()).isZero();
    }
}
