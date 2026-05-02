USE emc_management_system;

ALTER TABLE leg_test_steps
    ADD COLUMN IF NOT EXISTS test_room VARCHAR(100) NULL AFTER end_date;

CREATE TABLE IF NOT EXISTS step_dut_assignments (
    leg_test_step_id INT NOT NULL,
    dut_sample_id INT NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (leg_test_step_id, dut_sample_id),
    CONSTRAINT fk_step_dut_assignments_step FOREIGN KEY (leg_test_step_id) REFERENCES leg_test_steps(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_step_dut_assignments_dut FOREIGN KEY (dut_sample_id) REFERENCES dut_samples(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS climate_log_files (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sensor_code VARCHAR(120) NOT NULL,
    year_number INT NOT NULL,
    calendar_week INT NOT NULL,
    room_code VARCHAR(100) NOT NULL,
    source_filename VARCHAR(255) NOT NULL UNIQUE,
    file_content LONGTEXT NOT NULL,
    imported_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

UPDATE leg_test_steps lts
JOIN (
    SELECT sea.leg_test_step_id,
           REPLACE(MAX(me.location), ' ', '') AS inferred_room
    FROM step_equipment_assignments sea
    JOIN measurement_equipment me ON me.id = sea.equipment_id
    GROUP BY sea.leg_test_step_id
) inferred ON inferred.leg_test_step_id = lts.id
SET lts.test_room = COALESCE(lts.test_room, inferred.inferred_room);
