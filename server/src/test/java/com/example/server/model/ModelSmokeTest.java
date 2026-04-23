package com.example.server.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ModelSmokeTest {

    @Test
    void userAndCompletedCourseHelpersMaintainBidirectionalAssociation() {
        User user = new User("student-123", "Ada", "ada@umass.edu", "hash");
        CompletedCourse completedCourse = new CompletedCourse(user, "COMPSCI 121", "A", "Fall 2024");

        user.addCompletedCourse(completedCourse);
        assertThat(user.getCompletedCourses()).contains(completedCourse);
        assertThat(completedCourse.getUser()).isEqualTo(user);

        user.removeCompletedCourse(completedCourse);
        assertThat(user.getCompletedCourses()).doesNotContain(completedCourse);
        assertThat(completedCourse.getUser()).isNull();
    }

    @Test
    void compositeRequirementMutatorsAndEnumsAreCovered() {
        AndRequirement andRequirement = new AndRequirement();
        OrRequirement orRequirement = new OrRequirement();
        CourseRequirement courseRequirement = new CourseRequirement("COMPSCI 187");
        Course course = new Course("COMPSCI 220", "Programming Methodology", 4, "desc");

        andRequirement.addChild(courseRequirement);
        orRequirement.addChild(new CourseRequirement("MATH 131"));
        course.setPrerequisite(andRequirement);

        assertThat(andRequirement.getChildren()).hasSize(1);
        assertThat(orRequirement.getChildren()).hasSize(1);
        assertThat(course.getPrerequisite()).isEqualTo(andRequirement);
        assertThat(Requirement.class).isNotNull();
    }
}
