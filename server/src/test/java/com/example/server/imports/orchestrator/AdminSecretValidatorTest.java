package com.example.server.imports.orchestrator;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.example.server.security.AdminProperties;

class AdminSecretValidatorTest {

    @Test
    void assertValidAcceptsMatchingSecret() {
        AdminProperties adminProperties = new AdminProperties();
        adminProperties.setSecret("secret-123");
        AdminSecretValidator validator = new AdminSecretValidator(adminProperties);

        assertThatCode(() -> validator.assertValid("secret-123")).doesNotThrowAnyException();
    }

    @Test
    void assertValidRejectsMissingOrInvalidSecret() {
        AdminProperties adminProperties = new AdminProperties();
        adminProperties.setSecret("secret-123");
        AdminSecretValidator validator = new AdminSecretValidator(adminProperties);

        assertThatThrownBy(() -> validator.assertValid(null)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> validator.assertValid("wrong")).isInstanceOf(ResponseStatusException.class);
    }
}
