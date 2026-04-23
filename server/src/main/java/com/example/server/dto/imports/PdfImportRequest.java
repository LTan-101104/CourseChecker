package com.example.server.dto.imports;

import jakarta.validation.constraints.NotBlank;

public record PdfImportRequest(
    @NotBlank(message = "sourcePageUrl is required")
    String sourcePageUrl
) {}
