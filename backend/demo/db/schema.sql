CREATE DATABASE IF NOT EXISTS finance_eval
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE finance_eval;

CREATE TABLE IF NOT EXISTS evaluation_records (
    id BIGINT NOT NULL AUTO_INCREMENT,
    evaluation_type VARCHAR(20) NOT NULL,
    target_name VARCHAR(255) NOT NULL,
    present_value DECIMAL(18,4) NULL,
    risk_metric_name VARCHAR(50) NULL,
    risk_metric_value DECIMAL(18,4) NULL,
    duration_value DECIMAL(18,4) NULL,
    convexity DECIMAL(18,4) NULL,
    npv DECIMAL(18,4) NULL,
    irr DECIMAL(18,4) NULL,
    payback_period DECIMAL(18,4) NULL,
    risk_grade VARCHAR(20) NOT NULL,
    model_explanation VARCHAR(1000) NOT NULL,
    commentary VARCHAR(500) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

ALTER TABLE evaluation_records
    ADD COLUMN IF NOT EXISTS risk_metric_name VARCHAR(50) NULL AFTER present_value,
    ADD COLUMN IF NOT EXISTS risk_metric_value DECIMAL(18,4) NULL AFTER risk_metric_name,
    ADD COLUMN IF NOT EXISTS model_explanation VARCHAR(1000) NULL AFTER risk_grade,
    ADD COLUMN IF NOT EXISTS commentary VARCHAR(500) NULL AFTER model_explanation;

ALTER TABLE evaluation_records
    MODIFY COLUMN model_explanation VARCHAR(1000) NOT NULL,
    MODIFY COLUMN commentary VARCHAR(500) NOT NULL;

ALTER TABLE evaluation_records
    DROP COLUMN IF EXISTS var95,
    DROP COLUMN IF EXISTS summary;
