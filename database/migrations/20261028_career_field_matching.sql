-- Adds optional explicit matching fields while preserving legacy profiles and jobs.
SET @user_career_field_exists = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'career_field'
);
SET @user_career_field_sql = IF(
  @user_career_field_exists = 0,
  'ALTER TABLE users ADD COLUMN career_field VARCHAR(120) NULL',
  'SELECT 1'
);
PREPARE user_career_field_statement FROM @user_career_field_sql;
EXECUTE user_career_field_statement;
DEALLOCATE PREPARE user_career_field_statement;

SET @job_career_field_exists = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'jobs' AND column_name = 'career_field'
);
SET @job_career_field_sql = IF(
  @job_career_field_exists = 0,
  'ALTER TABLE jobs ADD COLUMN career_field VARCHAR(120) NULL',
  'SELECT 1'
);
PREPARE job_career_field_statement FROM @job_career_field_sql;
EXECUTE job_career_field_statement;
DEALLOCATE PREPARE job_career_field_statement;
