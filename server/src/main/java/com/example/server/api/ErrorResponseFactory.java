package com.example.server.api;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ErrorResponseFactory {

    public ErrorResponse create(HttpStatus status, String message, String path) {
        return create(status, message, path, Map.of());
    }

    public ErrorResponse create(
        HttpStatus status,
        String message,
        String path,
        Map<String, String> details
    ) {
        return new ErrorResponse(
            Instant.now(),
            status.value(),
            status.getReasonPhrase(),
            message,
            path,
            details == null ? Map.of() : details
        );
    }
}
