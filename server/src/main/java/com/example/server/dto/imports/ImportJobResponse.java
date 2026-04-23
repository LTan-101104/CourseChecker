package com.example.server.dto.imports;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ImportJobResponse(
    UUID jobId,
    String sourceType,
    String sourcePageUrl,
    String resolvedPdfUrl,
    String status,
    Instant requestedAt,
    Instant startedAt,
    Instant finishedAt,
    String requestedBy,
    int parsedCount,
    int insertedCount,
    int updatedCount,
    int skippedCount,
    int failedCount,
    int warningCount,
    String errorMessage,
    String sourceHash,
    List<ImportCourseResultResponse> results
) {}
