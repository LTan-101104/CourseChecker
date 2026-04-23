UPDATE app_user
SET password_hash = '$2y$10$a5JXny/t0KiauslKMUo2VuC/ZrJB83KToqInMWO1G8COowPA9tAhW'
WHERE password_hash IS NULL OR btrim(password_hash) = '';

ALTER TABLE app_user
    ALTER COLUMN password_hash SET NOT NULL;
