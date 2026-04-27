package com.example.server.imports.orchestrator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.server.dto.imports.ImportJobResponse;
import com.example.server.imports.parser.ParsedCourseRecord;
import com.example.server.imports.parser.PdfCourseCatalogParser;
import com.example.server.imports.parser.PdfParseResult;
import com.example.server.imports.parser.PdfTextExtractor;
import com.example.server.imports.parser.PrerequisiteParseOutcome;
import com.example.server.model.ImportJob;
import com.example.server.model.ImportJobStatus;
import com.example.server.model.ImportSourceType;
import com.example.server.repository.ImportCourseResultRepository;
import com.example.server.repository.ImportJobRepository;
import com.example.server.seed.CourseDefinition;
import com.example.server.service.CourseImportService;

@ExtendWith(MockitoExtension.class)
class PdfImportOrchestratorTest {

    @Mock
    private ImportJobRepository importJobRepository;

    @Mock
    private ImportCourseResultRepository importCourseResultRepository;

    @Mock
    private HttpContentFetcher httpContentFetcher;

    @Mock
    private PdfTextExtractor pdfTextExtractor;

    @Mock
    private PdfCourseCatalogParser pdfCourseCatalogParser;

    @Mock
    private CourseImportService courseImportService;

    private PdfImportOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new PdfImportOrchestrator(
            importJobRepository,
            importCourseResultRepository,
            httpContentFetcher,
            pdfTextExtractor,
            pdfCourseCatalogParser,
            courseImportService
        );
    }

    @Test
    void enqueueCreatesPendingJob() {
        when(importJobRepository.save(any(ImportJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ImportJobResponse response = orchestrator.enqueueFromPageUrl(
            "https://www.cics.umass.edu/media/8761/download?attachment",
            "admin"
        );

        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.sourcePageUrl()).contains("https://");
    }

    @Test
    void executeJobSucceedsWhenCoursesImport() {
        ImportJob job = new ImportJob();
        UUID jobId = UUID.randomUUID();
        job.setId(jobId);
        job.setSourcePageUrl("https://www.cics.umass.edu/media/8761/download?attachment");
        job.setSourceType(ImportSourceType.PDF_URL_PAGE);
        job.setStatus(ImportJobStatus.PENDING);

        when(importJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(importJobRepository.save(any(ImportJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(httpContentFetcher.fetchPdfBytes(any())).thenReturn("PDF".getBytes());
        when(pdfTextExtractor.extractText(any())).thenReturn("TEXT");

        CourseDefinition courseDefinition = new CourseDefinition(
            "COMPSCI 220",
            "Programming Methodology",
            4,
            "desc",
            com.example.server.seed.RequirementDefinition.course("COMPSCI 187")
        );
        ParsedCourseRecord record = new ParsedCourseRecord(
            courseDefinition,
            "COMPSCI 187",
            "COMPSCI 187",
            PrerequisiteParseOutcome.PARSED,
            List.of()
        );
        when(pdfCourseCatalogParser.parse("TEXT")).thenReturn(new PdfParseResult(List.of(record), List.of()));
        when(courseImportService.importCourse(any(), eq(false))).thenReturn(new CourseImportService.CourseImportResult(
            "COMPSCI 220",
            CourseImportService.ImportAction.INSERTED
        ));

        orchestrator.executeJob(jobId);

        assertThat(job.getStatus()).isEqualTo(ImportJobStatus.SUCCEEDED);
        assertThat(job.getInsertedCount()).isEqualTo(1);
        assertThat(job.getFailedCount()).isEqualTo(0);
        assertThat(job.getPrerequisiteTextExtractedCount()).isEqualTo(1);
        assertThat(job.getPrerequisiteParsedCount()).isEqualTo(1);
        assertThat(job.getPrerequisiteParseFailedCount()).isEqualTo(0);
    }

    @Test
    void executeJobFailsWhenFetchThrows() {
        ImportJob job = new ImportJob();
        UUID jobId = UUID.randomUUID();
        job.setId(jobId);
        job.setSourcePageUrl("https://www.cics.umass.edu/media/8761/download?attachment");
        job.setSourceType(ImportSourceType.PDF_URL_PAGE);
        job.setStatus(ImportJobStatus.PENDING);

        when(importJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(importJobRepository.save(any(ImportJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(httpContentFetcher.fetchPdfBytes(any())).thenThrow(new IllegalArgumentException("boom"));

        orchestrator.executeJob(jobId);

        assertThat(job.getStatus()).isEqualTo(ImportJobStatus.FAILED);
        assertThat(job.getErrorMessage()).contains("boom");
    }

    @Test
    void getJobIncludesResultsWhenRequested() {
        ImportJob job = new ImportJob();
        UUID jobId = UUID.randomUUID();
        job.setId(jobId);
        job.setSourcePageUrl("https://www.cics.umass.edu/media/8761/download?attachment");
        job.setSourceType(ImportSourceType.PDF_URL_PAGE);
        job.setStatus(ImportJobStatus.PENDING);

        when(importJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(importCourseResultRepository.findByJob_IdOrderByIdAsc(eq(jobId))).thenReturn(List.of());

        ImportJobResponse response = orchestrator.getJob(jobId, true);

        assertThat(response.jobId()).isEqualTo(jobId);
        assertThat(response.results()).isEmpty();
    }

    @Test
    void executeJobPreservesExistingPrerequisiteWhenParserFails() {
        ImportJob job = new ImportJob();
        UUID jobId = UUID.randomUUID();
        job.setId(jobId);
        job.setSourcePageUrl("https://www.cics.umass.edu/media/8761/download?attachment");
        job.setSourceType(ImportSourceType.PDF_URL_PAGE);
        job.setStatus(ImportJobStatus.PENDING);

        when(importJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(importJobRepository.save(any(ImportJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(httpContentFetcher.fetchPdfBytes(any())).thenReturn("PDF".getBytes());
        when(pdfTextExtractor.extractText(any())).thenReturn("TEXT");

        CourseDefinition courseDefinition = new CourseDefinition(
            "COMPSCI 220",
            "Programming Methodology",
            4,
            "desc",
            null
        );
        ParsedCourseRecord record = new ParsedCourseRecord(
            courseDefinition,
            "R1 or math placement test",
            "",
            PrerequisiteParseOutcome.UNSUPPORTED,
            List.of(new com.example.server.imports.parser.ParserWarning(
                "PREREQ_UNSUPPORTED",
                "unsupported",
                "COMPSCI 220",
                "R1 or math placement test",
                ""
            ))
        );
        when(pdfCourseCatalogParser.parse("TEXT")).thenReturn(new PdfParseResult(List.of(record), record.warnings()));
        when(courseImportService.importCourse(same(courseDefinition), eq(true))).thenReturn(
            new CourseImportService.CourseImportResult("COMPSCI 220", CourseImportService.ImportAction.UPDATED)
        );

        orchestrator.executeJob(jobId);

        assertThat(job.getStatus()).isEqualTo(ImportJobStatus.PARTIAL_SUCCESS);
        assertThat(job.getWarningCount()).isEqualTo(1);
        assertThat(job.getPrerequisiteParseFailedCount()).isEqualTo(1);
        verify(courseImportService).importCourse(same(courseDefinition), eq(true));
    }
}
