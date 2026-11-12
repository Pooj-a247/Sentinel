-- Sentinel schema compatible with the current Spring Boot controller.
-- Safe to run on a fresh database. CREATE TABLE IF NOT EXISTS does not migrate existing tables.
CREATE DATABASE IF NOT EXISTS sentinel CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE sentinel;

CREATE TABLE IF NOT EXISTS users (
  user_id VARCHAR(50) NOT NULL PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  full_name VARCHAR(120) NOT NULL,
  username VARCHAR(60) NOT NULL UNIQUE,
  email VARCHAR(180) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  role VARCHAR(30) NOT NULL DEFAULT 'INVESTIGATOR',
  department VARCHAR(120) NOT NULL DEFAULT '',
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS cases (
  case_id VARCHAR(100) NOT NULL PRIMARY KEY,
  title VARCHAR(180) NOT NULL,
  description TEXT NULL,
  created_by VARCHAR(50) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_cases_creator FOREIGN KEY (created_by) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS evidence (
  evidence_id VARCHAR(50) NOT NULL PRIMARY KEY,
  case_id VARCHAR(100) NOT NULL,
  name VARCHAR(255) NOT NULL,
  type VARCHAR(100) NOT NULL DEFAULT 'OTHER',
  original_hash CHAR(64) NOT NULL,
  verified BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  uploaded_by VARCHAR(50) NULL,
  file_path VARCHAR(1000) NULL,
  status VARCHAR(40) NOT NULL DEFAULT 'REGISTERED',
  last_verified_at TIMESTAMP NULL,
  INDEX idx_evidence_case (case_id),
  INDEX idx_evidence_status (status),
  CONSTRAINT fk_evidence_case FOREIGN KEY (case_id) REFERENCES cases(case_id),
  CONSTRAINT fk_evidence_uploader FOREIGN KEY (uploaded_by) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS custody_events (
  event_id VARCHAR(36) NOT NULL PRIMARY KEY,
  evidence_id VARCHAR(50) NOT NULL,
  actor_user_id VARCHAR(50) NULL,
  from_user_id VARCHAR(50) NULL,
  to_user_id VARCHAR(50) NULL,
  event_type VARCHAR(60) NOT NULL,
  notes TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_custody_evidence_time (evidence_id, created_at),
  CONSTRAINT fk_custody_evidence FOREIGN KEY (evidence_id) REFERENCES evidence(evidence_id),
  CONSTRAINT fk_custody_actor FOREIGN KEY (actor_user_id) REFERENCES users(user_id),
  CONSTRAINT fk_custody_from FOREIGN KEY (from_user_id) REFERENCES users(user_id),
  CONSTRAINT fk_custody_to FOREIGN KEY (to_user_id) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS transfers (
  transfer_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  evidence_id VARCHAR(50) NOT NULL,
  from_user VARCHAR(50) NOT NULL,
  to_user VARCHAR(50) NOT NULL,
  remarks TEXT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  responded_at TIMESTAMP NULL DEFAULT NULL,
  INDEX idx_transfers_status (status),
  INDEX idx_transfers_from_user (from_user),
  INDEX idx_transfers_to_user (to_user),
  CONSTRAINT fk_transfers_evidence FOREIGN KEY (evidence_id) REFERENCES evidence(evidence_id),
  CONSTRAINT fk_transfers_from_user FOREIGN KEY (from_user) REFERENCES users(user_id),
  CONSTRAINT fk_transfers_to_user FOREIGN KEY (to_user) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS alerts (
  alert_id VARCHAR(36) NOT NULL PRIMARY KEY,
  evidence_id VARCHAR(50) NOT NULL,
  message TEXT NOT NULL,
  severity VARCHAR(20) NOT NULL DEFAULT 'HIGH',
  resolved BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_alerts_open (resolved, created_at),
  CONSTRAINT fk_alerts_evidence FOREIGN KEY (evidence_id) REFERENCES evidence(evidence_id)
);

