package com.example.server.dto;

public record UserResponse(
    Long id,
    String studentId,
    String displayName,
    String email
) {}
