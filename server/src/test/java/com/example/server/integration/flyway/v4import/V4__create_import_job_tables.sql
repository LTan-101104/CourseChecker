CREATE TABLE import_job (
    id                  UUID PRIMARY KEY,
    source_type         VARCHAR(50) NOT NULL,
    source_page_url     TEXT NOT NULL,
    resolved_pdf_url    TEXT,
    status              VARCHAR(50) NOT NULL,
    requested_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    started_at          TIMESTAMP WITH TIME ZONE,
    finished_at         TIMESTAMP WITH TIME ZONE,
    requested_by        VARCHAR(255),
    parsed_count        INTEGER NOT NULL DEFAULT 0,
    inserted_count      INTEGER NOT NULL DEFAULT 0,
    updated_count       INTEGER NOT NULL DEFAULT 0,
    skipped_count       INTEGER NOT NULL DEFAULT 0,
    failed_count        INTEGER NOT NULL DEFAULT 0,
    warning_count       INTEGER NOT NULL DEFAULT 0,
    error_message       TEXT,
    source_hash         VARCHAR(128),
    CONSTRAINT chk_import_job_counts_non_negative CHECK (
        parsed_count >= 0
        AND inserted_count >= 0
        AND updated_count >= 0
        AND skipped_count >= 0
        AND failed_count >= 0
        AND warning_count >= 0
    )
);

CREATE TABLE import_course_result (
    id                  BIGSERIAL PRIMARY KEY,
    job_id              UUID NOT NULL,
    course_code         VARCHAR(100),
    action              VARCHAR(50) NOT NULL,
    title               TEXT,
    description_excerpt TEXT,
    prerequisite_text   TEXT,
    warning_message     TEXT,
    error_message       TEXT,
    CONSTRAINT fk_import_course_result_job
        FOREIGN KEY (job_id) REFERENCES import_job(id) ON DELETE CASCADE
);
