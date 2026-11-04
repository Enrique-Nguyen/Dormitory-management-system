-- Correct the known malformed legacy academic-year value before enforcing the catalog invariant.
DO $$
DECLARE
    malformed_ids BIGINT[];
    canonical_id BIGINT;
BEGIN
    SELECT array_agg(id ORDER BY id)
    INTO malformed_ids
    FROM semester
    WHERE academic_year = '2908-2910';

    IF malformed_ids IS NOT NULL THEN
        SELECT id
        INTO canonical_id
        FROM semester
        WHERE academic_year = '2908-2909'
          AND type = 'HK1'
        ORDER BY id
        LIMIT 1;

        IF canonical_id IS NULL THEN
            UPDATE semester
            SET academic_year = '2908-2909'
            WHERE id = malformed_ids[1];
            canonical_id := malformed_ids[1];
            malformed_ids := malformed_ids[2:array_length(malformed_ids, 1)];
        END IF;

        UPDATE registration_period
        SET semester_id = canonical_id
        WHERE semester_id = ANY(malformed_ids);
        UPDATE invoice
        SET semester_id = canonical_id
        WHERE semester_id = ANY(malformed_ids);
        UPDATE summer_choice
        SET semester_id = canonical_id
        WHERE semester_id = ANY(malformed_ids);

        DELETE FROM semester
        WHERE id = ANY(malformed_ids);
    END IF;
END $$;

-- Managed reference catalogs. Preserve the exact legacy labels; never merge on trim().
CREATE TABLE school (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE CHECK (btrim(code) <> ''),
    name VARCHAR(200) NOT NULL UNIQUE CHECK (btrim(name) <> ''),
    active BOOLEAN NOT NULL DEFAULT true
);
CREATE TABLE faculty (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE CHECK (btrim(code) <> ''),
    name VARCHAR(100) NOT NULL CHECK (btrim(name) <> ''),
    active BOOLEAN NOT NULL DEFAULT true,
    school_id BIGINT REFERENCES school(id),
    UNIQUE NULLS NOT DISTINCT (school_id, name)
);
CREATE TABLE student_cohort (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE CHECK (btrim(code) <> ''),
    name VARCHAR(10) NOT NULL UNIQUE CHECK (btrim(name) <> ''),
    active BOOLEAN NOT NULL DEFAULT true
);
CREATE TABLE hometown (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE CHECK (btrim(code) <> ''),
    name VARCHAR(200) NOT NULL CHECK (btrim(name) <> ''),
    active BOOLEAN NOT NULL DEFAULT true,
    parent_id BIGINT REFERENCES hometown(id),
    UNIQUE NULLS NOT DISTINCT (parent_id, name),
    CONSTRAINT ck_hometown_not_self CHECK (parent_id IS NULL OR parent_id <> id)
);
CREATE TABLE academic_year (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(9) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL UNIQUE CHECK (btrim(name) <> ''),
    active BOOLEAN NOT NULL DEFAULT true,
    CONSTRAINT ck_academic_year_code CHECK (
        code ~ '^[0-9]{4}-[0-9]{4}$'
        AND substring(code, 6, 4)::integer = substring(code, 1, 4)::integer + 1)
);
CREATE TABLE amenity (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE CHECK (btrim(code) <> ''),
    name VARCHAR(500) NOT NULL UNIQUE CHECK (btrim(name) <> ''),
    active BOOLEAN NOT NULL DEFAULT true
);
CREATE TABLE maintenance_category (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE CHECK (btrim(code) <> ''),
    name VARCHAR(200) NOT NULL UNIQUE CHECK (btrim(name) <> ''),
    active BOOLEAN NOT NULL DEFAULT true
);

INSERT INTO faculty(code, name)
SELECT 'LEGACY_F_' || row_number() OVER (ORDER BY value), value
FROM (SELECT DISTINCT faculty AS value FROM student_profile WHERE btrim(faculty) <> '') legacy;
INSERT INTO student_cohort(code, name)
SELECT 'LEGACY_C_' || row_number() OVER (ORDER BY value), value
FROM (SELECT DISTINCT cohort AS value FROM student_profile WHERE btrim(cohort) <> '') legacy;
INSERT INTO hometown(code, name)
SELECT 'LEGACY_H_' || row_number() OVER (ORDER BY value), value
FROM (SELECT DISTINCT hometown AS value FROM student_profile WHERE btrim(hometown) <> '') legacy;
INSERT INTO academic_year(code, name)
SELECT DISTINCT academic_year, academic_year FROM semester;
-- A legacy amenities description is one managed item, not an invented list of tokens.
INSERT INTO amenity(code, name)
SELECT 'LEGACY_A_' || row_number() OVER (ORDER BY value), value
FROM (SELECT DISTINCT amenities AS value FROM room_type WHERE btrim(amenities) <> '') legacy;
INSERT INTO maintenance_category(code, name) VALUES
    ('ELECTRIC', 'Điện'), ('WATER', 'Nước'), ('FURNITURE', 'Nội thất'),
    ('DOOR_LOCK', 'Cửa và khóa'), ('OTHER', 'Khác');
INSERT INTO maintenance_category(code, name)
SELECT DISTINCT category, category FROM maintenance_request
WHERE category NOT IN (SELECT code FROM maintenance_category);

ALTER TABLE student_profile
    ADD COLUMN school_id BIGINT REFERENCES school(id),
    ADD COLUMN faculty_id BIGINT REFERENCES faculty(id),
    ADD COLUMN cohort_id BIGINT REFERENCES student_cohort(id),
    ADD COLUMN hometown_id BIGINT REFERENCES hometown(id);
UPDATE student_profile s SET faculty_id = f.id FROM faculty f WHERE s.faculty = f.name;
UPDATE student_profile s SET cohort_id = c.id FROM student_cohort c WHERE s.cohort = c.name;
UPDATE student_profile s SET hometown_id = h.id FROM hometown h WHERE s.hometown = h.name;
ALTER TABLE student_profile DROP COLUMN faculty, DROP COLUMN cohort, DROP COLUMN hometown;
ALTER TABLE semester ADD CONSTRAINT fk_semester_academic_year
    FOREIGN KEY (academic_year) REFERENCES academic_year(code);

-- Discover the old fixed enum constraint rather than relying on PostgreSQL's generated name.
DO $$
DECLARE constraint_name TEXT;
BEGIN
    FOR constraint_name IN
        SELECT c.conname FROM pg_constraint c
        WHERE c.conrelid = 'maintenance_request'::regclass AND c.contype = 'c'
          AND c.conkey = ARRAY[(SELECT attnum FROM pg_attribute
              WHERE attrelid = 'maintenance_request'::regclass AND attname = 'category')]::smallint[]
    LOOP
        EXECUTE format('ALTER TABLE maintenance_request DROP CONSTRAINT %I', constraint_name);
    END LOOP;
END $$;
ALTER TABLE maintenance_request ALTER COLUMN category TYPE VARCHAR(30);
ALTER TABLE maintenance_request ADD CONSTRAINT fk_maintenance_category
    FOREIGN KEY (category) REFERENCES maintenance_category(code);

CREATE INDEX ix_faculty_school ON faculty(school_id);
CREATE INDEX ix_hometown_parent ON hometown(parent_id);
CREATE INDEX ix_student_school ON student_profile(school_id);
CREATE INDEX ix_student_faculty ON student_profile(faculty_id);
CREATE INDEX ix_student_cohort ON student_profile(cohort_id);
CREATE INDEX ix_student_hometown ON student_profile(hometown_id);

-- Lookup only by default. ADMIN receives all registered permissions in the registry;
-- STAFF writes remain eligible for explicit assignment through the dynamic matrix.
UPDATE role_permission_matrix
SET permissions = permissions || '["CATALOG_READ"]'::jsonb,
    version = version + 1, updated_at = CURRENT_TIMESTAMP
WHERE NOT permissions @> '["CATALOG_READ"]'::jsonb;
