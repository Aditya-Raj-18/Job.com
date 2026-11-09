USE jobcom;

SET @has_job_type_id = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'jobs' AND column_name = 'job_type_id'
);
SET @ddl = IF(@has_job_type_id = 0,
  'ALTER TABLE jobs ADD COLUMN job_type_id BIGINT NULL',
  'SELECT 1');
PREPARE jobcom_stmt FROM @ddl;
EXECUTE jobcom_stmt;
DEALLOCATE PREPARE jobcom_stmt;

SET @has_package_ctc = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'jobs' AND column_name = 'package_ctc'
);
SET @ddl = IF(@has_package_ctc = 0,
  'ALTER TABLE jobs ADD COLUMN package_ctc VARCHAR(180) NULL AFTER job_type_id',
  'SELECT 1');
PREPARE jobcom_stmt FROM @ddl;
EXECUTE jobcom_stmt;
DEALLOCATE PREPARE jobcom_stmt;

INSERT IGNORE INTO job_types(name) VALUES ('Full-time'), ('Internship');

CREATE TABLE IF NOT EXISTS applications (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  job_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  status VARCHAR(40) NOT NULL DEFAULT 'APPLIED',
  applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX(job_id),
  INDEX(user_id),
  FOREIGN KEY (job_id) REFERENCES jobs(id),
  FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS application_messages (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  application_id BIGINT NOT NULL,
  sender_type ENUM('SEEKER','RECRUITER') NOT NULL,
  sender_id BIGINT NOT NULL,
  body VARCHAR(4000) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX(application_id, created_at)
) ENGINE=InnoDB;
