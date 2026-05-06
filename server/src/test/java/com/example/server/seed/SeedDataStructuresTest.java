package com.example.server.seed;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.example.server.model.CourseType;

class SeedDataStructuresTest {

    @Test
    void mockCourseDataProviderExposesExpectedDefinitions() {
        MockCourseDataProvider provider = new MockCourseDataProvider();

        assertThat(provider.getUserDefinitions()).hasSize(2);
        assertThat(provider.getCourseDefinitions()).extracting(CourseDefinition::getCourseCode)
            .contains("COMPSCI 121", "COMPSCI 220");
        assertThat(provider.getCompletedCourseDefinitions()).hasSize(5);
    }

    @Test
    void requirementDefinitionFactoryMethodsBuildExpectedShape() {
        RequirementDefinition requirement = RequirementDefinition.and(
            RequirementDefinition.course("COMPSCI 187"),
            RequirementDefinition.or(
                RequirementDefinition.course("MATH 131"),
                RequirementDefinition.course("MATH 132")
            )
        );

        assertThat(requirement.getType()).isEqualTo(RequirementDefinition.Type.AND);
        assertThat(requirement.getChildren()).hasSize(2);
        assertThat(requirement.getChildren().get(0).getCourseCode()).isEqualTo("COMPSCI 187");
    }

    @Test
    void courseDefinitionStoresFields() {
        CourseDefinition definition = new CourseDefinition(
            "COMPSCI 121",
            "Intro",
            4,
            "desc",
            null,
            CourseType.CS
        );

        assertThat(definition.getCourseCode()).isEqualTo("COMPSCI 121");
        assertThat(definition.getTitle()).isEqualTo("Intro");
        assertThat(definition.getCredits()).isEqualTo(4);
        assertThat(definition.getDescription()).isEqualTo("desc");
        assertThat(definition.getPrerequisite()).isNull();
        assertThat(definition.getCourseType()).isEqualTo(CourseType.CS);
    }
}
