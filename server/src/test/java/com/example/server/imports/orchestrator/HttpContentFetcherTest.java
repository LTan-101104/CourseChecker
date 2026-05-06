package com.example.server.imports.orchestrator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.example.server.config.ImportProperties;
import com.sun.net.httpserver.HttpServer;

class HttpContentFetcherTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void fetchPdfBytesReturnsBodyForSuccessfulPdfResponse() throws Exception {
        byte[] pdfBytes = "%PDF-1.7".getBytes(StandardCharsets.UTF_8);
        URI uri = startServer(200, "application/pdf", pdfBytes);

        assertThat(newFetcher(100).fetchPdfBytes(uri)).isEqualTo(pdfBytes);
    }

    @Test
    void fetchPdfBytesRejectsNonSuccessStatus() throws Exception {
        URI uri = startServer(404, "application/pdf", "missing".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> newFetcher(100).fetchPdfBytes(uri))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Failed to download PDF (HTTP 404)");
    }

    @Test
    void fetchPdfBytesRejectsOversizedBody() throws Exception {
        URI uri = startServer(200, "application/pdf", "too-large".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> newFetcher(3).fetchPdfBytes(uri))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("PDF exceeds maximum allowed size");
    }

    @Test
    void fetchPdfBytesRejectsNonPdfContentType() throws Exception {
        URI uri = startServer(200, "text/html", "<html></html>".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> newFetcher(100).fetchPdfBytes(uri))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Resolved URL did not return a PDF document");
    }

    private URI startServer(int status, String contentType, byte[] body) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/catalog.pdf", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/catalog.pdf");
    }

    private HttpContentFetcher newFetcher(int maxPdfSizeBytes) {
        ImportProperties properties = new ImportProperties();
        properties.setMaxPdfSizeBytes(maxPdfSizeBytes);
        properties.setRequestTimeoutSeconds(5);
        return new HttpContentFetcher(properties);
    }
}
