package com.example.server.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.server.dto.RequirementNodeRequest;
import com.example.server.dto.RequirementNodeType;
import com.example.server.model.AndRequirement;
import com.example.server.model.CourseRequirement;
import com.example.server.model.OrRequirement;
import com.example.server.model.Requirement;

class RequirementTreeMapperTest {

    private final RequirementTreeMapper mapper = new RequirementTreeMapper();

    @Test
    void nullRequirementInputsMapToNullOutputs() {
        assertThat(mapper.toDescription(null)).isNull();
        assertThat(mapper.toResponse(null)).isNull();
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void toDescriptionFormatsNestedCompositeRequirement() {
        AndRequirement root = new AndRequirement();
        root.addChild(new CourseRequirement("COMPSCI 187"));

        OrRequirement choice = new OrRequirement();
        choice.addChild(new CourseRequirement("MATH 131"));
        choice.addChild(new CourseRequirement("MATH 132"));
        root.addChild(choice);

        assertThat(mapper.toDescription(root))
            .isEqualTo("COMPSCI 187 AND (MATH 131 OR MATH 132)");
    }

    @Test
    void toEntityBuildsRequirementTreeFromRequest() {
        RequirementNodeRequest request = new RequirementNodeRequest(
            RequirementNodeType.AND,
            null,
            List.of(
                new RequirementNodeRequest(RequirementNodeType.COURSE, "COMPSCI 187", List.of()),
                new RequirementNodeRequest(
                    RequirementNodeType.OR,
                    null,
                    List.of(
                        new RequirementNodeRequest(RequirementNodeType.COURSE, "MATH 131", List.of()),
                        new RequirementNodeRequest(RequirementNodeType.COURSE, "MATH 132", List.of())
                    )
                )
            )
        );

        Requirement requirement = mapper.toEntity(request);

        assertThat(requirement).isInstanceOf(AndRequirement.class);
        AndRequirement andRequirement = (AndRequirement) requirement;
        assertThat(andRequirement.getChildren()).hasSize(2);
        assertThat(andRequirement.getChildren().get(0)).isInstanceOf(CourseRequirement.class);
        assertThat(((CourseRequirement) andRequirement.getChildren().get(0)).getRequiredCourseCode())
            .isEqualTo("COMPSCI 187");
        assertThat(andRequirement.getChildren().get(1)).isInstanceOf(OrRequirement.class);
    }
}
