package com.example.server.imports.orchestrator;

import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.server.dto.imports.ImportCourseResultResponse;
import com.example.server.dto.imports.ImportJobResponse;
import com.example.server.imports.parser.ParsedCourseRecord;
import com.example.server.imports.parser.PdfCourseCatalogParser;
import com.example.server.imports.parser.PdfParseResult;
import com.example.server.imports.parser.PdfTextExtractor;
import com.example.server.model.ImportCourseAction;
import com.example.server.model.ImportCourseResult;
import com.example.server.model.ImportJob;
import com.example.server.model.ImportJobStatus;
import com.example.server.model.ImportSourceType;
import com.example.server.repository.ImportCourseResultRepository;
import com.example.server.repository.ImportJobRepository;
import com.example.server.service.CourseImportService;

@Service
public class PdfImportOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(PdfImportOrchestrator.class);

    private final ImportJobRepository importJobRepository;
    private final ImportCourseResultRepository importCourseResultRepository;
    private final HttpContentFetcher httpContentFetcher;
    private final PdfTextExtractor pdfTextExtractor;
    private final PdfCourseCatalogParser pdfCourseCatalogParser;
    private final CourseImportService courseImportService;

    public PdfImportOrchestrator(
        ImportJobRepository importJobRepository,
        ImportCourseResultRepository importCourseResultRepository,
        HttpContentFetcher httpContentFetcher,
        PdfTextExtractor pdfTextExtractor,
        PdfCourseCatalogParser pdfCourseCatalogParser,
        CourseImportService courseImportService
    ) {
        this.importJobRepository = importJobRepository;
        this.importCourseResultRepository = importCourseResultRepository;
        this.httpContentFetcher = httpContentFetcher;
        this.pdfTextExtractor = pdfTextExtractor;
        this.pdfCourseCatalogParser = pdfCourseCatalogParser;
        this.courseImportService = courseImportService;
    }

    @Transactional
    public ImportJobResponse enqueueFromPageUrl(String sourcePageUrl, String requestedBy) {
        URI normalizedUri = validateAndNormalizeUrl(sourcePageUrl);

        ImportJob job = new ImportJob();
        job.setId(UUID.randomUUID());
        job.setSourceType(ImportSourceType.PDF_URL_PAGE);
        job.setSourcePageUrl(normalizedUri.toString());
        job.setStatus(ImportJobStatus.PENDING);
        job.setRequestedAt(Instant.now());
        job.setRequestedBy(requestedBy);
        job = importJobRepository.save(job);

        log.info("pdf import job={} status=PENDING source={}", job.getId(), job.getSourcePageUrl());
        return toResponse(job, List.of());
    }

    @Transactional(readOnly = true)
    public ImportJobResponse getJob(UUID jobId, boolean includeResults) {
        ImportJob job = importJobRepository.findById(jobId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Import job not found"));
        List<ImportCourseResultResponse> results = includeResults
            ? importCourseResultRepository.findByJob_IdOrderByIdAsc(jobId).stream().map(this::toResponse).toList()
            : List.of();
        return toResponse(job, results);
    }

    @Transactional
    public void executeJob(UUID jobId) {
        ImportJob job = importJobRepository.findById(jobId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Import job not found"));
        job.setStatus(ImportJobStatus.RUNNING);
        job.setStartedAt(Instant.now());
        importJobRepository.save(job);

        log.info("pdf import job={} status=RUNNING", job.getId());
        try {
            URI pdfUri = URI.create(job.getSourcePageUrl());
            job.setResolvedPdfUrl(pdfUri.toString());
            byte[] pdfBytes = httpContentFetcher.fetchPdfBytes(pdfUri);
            job.setSourceHash(hashBytes(pdfBytes));

            String text = pdfTextExtractor.extractText(pdfBytes);
            PdfParseResult parseResult = pdfCourseCatalogParser.parse(text);

            job.setWarningCount(parseResult.warnings().size());
            job.setParsedCount(parseResult.records().size());

            for (ParsedCourseRecord record : parseResult.records()) {
                importSingleCourse(job, record);
            }

            if (job.getParsedCount() == 0) {
                job.setStatus(ImportJobStatus.FAILED);
                job.setErrorMessage("No course records were parsed from the PDF");
            } else if (job.getFailedCount() > 0 || job.getWarningCount() > 0) {
                job.setStatus(ImportJobStatus.PARTIAL_SUCCESS);
            } else {
                job.setStatus(ImportJobStatus.SUCCEEDED);
            }
        } catch (Exception exception) {
            job.setStatus(ImportJobStatus.FAILED);
            job.setErrorMessage(exception.getMessage());
            log.warn("pdf import job={} failed: {}", job.getId(), exception.getMessage());
        } finally {
            job.setFinishedAt(Instant.now());
            importJobRepository.save(job);
            log.info(
                "pdf import job={} status={} parsed={} inserted={} updated={} failed={} warnings={}",
                job.getId(),
                job.getStatus(),
                job.getParsedCount(),
                job.getInsertedCount(),
                job.getUpdatedCount(),
                job.getFailedCount(),
                job.getWarningCount()
            );
        }
    }

    private void importSingleCourse(ImportJob job, ParsedCourseRecord record) {
        ImportCourseResult resultRow = new ImportCourseResult();
        resultRow.setCourseCode(record.courseDefinition().getCourseCode());
        resultRow.setTitle(record.courseDefinition().getTitle());
        resultRow.setDescriptionExcerpt(record.courseDefinition().getDescription());
        resultRow.setPrerequisiteText(record.prerequisiteText());
        resultRow.setWarningMessage(record.warnings().isEmpty() ? null : String.join("; ", record.warnings()));

        try {
            CourseImportService.CourseImportResult importResult =
                courseImportService.importCourse(record.courseDefinition());
            if (importResult.action() == CourseImportService.ImportAction.INSERTED) {
                job.setInsertedCount(job.getInsertedCount() + 1);
                resultRow.setAction(ImportCourseAction.INSERTED);
            } else {
                job.setUpdatedCount(job.getUpdatedCount() + 1);
                resultRow.setAction(ImportCourseAction.UPDATED);
            }
        } catch (Exception exception) {
            job.setFailedCount(job.getFailedCount() + 1);
            resultRow.setAction(ImportCourseAction.FAILED);
            resultRow.setErrorMessage(exception.getMessage());
        }

        resultRow.setJob(job);
        importCourseResultRepository.save(resultRow);
    }

    private URI validateAndNormalizeUrl(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("sourcePageUrl is required");
        }
        URI uri = URI.create(value.trim());
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new IllegalArgumentException("Only HTTP(S) URLs are supported");
        }
        return uri;
    }

    private String hashBytes(byte[] bytes) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(messageDigest.digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest unavailable", exception);
        }
    }

    private ImportJobResponse toResponse(ImportJob job, List<ImportCourseResultResponse> results) {
        return new ImportJobResponse(
            job.getId(),
            job.getSourceType().name(),
            job.getSourcePageUrl(),
            job.getResolvedPdfUrl(),
            job.getStatus().name(),
            job.getRequestedAt(),
            job.getStartedAt(),
            job.getFinishedAt(),
            job.getRequestedBy(),
            job.getParsedCount(),
            job.getInsertedCount(),
            job.getUpdatedCount(),
            job.getSkippedCount(),
            job.getFailedCount(),
            job.getWarningCount(),
            job.getErrorMessage(),
            job.getSourceHash(),
            results
        );
    }

    private ImportCourseResultResponse toResponse(ImportCourseResult result) {
        return new ImportCourseResultResponse(
            result.getCourseCode(),
            result.getAction().name(),
            result.getTitle(),
            result.getDescriptionExcerpt(),
            result.getPrerequisiteText(),
            result.getWarningMessage(),
            result.getErrorMessage()
        );
    }
}
