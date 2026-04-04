-- ============================================================
-- V1: Create schema for UMass CS Prerequisite Eligibility Checker
-- ============================================================

-- Requirement table (SINGLE_TABLE inheritance for Composite Pattern)
-- Stores all requirement types: COURSE, AND, OR
CREATE TABLE requirement (
    id              BIGSERIAL       PRIMARY KEY,
    requirement_type VARCHAR(31)    NOT NULL,

    -- Used by COURSE type only (leaf node)
    required_course_code VARCHAR(50),

    -- Used by AND/OR types: self-referencing FK for tree structure
    parent_id       BIGINT          REFERENCES requirement(id) ON DELETE CASCADE
);

CREATE INDEX idx_requirement_parent ON requirement(parent_id);
CREATE INDEX idx_requirement_type   ON requirement(requirement_type);

-- Course catalog
CREATE TABLE course (
    id              BIGSERIAL       PRIMARY KEY,
    course_code     VARCHAR(50)     NOT NULL UNIQUE,
    title           VARCHAR(255)    NOT NULL,
    credits         INTEGER,
    description     TEXT,

    -- Root of the prerequisite tree (nullable = no prerequisites)
    prerequisite_id BIGINT          REFERENCES requirement(id) ON DELETE SET NULL
);

CREATE INDEX idx_course_code ON course(course_code);

-- Student transcript / completed courses
CREATE TABLE completed_course (
    id              BIGSERIAL       PRIMARY KEY,
    student_id      VARCHAR(100)    NOT NULL,
    course_code     VARCHAR(50)     NOT NULL,
    grade           VARCHAR(10),
    semester        VARCHAR(50)
);

CREATE INDEX idx_completed_student  ON completed_course(student_id);
CREATE INDEX idx_completed_course   ON completed_course(course_code);
