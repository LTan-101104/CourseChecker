package com.example.server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSelfUserRequest(
    @NotBlank @Size(max = 100) String studentId,
    @NotBlank @Size(max = 255) String displayName,
    @NotBlank @Email @Size(max = 255) String email
) {}
