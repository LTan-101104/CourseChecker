package com.example.server.imports.orchestrator;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.example.server.security.AdminProperties;

@Component
public class AdminSecretValidator {

    private static final String ADMIN_HEADER = "X-Admin-Secret";

    private final AdminProperties adminProperties;

    public AdminSecretValidator(AdminProperties adminProperties) {
        this.adminProperties = adminProperties;
    }

    public void assertValid(String providedSecret) {
        String expected = adminProperties.getSecret();
        if (expected == null || expected.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin secret is not configured");
        }
        if (providedSecret == null || !providedSecret.equals(expected)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Missing or invalid admin secret");
        }
    }

    public String adminHeaderName() {
        return ADMIN_HEADER;
    }
}
