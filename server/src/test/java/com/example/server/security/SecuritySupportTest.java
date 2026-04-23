package com.example.server.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import com.example.server.model.User;

class SecuritySupportTest {

    @Test
    void authenticatedUserFactoryAndJwtServiceWork() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-jwt-secret-test-jwt-secret-test-jwt-secret-test-jwt-secret");
        properties.setExpirationMinutes(30);
        JwtService jwtService = new JwtService(properties);

        User user = new User("student-123", "Ada", "ada@umass.edu", "hash");
        user.setId(10L);

        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractUserId(token)).contains(10L);
        assertThat(AuthenticatedUser.from(user).email()).isEqualTo("ada@umass.edu");
    }

    @Test
    void applicationSecretsValidatorRejectsBlankSecretsOutsideDevAndTest() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("");
        AdminProperties adminProperties = new AdminProperties();
        adminProperties.setSecret("");

        Environment environment = org.mockito.Mockito.mock(Environment.class);
        org.mockito.Mockito.when(environment.getActiveProfiles()).thenReturn(new String[] {"prod"});

        ApplicationSecretsValidator validator = new ApplicationSecretsValidator(
            environment,
            jwtProperties,
            adminProperties
        );

        assertThatThrownBy(validator::validate)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("app.jwt.secret");
    }

    @Test
    void applicationSecretsValidatorAllowsDevDefaults() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("");
        AdminProperties adminProperties = new AdminProperties();
        adminProperties.setSecret("");

        Environment environment = org.mockito.Mockito.mock(Environment.class);
        org.mockito.Mockito.when(environment.getActiveProfiles()).thenReturn(new String[] {"dev"});

        new ApplicationSecretsValidator(environment, jwtProperties, adminProperties).validate();
    }
}
