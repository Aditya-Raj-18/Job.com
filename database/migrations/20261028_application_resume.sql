USE jobcom;

SET @has_resume_path = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'applications' AND column_name = 'resume_path'
);
SET @ddl = IF(@has_resume_path = 0,
  'ALTER TABLE applications ADD COLUMN resume_path VARCHAR(500) NULL',
  'SELECT 1');
PREPARE jobcom_stmt FROM @ddl;
EXECUTE jobcom_stmt;
DEALLOCATE PREPARE jobcom_stmt;