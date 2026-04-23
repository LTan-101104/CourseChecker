package com.example.server.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ImportModelsSmokeTest {

    @Test
    void importJobAndResultAccessorsWork() {
        ImportJob job = new ImportJob();
        UUID jobId = UUID.randomUUID();
        Instant now = Instant.now();

        job.setId(jobId);
        job.setSourceType(ImportSourceType.PDF_URL_PAGE);
        job.setSourcePageUrl("https://example.com/catalog");
        job.setResolvedPdfUrl("https://example.com/catalog.pdf");
        job.setStatus(ImportJobStatus.PENDING);
        job.setRequestedAt(now);
        job.setStartedAt(now);
        job.setFinishedAt(now);
        job.setRequestedBy("admin");
        job.setParsedCount(10);
        job.setInsertedCount(3);
        job.setUpdatedCount(7);
        job.setSkippedCount(0);
        job.setFailedCount(1);
        job.setWarningCount(2);
        job.setErrorMessage("warning");
        job.setSourceHash("abc");

        ImportCourseResult result = new ImportCourseResult();
        result.setCourseCode("COMPSCI 220");
        result.setAction(ImportCourseAction.UPDATED);
        result.setTitle("Programming Methodology");
        result.setDescriptionExcerpt("desc");
        result.setPrerequisiteText("COMPSCI 187");
        result.setWarningMessage("warn");
        result.setErrorMessage("err");
        job.addResult(result);

        assertThat(job.getId()).isEqualTo(jobId);
        assertThat(job.getSourceType()).isEqualTo(ImportSourceType.PDF_URL_PAGE);
        assertThat(job.getSourcePageUrl()).contains("catalog");
        assertThat(job.getResolvedPdfUrl()).contains(".pdf");
        assertThat(job.getStatus()).isEqualTo(ImportJobStatus.PENDING);
        assertThat(job.getRequestedAt()).isEqualTo(now);
        assertThat(job.getStartedAt()).isEqualTo(now);
        assertThat(job.getFinishedAt()).isEqualTo(now);
        assertThat(job.getRequestedBy()).isEqualTo("admin");
        assertThat(job.getParsedCount()).isEqualTo(10);
        assertThat(job.getInsertedCount()).isEqualTo(3);
        assertThat(job.getUpdatedCount()).isEqualTo(7);
        assertThat(job.getSkippedCount()).isEqualTo(0);
        assertThat(job.getFailedCount()).isEqualTo(1);
        assertThat(job.getWarningCount()).isEqualTo(2);
        assertThat(job.getErrorMessage()).isEqualTo("warning");
        assertThat(job.getSourceHash()).isEqualTo("abc");
        assertThat(job.getResults()).hasSize(1);

        result.setJob(job);
        result.setId(5L);
        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getJob()).isEqualTo(job);
        assertThat(result.getCourseCode()).isEqualTo("COMPSCI 220");
        assertThat(result.getAction()).isEqualTo(ImportCourseAction.UPDATED);
        assertThat(result.getTitle()).isEqualTo("Programming Methodology");
        assertThat(result.getDescriptionExcerpt()).isEqualTo("desc");
        assertThat(result.getPrerequisiteText()).isEqualTo("COMPSCI 187");
        assertThat(result.getWarningMessage()).isEqualTo("warn");
        assertThat(result.getErrorMessage()).isEqualTo("err");

        job.setResults(List.of(result));
        assertThat(job.getResults()).containsExactly(result);
    }
}
