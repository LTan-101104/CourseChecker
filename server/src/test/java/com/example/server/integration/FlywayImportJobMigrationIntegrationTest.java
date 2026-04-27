package com.example.server.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class FlywayImportJobMigrationIntegrationTest {

    @Test
    void migrationsCreateImportTablesWithDiagnostics() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource("v6-" + UUID.randomUUID()));
        migrate(jdbcTemplate);

        jdbcTemplate.execute("""
            INSERT INTO import_job (
                id, source_type, source_page_url, status, requested_at,
                parsed_count, inserted_count, updated_count, skipped_count, failed_count, warning_count,
                prerequisite_text_extracted_count, prerequisite_parsed_count, prerequisite_parse_failed_count
            ) VALUES (
                RANDOM_UUID(), 'PDF_URL_PAGE', 'https://example.com', 'PENDING', CURRENT_TIMESTAMP(),
                0, 0, 0, 0, 0, 0, 0, 0, 0
            )
            """);

        Integer jobCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM import_job", Integer.class);
        assertThat(jobCount).isEqualTo(1);
        Integer diagnosticCount = jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
            WHERE LOWER(TABLE_NAME) = 'import_course_result'
              AND LOWER(COLUMN_NAME) = 'normalized_prerequisite_text'
            """, Integer.class);
        assertThat(diagnosticCount).isEqualTo(1);
    }

    private JdbcDataSource dataSource(String databaseName) {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + databaseName + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    private void migrate(JdbcTemplate jdbcTemplate) {
        Flyway.configure()
            .dataSource(jdbcTemplate.getDataSource())
            .locations(
                "filesystem:" + Path.of("src/test/java/com/example/server/integration/flyway/v6import").toAbsolutePath(),
                "filesystem:" + Path.of("src/test/java/com/example/server/integration/flyway/v7import").toAbsolutePath()
            )
            .load()
            .migrate();
    }
}
