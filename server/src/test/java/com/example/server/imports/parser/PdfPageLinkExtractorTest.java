package com.example.server.imports.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import org.junit.jupiter.api.Test;

class PdfPageLinkExtractorTest {

    private final PdfPageLinkExtractor extractor = new PdfPageLinkExtractor();

    @Test
    void extractBestPdfUrlPicksRelativeCatalogPdf() {
        String html = """
            <html><body>
              <a href="/documents/old.pdf">Old</a>
              <a href="/documents/s26-course-descriptions.pdf">CICS Course Descriptions</a>
            </body></html>
            """;

        URI result = extractor.extractBestPdfUrl(
            "https://www.cics.umass.edu/documents/s26-course-descriptions",
            html
        );

        assertThat(result.toString())
            .isEqualTo("https://www.cics.umass.edu/documents/s26-course-descriptions.pdf");
    }
}
