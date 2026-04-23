package com.example.server.security;

import java.util.Arrays;
import java.util.Set;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;

@Component
public class ApplicationSecretsValidator {

    private final Environment environment;
    private final JwtProperties jwtProperties;
    private final AdminProperties adminProperties;

    public ApplicationSecretsValidator(
        Environment environment,
        JwtProperties jwtProperties,
        AdminProperties adminProperties
    ) {
        this.environment = environment;
        this.jwtProperties = jwtProperties;
        this.adminProperties = adminProperties;
    }

    @PostConstruct
    void validate() {
        Set<String> activeProfiles = Set.copyOf(Arrays.asList(environment.getActiveProfiles()));
        if (activeProfiles.isEmpty() || activeProfiles.contains("dev") || activeProfiles.contains("test")) {
            return;
        }

        requireSecret(jwtProperties.getSecret(), "app.jwt.secret");
        requireSecret(adminProperties.getSecret(), "app.admin.secret");
    }

    private void requireSecret(String value, String propertyName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(propertyName + " must be configured outside dev/test profiles");
        }
    }
}
