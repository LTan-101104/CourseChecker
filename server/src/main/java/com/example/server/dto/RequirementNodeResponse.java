package com.example.server.dto;

import java.util.List;

public record RequirementNodeResponse(
    RequirementNodeType type,
    String courseCode,
    List<RequirementNodeResponse> children
) {}
