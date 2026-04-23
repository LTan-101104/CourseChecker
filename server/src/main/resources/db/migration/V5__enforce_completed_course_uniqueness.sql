-- ============================================================
-- V5: Enforce one completed course row per user + course code
-- ============================================================

ALTER TABLE completed_course
    ADD CONSTRAINT uk_completed_course_user_course_code
    UNIQUE (user_id, course_code);
