-- ============================================================
-- V3: Add user model and link transcript rows to app users
-- ============================================================

CREATE TABLE app_user (
    id              BIGSERIAL       PRIMARY KEY,
    student_id      VARCHAR(100)    NOT NULL UNIQUE,
    display_name    VARCHAR(255)    NOT NULL,
    email           VARCHAR(255)    NOT NULL UNIQUE,
    password_hash   VARCHAR(255)
);

INSERT INTO app_user (student_id, display_name, email, password_hash)
SELECT DISTINCT
    cc.student_id,
    cc.student_id,
    cc.student_id || '@example.local',
    NULL
FROM completed_course cc;

ALTER TABLE completed_course
    ADD COLUMN user_id BIGINT;

UPDATE completed_course cc
SET user_id = au.id
FROM app_user au
WHERE au.student_id = cc.student_id;

ALTER TABLE completed_course
    ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE completed_course
    ADD CONSTRAINT fk_completed_course_user
    FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE;

CREATE INDEX idx_completed_user_course_code ON completed_course(user_id, course_code);

DROP INDEX IF EXISTS idx_completed_student;

ALTER TABLE completed_course
    DROP COLUMN student_id;
