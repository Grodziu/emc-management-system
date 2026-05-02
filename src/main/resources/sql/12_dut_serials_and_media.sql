USE emc_management_system;

ALTER TABLE dut_samples
    ADD COLUMN IF NOT EXISTS serial_number VARCHAR(12) NULL AFTER sample_code;

UPDATE dut_samples
SET serial_number = CONCAT(
    'SN',
    UPPER(
        LEFT(
            CONCAT(
                REPLACE(REPLACE(REPLACE(sample_code, '-', ''), '_', ''), ' ', ''),
                'XXXXXXXXXX'
            ),
            10
        )
    )
)
WHERE serial_number IS NULL OR TRIM(serial_number) = '';

ALTER TABLE dut_samples
    MODIFY COLUMN serial_number VARCHAR(12) NOT NULL;

CREATE INDEX idx_dut_samples_serial_number ON dut_samples(serial_number);

CREATE TABLE IF NOT EXISTS leg_test_step_media (
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

CREATE INDEX idx_leg_test_step_media_step ON leg_test_step_media(leg_test_step_id, media_kind, sort_order);

CREATE TABLE IF NOT EXISTS dut_media (
    id INT AUTO_INCREMENT PRIMARY KEY,
    dut_sample_id INT NOT NULL,
    media_type ENUM('FRONT_VIEW', 'BACK_VIEW', 'CONNECTOR_VIEW', 'LABEL') NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_data LONGBLOB NOT NULL,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_dut_media_type UNIQUE (dut_sample_id, media_type),
    CONSTRAINT fk_dut_media_sample FOREIGN KEY (dut_sample_id) REFERENCES dut_samples(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_dut_media_sample ON dut_media(dut_sample_id, media_type);
