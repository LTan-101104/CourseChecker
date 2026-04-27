ALTER TABLE import_job
    ADD COLUMN prerequisite_text_extracted_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE import_job
    ADD COLUMN prerequisite_parsed_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE import_job
    ADD COLUMN prerequisite_parse_failed_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE import_job
    ADD CONSTRAINT chk_import_job_prerequisite_counts_non_negative CHECK (
        prerequisite_text_extracted_count >= 0
        AND prerequisite_parsed_count >= 0
        AND prerequisite_parse_failed_count >= 0
    );

ALTER TABLE import_course_result
    ADD COLUMN warning_code VARCHAR(100);

ALTER TABLE import_course_result
    ADD COLUMN warning_detail TEXT;

ALTER TABLE import_course_result
    ADD COLUMN normalized_prerequisite_text TEXT;
