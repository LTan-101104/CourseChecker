CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_course_code_trgm
    ON course
    USING gin (lower(course_code) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_course_title_trgm
    ON course
    USING gin (lower(title) gin_trgm_ops);
