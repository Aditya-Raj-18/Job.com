-- Run this once against the existing `jobcom` database in MySQL Workbench.
-- Safe to re-run: each column is added only when it is missing.
USE jobcom;

DELIMITER $$
DROP PROCEDURE IF EXISTS jobcom_add_column_if_missing$$
CREATE PROCEDURE jobcom_add_column_if_missing(IN p_table VARCHAR(64), IN p_column VARCHAR(64), IN p_definition VARCHAR(500))
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = p_table AND column_name = p_column
  ) THEN
    SET @jobcom_ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
    PREPARE jobcom_stmt FROM @jobcom_ddl;
    EXECUTE jobcom_stmt;
    DEALLOCATE PREPARE jobcom_stmt;
  END IF;
END$$
DELIMITER ;

CALL jobcom_add_column_if_missing('users','date_of_birth','DATE NULL');
CALL jobcom_add_column_if_missing('users','address','VARCHAR(300) NULL');
CALL jobcom_add_column_if_missing('users','city','VARCHAR(100) NULL');
CALL jobcom_add_column_if_missing('users','state','VARCHAR(100) NULL');
CALL jobcom_add_column_if_missing('users','postal_code','VARCHAR(20) NULL');
CALL jobcom_add_column_if_missing('users','qualification','VARCHAR(180) NULL');
CALL jobcom_add_column_if_missing('users','experience_years','DECIMAL(5,2) NULL');
CALL jobcom_add_column_if_missing('users','skills','TEXT NULL');
CALL jobcom_add_column_if_missing('users','profile_headline','VARCHAR(220) NULL');
CALL jobcom_add_column_if_missing('users','profile_summary','TEXT NULL');
CALL jobcom_add_column_if_missing('users','linkedin_url','VARCHAR(500) NULL');
CALL jobcom_add_column_if_missing('users','portfolio_url','VARCHAR(500) NULL');
CALL jobcom_add_column_if_missing('users','resume_path','VARCHAR(500) NULL');
CALL jobcom_add_column_if_missing('users','government_id_type','VARCHAR(80) NULL');
CALL jobcom_add_column_if_missing('users','government_id_last4','CHAR(4) NULL');
CALL jobcom_add_column_if_missing('users','government_id_document_path','VARCHAR(500) NULL');
CALL jobcom_add_column_if_missing('users','id_verification_status','ENUM(''PENDING'',''APPROVED'',''REJECTED'') NOT NULL DEFAULT ''PENDING''');

CALL jobcom_add_column_if_missing('companies','password_hash','VARCHAR(100) NULL');
CALL jobcom_add_column_if_missing('companies','legal_name','VARCHAR(180) NULL');
CALL jobcom_add_column_if_missing('companies','contact_person','VARCHAR(160) NULL');
CALL jobcom_add_column_if_missing('companies','contact_title','VARCHAR(100) NULL');
CALL jobcom_add_column_if_missing('companies','phone','VARCHAR(40) NULL');
CALL jobcom_add_column_if_missing('companies','website','VARCHAR(500) NULL');
CALL jobcom_add_column_if_missing('companies','industry','VARCHAR(120) NULL');
CALL jobcom_add_column_if_missing('companies','company_size','VARCHAR(60) NULL');
CALL jobcom_add_column_if_missing('companies','license_number','VARCHAR(100) NULL');
CALL jobcom_add_column_if_missing('companies','license_type','VARCHAR(100) NULL');
CALL jobcom_add_column_if_missing('companies','license_document_path','VARCHAR(500) NULL');
CALL jobcom_add_column_if_missing('companies','company_registration_number','VARCHAR(100) NULL');
CALL jobcom_add_column_if_missing('companies','tax_registration_number','VARCHAR(100) NULL');
CALL jobcom_add_column_if_missing('companies','address','VARCHAR(300) NULL');
CALL jobcom_add_column_if_missing('companies','city','VARCHAR(100) NULL');
CALL jobcom_add_column_if_missing('companies','state','VARCHAR(100) NULL');
CALL jobcom_add_column_if_missing('companies','country','VARCHAR(100) NULL');
CALL jobcom_add_column_if_missing('companies','postal_code','VARCHAR(20) NULL');
CALL jobcom_add_column_if_missing('companies','verification_status','ENUM(''PENDING'',''APPROVED'',''REJECTED'') NOT NULL DEFAULT ''PENDING''');

DROP PROCEDURE jobcom_add_column_if_missing;
