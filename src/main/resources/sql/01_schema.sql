DROP DATABASE IF EXISTS emc_management_system;
CREATE DATABASE emc_management_system
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE emc_management_system;

CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    login VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('ADMIN', 'VE', 'TE', 'TT') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE projects (
    id INT AUTO_INCREMENT PRIMARY KEY,
    ewr_number VARCHAR(20) NOT NULL UNIQUE,
    brand VARCHAR(100) NOT NULL,
    device_name VARCHAR(150) NOT NULL,
    short_description VARCHAR(255),
    ve_id INT NOT NULL,
    te_id INT NOT NULL,
    start_date DATE NULL,
    end_date DATE NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_projects_ve FOREIGN KEY (ve_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_projects_te FOREIGN KEY (te_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE project_tt_assignments (
    project_id INT NOT NULL,
    tt_user_id INT NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (project_id, tt_user_id),
    CONSTRAINT fk_project_tt_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_project_tt_user FOREIGN KEY (tt_user_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE project_legs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    project_id INT NOT NULL,
    leg_code VARCHAR(20) NOT NULL,
    test_type ENUM('ESD', 'EMC', 'FT', 'ELE') NOT NULL,
    accreditation ENUM('YES', 'NO') NOT NULL DEFAULT 'NO',
    start_date DATE NULL,
    end_date DATE NULL,
    assigned_tt_id INT NULL,
    iso_standard_name VARCHAR(150) NULL,
    iso_file_path VARCHAR(255) NULL,
    iso_file_name VARCHAR(255) NULL,
    iso_file_data LONGBLOB NULL,
    client_standard_name VARCHAR(150) NULL,
    client_file_path VARCHAR(255) NULL,
    client_file_name VARCHAR(255) NULL,
    client_file_data LONGBLOB NULL,
    test_plan_name VARCHAR(150) NULL,
    test_plan_file_path VARCHAR(255) NULL,
    test_plan_file_name VARCHAR(255) NULL,
    test_plan_file_data LONGBLOB NULL,
    pca_url VARCHAR(500) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_project_leg UNIQUE (project_id, leg_code),
    CONSTRAINT fk_project_legs_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_project_legs_tt FOREIGN KEY (assigned_tt_id) REFERENCES users(id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE leg_test_steps (
    id INT AUTO_INCREMENT PRIMARY KEY,
    leg_id INT NOT NULL,
    step_order INT NOT NULL,
    step_name VARCHAR(150) NOT NULL,
    status ENUM('NOT_STARTED', 'ONGOING', 'DATA_IN_ANALYSIS', 'PASSED', 'FAILED') NOT NULL DEFAULT 'NOT_STARTED',
    start_date DATE NULL,
    end_date DATE NULL,
    test_room VARCHAR(100) NULL,
    CONSTRAINT uq_leg_step_order UNIQUE (leg_id, step_order),
    CONSTRAINT fk_leg_test_steps_leg FOREIGN KEY (leg_id) REFERENCES project_legs(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE dut_samples (
    id INT AUTO_INCREMENT PRIMARY KEY,
    project_id INT NOT NULL,
    sample_code VARCHAR(30) NOT NULL,
    serial_number VARCHAR(12) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_project_sample UNIQUE (project_id, sample_code),
    CONSTRAINT fk_dut_samples_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE leg_dut_assignments (
    leg_id INT NOT NULL,
    dut_sample_id INT NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (leg_id, dut_sample_id),
    CONSTRAINT fk_leg_dut_assignments_leg FOREIGN KEY (leg_id) REFERENCES project_legs(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_leg_dut_assignments_dut FOREIGN KEY (dut_sample_id) REFERENCES dut_samples(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE step_dut_assignments (
    leg_test_step_id INT NOT NULL,
    dut_sample_id INT NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (leg_test_step_id, dut_sample_id),
    CONSTRAINT fk_step_dut_assignments_step FOREIGN KEY (leg_test_step_id) REFERENCES leg_test_steps(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_step_dut_assignments_dut FOREIGN KEY (dut_sample_id) REFERENCES dut_samples(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE dut_test_results (
    id INT AUTO_INCREMENT PRIMARY KEY,
    dut_sample_id INT NOT NULL,
    leg_test_step_id INT NOT NULL,
    condition_status ENUM('OK', 'NOK') NOT NULL DEFAULT 'OK',
    observed_functional_class ENUM('CLASS_A', 'CLASS_B', 'CLASS_C', 'CLASS_D', 'CLASS_E') NOT NULL DEFAULT 'CLASS_A',
    result_status ENUM('NOT_STARTED', 'ONGOING', 'DATA_IN_ANALYSIS', 'PASSED', 'FAILED') NOT NULL DEFAULT 'NOT_STARTED',
    execution_date DATE NULL,
    comment VARCHAR(500) NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_dut_step_result UNIQUE (dut_sample_id, leg_test_step_id),
    CONSTRAINT fk_dut_test_results_dut FOREIGN KEY (dut_sample_id) REFERENCES dut_samples(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_dut_test_results_step FOREIGN KEY (leg_test_step_id) REFERENCES leg_test_steps(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE measurement_equipment (
    id INT AUTO_INCREMENT PRIMARY KEY,
    equipment_code VARCHAR(40) NOT NULL UNIQUE,
    equipment_name VARCHAR(150) NOT NULL,
    category VARCHAR(100) NOT NULL,
    manufacturer VARCHAR(100) NULL,
    model VARCHAR(100) NULL,
    serial_number VARCHAR(100) NULL,
    calibration_valid_until DATE NULL,
    lab_owned ENUM('YES', 'NO') NOT NULL DEFAULT 'YES',
    location VARCHAR(100) NULL,
    notes VARCHAR(500) NULL,
    climate_sensor_code VARCHAR(120) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE leg_equipment_assignments (
    leg_id INT NOT NULL,
    equipment_id INT NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (leg_id, equipment_id),
    CONSTRAINT fk_leg_equipment_assignments_leg FOREIGN KEY (leg_id) REFERENCES project_legs(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_leg_equipment_assignments_equipment FOREIGN KEY (equipment_id) REFERENCES measurement_equipment(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE step_equipment_assignments (
    leg_test_step_id INT NOT NULL,
    equipment_id INT NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (leg_test_step_id, equipment_id),
    CONSTRAINT fk_step_equipment_assignments_step FOREIGN KEY (leg_test_step_id) REFERENCES leg_test_steps(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_step_equipment_assignments_equipment FOREIGN KEY (equipment_id) REFERENCES measurement_equipment(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE climate_log_files (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sensor_code VARCHAR(120) NOT NULL,
    year_number INT NOT NULL,
    calendar_week INT NOT NULL,
    room_code VARCHAR(100) NOT NULL,
    source_filename VARCHAR(255) NOT NULL UNIQUE,
    file_content LONGTEXT NOT NULL,
    imported_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE leg_test_step_media (
    id INT AUTO_INCREMENT PRIMARY KEY,
    leg_test_step_id INT NOT NULL,
    media_kind ENUM('SETUP', 'VERIFICATION') NOT NULL,
    slot_code VARCHAR(50) NOT NULL,
    display_name VARCHAR(150) NOT NULL,
    sort_order INT NOT NULL DEFAULT 1,
    file_name VARCHAR(255) NOT NULL,
    file_data LONGBLOB NOT NULL,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_step_media UNIQUE (leg_test_step_id, media_kind, slot_code, sort_order),
    CONSTRAINT fk_leg_test_step_media_step FOREIGN KEY (leg_test_step_id) REFERENCES leg_test_steps(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE dut_media (
    id INT AUTO_INCREMENT PRIMARY KEY,
    dut_sample_id INT NOT NULL,
    media_type ENUM('FRONT_VIEW', 'BACK_VIEW', 'CONNECTOR_VIEW', 'LABEL') NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_data LONGBLOB NOT NULL,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_dut_media_type UNIQUE (dut_sample_id, media_type),
    CONSTRAINT fk_dut_media_sample FOREIGN KEY (dut_sample_id) REFERENCES dut_samples(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE audit_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    actor_user_id INT NULL,
    actor_name VARCHAR(150) NOT NULL,
    actor_role VARCHAR(50) NOT NULL,
    action_type VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id INT NULL,
    project_id INT NULL,
    leg_id INT NULL,
    step_id INT NULL,
    summary VARCHAR(255) NOT NULL,
    details TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_log_actor FOREIGN KEY (actor_user_id) REFERENCES users(id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_audit_log_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_audit_log_leg FOREIGN KEY (leg_id) REFERENCES project_legs(id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_audit_log_step FOREIGN KEY (step_id) REFERENCES leg_test_steps(id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_projects_ewr_number ON projects(ewr_number);
CREATE INDEX idx_project_legs_project_id ON project_legs(project_id);
CREATE INDEX idx_project_legs_assigned_tt_id ON project_legs(assigned_tt_id);
CREATE INDEX idx_leg_test_steps_leg_id ON leg_test_steps(leg_id);
CREATE INDEX idx_leg_test_steps_status ON leg_test_steps(status);
CREATE INDEX idx_leg_test_steps_room ON leg_test_steps(test_room);
CREATE INDEX idx_dut_samples_project_id ON dut_samples(project_id);
CREATE INDEX idx_dut_samples_serial_number ON dut_samples(serial_number);
CREATE INDEX idx_leg_dut_assignments_leg_id ON leg_dut_assignments(leg_id);
CREATE INDEX idx_leg_dut_assignments_dut_id ON leg_dut_assignments(dut_sample_id);
CREATE INDEX idx_step_dut_assignments_step_id ON step_dut_assignments(leg_test_step_id);
CREATE INDEX idx_step_dut_assignments_dut_id ON step_dut_assignments(dut_sample_id);
CREATE INDEX idx_dut_test_results_step_id ON dut_test_results(leg_test_step_id);
CREATE INDEX idx_dut_test_results_status ON dut_test_results(result_status);
CREATE INDEX idx_measurement_equipment_category ON measurement_equipment(category);
CREATE INDEX idx_leg_equipment_assignments_leg_id ON leg_equipment_assignments(leg_id);
CREATE INDEX idx_leg_equipment_assignments_equipment_id ON leg_equipment_assignments(equipment_id);
CREATE INDEX idx_step_equipment_assignments_step_id ON step_equipment_assignments(leg_test_step_id);
CREATE INDEX idx_step_equipment_assignments_equipment_id ON step_equipment_assignments(equipment_id);
CREATE INDEX idx_climate_log_files_lookup ON climate_log_files(room_code, year_number, calendar_week);
CREATE INDEX idx_leg_test_step_media_step ON leg_test_step_media(leg_test_step_id, media_kind, sort_order);
CREATE INDEX idx_dut_media_sample ON dut_media(dut_sample_id, media_type);
CREATE INDEX idx_audit_log_project ON audit_log(project_id, created_at);
CREATE INDEX idx_audit_log_leg ON audit_log(leg_id, created_at);
CREATE INDEX idx_audit_log_actor ON audit_log(actor_user_id, created_at);

CREATE OR REPLACE VIEW vw_leg_status AS
SELECT
    pl.id AS leg_id,
    pl.project_id,
    pl.leg_code,
    CASE
        WHEN COUNT(lts.id) = 0 THEN 'NOT_STARTED'
        WHEN SUM(lts.status = 'NOT_STARTED') = COUNT(lts.id) THEN 'NOT_STARTED'
        WHEN SUM(lts.status = 'ONGOING') > 0 THEN 'ONGOING'
        WHEN SUM(lts.status = 'DATA_IN_ANALYSIS') > 0 AND SUM(lts.status = 'ONGOING') = 0 THEN 'DATA_IN_ANALYSIS'
        WHEN SUM(lts.status = 'PASSED') = COUNT(lts.id) THEN 'PASSED'
        WHEN SUM(lts.status IN ('PASSED', 'FAILED')) = COUNT(lts.id) AND SUM(lts.status = 'FAILED') > 0 THEN 'FAILED'
        ELSE 'ONGOING'
    END AS leg_status
FROM project_legs pl
LEFT JOIN leg_test_steps lts ON lts.leg_id = pl.id
GROUP BY pl.id, pl.project_id, pl.leg_code;

CREATE OR REPLACE VIEW vw_project_status AS
SELECT
    p.id AS project_id,
    p.ewr_number,
    CASE
        WHEN COUNT(vls.leg_id) = 0 THEN 'NOT_STARTED'
        WHEN SUM(vls.leg_status = 'NOT_STARTED') = COUNT(vls.leg_id) THEN 'NOT_STARTED'
        WHEN SUM(vls.leg_status = 'ONGOING') > 0 THEN 'ONGOING'
        WHEN SUM(vls.leg_status = 'DATA_IN_ANALYSIS') > 0 AND SUM(vls.leg_status = 'ONGOING') = 0 THEN 'DATA_IN_ANALYSIS'
        WHEN SUM(vls.leg_status = 'PASSED') = COUNT(vls.leg_id) THEN 'PASSED'
        WHEN SUM(vls.leg_status IN ('PASSED', 'FAILED')) = COUNT(vls.leg_id) AND SUM(vls.leg_status = 'FAILED') > 0 THEN 'FAILED'
        ELSE 'ONGOING'
    END AS project_status
FROM projects p
LEFT JOIN vw_leg_status vls ON vls.project_id = p.id
GROUP BY p.id, p.ewr_number;

CREATE OR REPLACE VIEW vw_leg_dut_count AS
SELECT
    pl.id AS leg_id,
    COUNT(DISTINCT lda.dut_sample_id) AS dut_count
FROM project_legs pl
LEFT JOIN leg_dut_assignments lda ON lda.leg_id = pl.id
GROUP BY pl.id;

CREATE OR REPLACE VIEW vw_equipment_reservations AS
SELECT
    p.id AS project_id,
    p.ewr_number,
    pl.id AS leg_id,
    pl.leg_code,
    lts.id AS step_id,
    lts.step_order,
    lts.step_name,
    lts.status,
    lts.test_room AS room_code,
    me.id AS equipment_id,
    me.equipment_code,
    me.equipment_name,
    me.category,
    COALESCE(lts.start_date, pl.start_date) AS reserved_from,
    COALESCE(lts.end_date, pl.end_date) AS reserved_to
FROM step_equipment_assignments sea
JOIN leg_test_steps lts ON lts.id = sea.leg_test_step_id
JOIN project_legs pl ON pl.id = lts.leg_id
JOIN projects p ON p.id = pl.project_id
JOIN measurement_equipment me ON me.id = sea.equipment_id
WHERE COALESCE(lts.start_date, pl.start_date) IS NOT NULL
  AND COALESCE(lts.end_date, pl.end_date) IS NOT NULL;
