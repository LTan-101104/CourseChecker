package com.example.server.imports.orchestrator;

import java.util.UUID;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class AsyncPdfImportRunner {

    private final PdfImportOrchestrator pdfImportOrchestrator;

    public AsyncPdfImportRunner(PdfImportOrchestrator pdfImportOrchestrator) {
        this.pdfImportOrchestrator = pdfImportOrchestrator;
    }

    @Async("importTaskExecutor")
    public void run(UUID jobId) {
        pdfImportOrchestrator.executeJob(jobId);
    }
}
