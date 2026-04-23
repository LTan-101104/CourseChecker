package com.example.server.imports.parser;

import java.net.URI;
import java.util.Comparator;
import java.util.Locale;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Component
public class PdfPageLinkExtractor {

    public URI extractBestPdfUrl(String sourcePageUrl, String html) {
        URI baseUri = URI.create(sourcePageUrl);
        if (isPdfPath(baseUri.toString())) {
            return baseUri;
        }

        Document document = Jsoup.parse(html, sourcePageUrl);
        return document.select("a[href]").stream()
            .map(anchor -> toCandidate(baseUri, anchor))
            .filter(candidate -> candidate.url() != null)
            .filter(candidate -> isPdfPath(candidate.url().toString()))
            .max(Comparator.comparingInt(PdfCandidate::score))
            .map(PdfCandidate::url)
            .orElseThrow(() -> new IllegalArgumentException("No PDF link found on source page"));
    }

    private PdfCandidate toCandidate(URI baseUri, Element anchor) {
        String href = anchor.attr("href");
        String text = anchor.text();
        try {
            URI resolved = baseUri.resolve(href);
            int score = score(resolved.toString(), text);
            return new PdfCandidate(resolved, score);
        } catch (IllegalArgumentException exception) {
            return new PdfCandidate(null, Integer.MIN_VALUE);
        }
    }

    private int score(String href, String text) {
        String haystack = (href + " " + text).toLowerCase(Locale.ROOT);
        int score = 1;
        if (haystack.contains("course")) {
            score += 3;
        }
        if (haystack.contains("description")) {
            score += 3;
        }
        if (haystack.contains("catalog")) {
            score += 2;
        }
        if (haystack.contains("cics")) {
            score += 1;
        }
        return score;
    }

    private boolean isPdfPath(String value) {
        return value.toLowerCase(Locale.ROOT).contains(".pdf");
    }

    private record PdfCandidate(URI url, int score) {
    }
}
