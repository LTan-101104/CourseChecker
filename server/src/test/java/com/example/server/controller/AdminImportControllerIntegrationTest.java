package com.example.server.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.server.dto.imports.ImportJobResponse;
import com.example.server.imports.orchestrator.AsyncPdfImportRunner;
import com.example.server.imports.orchestrator.PdfImportOrchestrator;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminImportControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PdfImportOrchestrator pdfImportOrchestrator;

    @MockitoBean
    private AsyncPdfImportRunner asyncPdfImportRunner;

    @Test
    void postRequiresAdminSecret() throws Exception {
        mockMvc.perform(post("/api/v1/admin/imports/pdf-url")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "sourcePageUrl": "https://www.cics.umass.edu/media/8761/download?attachment" }
                    """))
            .andExpect(status().isForbidden());
    }

    @Test
    void postReturnsAcceptedWithJobId() throws Exception {
        UUID jobId = UUID.randomUUID();
        ImportJobResponse response = new ImportJobResponse(
            jobId,
            "PDF_URL_PAGE",
            "https://www.cics.umass.edu/media/8761/download?attachment",
            null,
            "PENDING",
            Instant.now(),
            null,
            null,
            "admin-secret",
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            null,
            null,
            List.of()
        );
        when(pdfImportOrchestrator.enqueueFromPageUrl(any(), any())).thenReturn(response);
        doNothing().when(asyncPdfImportRunner).run(eq(jobId));

        mockMvc.perform(post("/api/v1/admin/imports/pdf-url")
                .header("X-Admin-Secret", "test-admin-secret")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "sourcePageUrl": "https://www.cics.umass.edu/media/8761/download?attachment" }
                    """))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.jobId").value(jobId.toString()))
            .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getReturnsJobSnapshot() throws Exception {
        UUID jobId = UUID.randomUUID();
        when(pdfImportOrchestrator.getJob(eq(jobId), eq(true))).thenReturn(new ImportJobResponse(
            jobId,
            "PDF_URL_PAGE",
            "https://www.cics.umass.edu/media/8761/download?attachment",
            "https://www.cics.umass.edu/media/8761/download?attachment",
            "SUCCEEDED",
            Instant.now(),
            Instant.now(),
            Instant.now(),
            "admin-secret",
            1,
            1,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            null,
            "abc",
            List.of()
        ));

        mockMvc.perform(get("/api/v1/admin/imports/{jobId}", jobId)
                .header("X-Admin-Secret", "test-admin-secret"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.jobId").value(jobId.toString()))
            .andExpect(jsonPath("$.status").value("SUCCEEDED"));
    }

    @Test
    void postRejectsInvalidUrl() throws Exception {
        when(pdfImportOrchestrator.enqueueFromPageUrl(any(), any()))
            .thenThrow(new IllegalArgumentException("Only HTTP(S) URLs are supported"));

        mockMvc.perform(post("/api/v1/admin/imports/pdf-url")
                .header("X-Admin-Secret", "test-admin-secret")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "sourcePageUrl": "file:///tmp/input.pdf" }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Only HTTP(S) URLs are supported"));
    }
}
