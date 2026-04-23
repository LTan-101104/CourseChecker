package com.example.server.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record RequirementNodeRequest(
    @NotNull RequirementNodeType type,
    String courseCode,
    List<@Valid RequirementNodeRequest> children
) {}
