package com.example.server.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class FlywayMigrationIntegrationTest {

    @Test
    void v4BackfillsPasswordsAndThenEnforcesNotNull() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource("v4-" + UUID.randomUUID()));

        jdbcTemplate.execute("""
            CREATE TABLE app_user (
                id BIGSERIAL PRIMARY KEY,
                student_id VARCHAR(100) NOT NULL UNIQUE,
                display_name VARCHAR(255) NOT NULL,
                email VARCHAR(255) NOT NULL UNIQUE,
                password_hash VARCHAR(255)
            )
            """);

        jdbcTemplate.update(
            "INSERT INTO app_user (student_id, display_name, email, password_hash) VALUES (?, ?, ?, ?)",
            "A10001",
            "Alice",
            "alice@example.com",
            null
        );
        jdbcTemplate.update(
            "INSERT INTO app_user (student_id, display_name, email, password_hash) VALUES (?, ?, ?, ?)",
            "A10002",
            "Bob",
            "bob@example.com",
            ""
        );

        migrate(jdbcTemplate, "v4");

        assertThat(jdbcTemplate.queryForList("SELECT password_hash FROM app_user", String.class))
            .hasSize(2)
            .allSatisfy(passwordHash -> assertThat(passwordHash)
                .isEqualTo("$2y$10$a5JXny/t0KiauslKMUo2VuC/ZrJB83KToqInMWO1G8COowPA9tAhW"));

        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO app_user (student_id, display_name, email, password_hash) VALUES (?, ?, ?, ?)",
            "A10003",
            "Carol",
            "carol@example.com",
            null
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void v5EnforcesPerUserCompletedCourseUniqueness() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource("v5-" + UUID.randomUUID()));

        jdbcTemplate.execute("""
            CREATE TABLE app_user (
                id BIGSERIAL PRIMARY KEY,
                student_id VARCHAR(100) NOT NULL UNIQUE,
                display_name VARCHAR(255) NOT NULL,
                email VARCHAR(255) NOT NULL UNIQUE,
                password_hash VARCHAR(255) NOT NULL
            )
            """);

        jdbcTemplate.execute("""
            CREATE TABLE completed_course (
                id BIGSERIAL PRIMARY KEY,
                user_id BIGINT NOT NULL,
                course_code VARCHAR(50) NOT NULL,
                grade VARCHAR(10),
                semester VARCHAR(50),
                CONSTRAINT fk_completed_course_user
                    FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
            )
            """);

        jdbcTemplate.update(
            "INSERT INTO app_user (student_id, display_name, email, password_hash) VALUES (?, ?, ?, ?)",
            "A10001",
            "Alice",
            "alice@example.com",
            "hash-1"
        );
        jdbcTemplate.update(
            "INSERT INTO app_user (student_id, display_name, email, password_hash) VALUES (?, ?, ?, ?)",
            "A10002",
            "Bob",
            "bob@example.com",
            "hash-2"
        );

        migrate(jdbcTemplate, "v5");

        jdbcTemplate.update(
            "INSERT INTO completed_course (user_id, course_code, grade, semester) VALUES (?, ?, ?, ?)",
            1L,
            "COMPSCI 187",
            "A",
            "Fall 2024"
        );
        jdbcTemplate.update(
            "INSERT INTO completed_course (user_id, course_code, grade, semester) VALUES (?, ?, ?, ?)",
            2L,
            "COMPSCI 187",
            "A-",
            "Fall 2024"
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO completed_course (user_id, course_code, grade, semester) VALUES (?, ?, ?, ?)",
            1L,
            "COMPSCI 187",
            "B+",
            "Spring 2025"
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    private JdbcDataSource dataSource(String databaseName) {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + databaseName + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    private void migrate(JdbcTemplate jdbcTemplate, String fixtureDirectory) {
        Flyway.configure()
            .dataSource(jdbcTemplate.getDataSource())
            .locations("filesystem:" + migrationPath(fixtureDirectory))
            .baselineOnMigrate(true)
            .baselineVersion(MigrationVersion.fromVersion("3"))
            .load()
            .migrate();
    }

    private String migrationPath(String fixtureDirectory) {
        return Path.of("src/test/java/com/example/server/integration/flyway", fixtureDirectory)
            .toAbsolutePath()
            .toString();
    }
}
