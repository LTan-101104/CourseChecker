package com.example.server.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.example.server.dto.RequirementNodeRequest;
import com.example.server.dto.RequirementNodeResponse;
import com.example.server.dto.RequirementNodeType;
import com.example.server.model.AndRequirement;
import com.example.server.model.CourseRequirement;
import com.example.server.model.OrRequirement;
import com.example.server.model.Requirement;

@Component
public class RequirementTreeMapper {

    public String toDescription(Requirement requirement) {
        if (requirement == null) {
            return null;
        }

        return formatRequirement(requirement, null);
    }

    public RequirementNodeResponse toResponse(Requirement requirement) {
        if (requirement == null) {
            return null;
        }

        if (requirement instanceof CourseRequirement courseRequirement) {
            return new RequirementNodeResponse(
                RequirementNodeType.COURSE,
                courseRequirement.getRequiredCourseCode(),
                List.of()
            );
        }

        if (requirement instanceof AndRequirement andRequirement) {
            return new RequirementNodeResponse(
                RequirementNodeType.AND,
                null,
                andRequirement.getChildren().stream().map(this::toResponse).toList()
            );
        }

        if (requirement instanceof OrRequirement orRequirement) {
            return new RequirementNodeResponse(
                RequirementNodeType.OR,
                null,
                orRequirement.getChildren().stream().map(this::toResponse).toList()
            );
        }

        throw new IllegalArgumentException("Unsupported requirement type: " + requirement.getClass().getName());
    }

    public Requirement toEntity(RequirementNodeRequest request) {
        if (request == null) {
            return null;
        }

        return switch (request.type()) {
            case COURSE -> new CourseRequirement(request.courseCode());
            case AND -> {
                AndRequirement andRequirement = new AndRequirement();
                request.children().forEach(child -> andRequirement.addChild(toEntity(child)));
                yield andRequirement;
            }
            case OR -> {
                OrRequirement orRequirement = new OrRequirement();
                request.children().forEach(child -> orRequirement.addChild(toEntity(child)));
                yield orRequirement;
            }
        };
    }

    private String formatRequirement(
        Requirement requirement,
        Class<? extends Requirement> parentType
    ) {
        if (requirement instanceof CourseRequirement courseRequirement) {
            return courseRequirement.getRequiredCourseCode();
        }

        if (requirement instanceof AndRequirement andRequirement) {
            return formatComposite(andRequirement.getChildren(), " AND ", AndRequirement.class, parentType);
        }

        if (requirement instanceof OrRequirement orRequirement) {
            return formatComposite(orRequirement.getChildren(), " OR ", OrRequirement.class, parentType);
        }

        throw new IllegalArgumentException("Unsupported requirement type: " + requirement.getClass().getName());
    }

    private String formatComposite(
        List<Requirement> children,
        String delimiter,
        Class<? extends Requirement> currentType,
        Class<? extends Requirement> parentType
    ) {
        String text = children.stream()
            .map(child -> formatRequirement(child, currentType))
            .filter(childText -> !childText.isBlank())
            .collect(java.util.stream.Collectors.joining(delimiter));

        if (parentType != null && !parentType.equals(currentType)) {
            return "(" + text + ")";
        }

        return text;
    }
}
