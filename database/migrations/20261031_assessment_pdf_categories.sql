-- Organize admin-uploaded assessment PDFs by company and job role.
USE jobcom;

SET @assessment_company_exists = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'assessments' AND column_name = 'company_name'
);
SET @assessment_company_sql = IF(
  @assessment_company_exists = 0,
  'ALTER TABLE assessments ADD COLUMN company_name VARCHAR(120) NOT NULL DEFAULT ''General''',
  'SELECT 1'
);
PREPARE assessment_company_statement FROM @assessment_company_sql;
EXECUTE assessment_company_statement;
DEALLOCATE PREPARE assessment_company_statement;

SET @assessment_role_exists = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'assessments' AND column_name = 'role_name'
);
SET @assessment_role_sql = IF(
  @assessment_role_exists = 0,
  'ALTER TABLE assessments ADD COLUMN role_name VARCHAR(120) NOT NULL DEFAULT ''General''',
  'SELECT 1'
);
PREPARE assessment_role_statement FROM @assessment_role_sql;
EXECUTE assessment_role_statement;
DEALLOCATE PREPARE assessment_role_statement;
