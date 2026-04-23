package com.example.server.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.server.dto.imports.ImportCourseResultResponse;
import com.example.server.dto.imports.ImportJobResponse;
import com.example.server.dto.imports.PdfImportRequest;
import com.example.server.imports.orchestrator.AdminSecretValidator;
import com.example.server.imports.orchestrator.AsyncPdfImportRunner;
import com.example.server.imports.orchestrator.PdfImportOrchestrator;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/v1/admin/imports")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class AdminImportController {

    private final PdfImportOrchestrator pdfImportOrchestrator;
    private final AsyncPdfImportRunner asyncPdfImportRunner;
    private final AdminSecretValidator adminSecretValidator;

    public AdminImportController(
        PdfImportOrchestrator pdfImportOrchestrator,
        AsyncPdfImportRunner asyncPdfImportRunner,
        AdminSecretValidator adminSecretValidator
    ) {
        this.pdfImportOrchestrator = pdfImportOrchestrator;
        this.asyncPdfImportRunner = asyncPdfImportRunner;
        this.adminSecretValidator = adminSecretValidator;
    }

    @PostMapping("/pdf-url")
    public ResponseEntity<ImportJobResponse> createPdfUrlImport(
        @RequestHeader(name = "X-Admin-Secret", required = false) String adminSecret,
        @Valid @RequestBody PdfImportRequest request
    ) {
        adminSecretValidator.assertValid(adminSecret);
        ImportJobResponse response = pdfImportOrchestrator.enqueueFromPageUrl(
            request.sourcePageUrl(),
            "admin-secret"
        );
        asyncPdfImportRunner.run(response.jobId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{jobId}")
    public ImportJobResponse getJob(
        @RequestHeader(name = "X-Admin-Secret", required = false) String adminSecret,
        @PathVariable UUID jobId,
        @RequestParam(name = "includeResults", defaultValue = "true") boolean includeResults
    ) {
        adminSecretValidator.assertValid(adminSecret);
        return pdfImportOrchestrator.getJob(jobId, includeResults);
    }

    @GetMapping("/{jobId}/results")
    public java.util.List<ImportCourseResultResponse> getJobResults(
        @RequestHeader(name = "X-Admin-Secret", required = false) String adminSecret,
        @PathVariable UUID jobId
    ) {
        adminSecretValidator.assertValid(adminSecret);
        return pdfImportOrchestrator.getJob(jobId, true).results();
    }
}
