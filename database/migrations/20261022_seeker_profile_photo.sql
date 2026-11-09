-- Adds the path for profile photos. Safe to apply more than once.
SET @profile_photo_column_exists = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'users'
    AND column_name = 'profile_photo_path'
);
SET @profile_photo_sql = IF(
  @profile_photo_column_exists = 0,
  'ALTER TABLE users ADD COLUMN profile_photo_path VARCHAR(500) NULL',
  'SELECT 1'
);
PREPARE profile_photo_statement FROM @profile_photo_sql;
EXECUTE profile_photo_statement;
DEALLOCATE PREPARE profile_photo_statement;
