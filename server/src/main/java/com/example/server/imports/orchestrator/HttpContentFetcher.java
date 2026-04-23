package com.example.server.imports.orchestrator;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.springframework.stereotype.Component;

import com.example.server.config.ImportProperties;

@Component
public class HttpContentFetcher {

    private final HttpClient httpClient;
    private final ImportProperties importProperties;

    public HttpContentFetcher(ImportProperties importProperties) {
        this.httpClient = HttpClient.newHttpClient();
        this.importProperties = importProperties;
    }

    public String fetchHtml(URI uri) {
        HttpResponse<String> response = send(
            HttpRequest.newBuilder(uri)
                .GET()
                .timeout(Duration.ofSeconds(importProperties.getRequestTimeoutSeconds()))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        ensureSuccess(response.statusCode(), "Failed to fetch source page");
        return response.body();
    }

    public byte[] fetchPdfBytes(URI uri) {
        HttpResponse<byte[]> response = send(
            HttpRequest.newBuilder(uri)
                .GET()
                .timeout(Duration.ofSeconds(importProperties.getRequestTimeoutSeconds()))
                .build(),
            HttpResponse.BodyHandlers.ofByteArray()
        );
        ensureSuccess(response.statusCode(), "Failed to download PDF");

        byte[] body = response.body();
        if (body.length > importProperties.getMaxPdfSizeBytes()) {
            throw new IllegalArgumentException("PDF exceeds maximum allowed size");
        }

        String contentType = response.headers().firstValue("Content-Type").orElse("");
        if (!contentType.isBlank() && !contentType.toLowerCase().contains("pdf")) {
            throw new IllegalArgumentException("Resolved URL did not return a PDF document");
        }
        return body;
    }

    private <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> bodyHandler) {
        try {
            return httpClient.send(request, bodyHandler);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed HTTP request for import pipeline", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed HTTP request for import pipeline", exception);
        }
    }

    private void ensureSuccess(int statusCode, String message) {
        if (statusCode < 200 || statusCode >= 300) {
            throw new IllegalArgumentException(message + " (HTTP " + statusCode + ")");
        }
    }
}
