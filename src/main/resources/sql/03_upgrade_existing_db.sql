USE emc_management_system;

CREATE TABLE IF NOT EXISTS leg_dut_assignments (
    leg_id INT NOT NULL,
    dut_sample_id INT NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (leg_id, dut_sample_id),
    CONSTRAINT fk_leg_dut_assignments_leg FOREIGN KEY (leg_id) REFERENCES project_legs(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_leg_dut_assignments_dut FOREIGN KEY (dut_sample_id) REFERENCES dut_samples(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS measurement_equipment (
    id INT AUTO_INCREMENT PRIMARY KEY,
    equipment_code VARCHAR(40) NOT NULL UNIQUE,
    equipment_name VARCHAR(150) NOT NULL,
    category VARCHAR(100) NOT NULL,
    manufacturer VARCHAR(100) NULL,
    model VARCHAR(100) NULL,
    serial_number VARCHAR(100) NULL,
    calibration_valid_until DATE NULL,
    location VARCHAR(100) NULL,
    notes VARCHAR(500) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS leg_equipment_assignments (
    leg_id INT NOT NULL,
    equipment_id INT NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (leg_id, equipment_id),
    CONSTRAINT fk_leg_equipment_assignments_leg FOREIGN KEY (leg_id) REFERENCES project_legs(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_leg_equipment_assignments_equipment FOREIGN KEY (equipment_id) REFERENCES measurement_equipment(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE OR REPLACE VIEW vw_leg_dut_count AS
SELECT
    pl.id AS leg_id,
    COUNT(DISTINCT lda.dut_sample_id) AS dut_count
FROM project_legs pl
LEFT JOIN leg_dut_assignments lda ON lda.leg_id = pl.id
GROUP BY pl.id;

INSERT IGNORE INTO leg_dut_assignments (leg_id, dut_sample_id)
SELECT DISTINCT lts.leg_id, dtr.dut_sample_id
FROM dut_test_results dtr
JOIN leg_test_steps lts ON lts.id = dtr.leg_test_step_id;

INSERT IGNORE INTO leg_dut_assignments (leg_id, dut_sample_id)
SELECT pl.id, ds.id
FROM project_legs pl
JOIN projects p ON p.id = pl.project_id
JOIN dut_samples ds ON ds.project_id = p.id
WHERE p.ewr_number = 'EWR103612'
  AND pl.leg_code = 'LEG_00B';

INSERT IGNORE INTO measurement_equipment (
    equipment_code, equipment_name, category, manufacturer, model, serial_number, calibration_valid_until, location, notes
) VALUES
('EQ-RF-001', 'EMI Receiver', 'RF', 'Rohde & Schwarz', 'ESR7', 'RS-ESR7-001', '2026-12-31', 'Lab A', 'Main receiver for radiated EMC tests'),
('EQ-AMP-002', 'RF Power Amplifier', 'RF', 'Amplifier Research', '150W1000B', 'AR-150W-002', '2026-11-15', 'Lab A', 'Used for immunity tests'),
('EQ-ESD-003', 'ESD Simulator', 'ESD', 'EM Test', 'DITO', 'EMT-DITO-003', '2026-10-20', 'Lab B', 'Gun for contact and air discharge'),
('EQ-LISN-004', 'LISN', 'Conducted', 'Teseq', 'NNBL 8227', 'TSQ-LISN-004', '2026-09-10', 'Lab C', 'Line impedance stabilization network'),
('EQ-OSC-005', 'Oscilloscope', 'Measurement', 'Tektronix', 'MSO64', 'TEK-MSO64-005', '2026-08-30', 'Lab C', 'General purpose analysis scope'),
('EQ-PSU-006', 'Programmable Power Supply', 'Power', 'Keysight', 'N7900', 'KEY-N7900-006', '2026-12-05', 'Lab D', 'Supply for electrical tests');

INSERT IGNORE INTO leg_equipment_assignments (leg_id, equipment_id)
SELECT pl.id, me.id
FROM project_legs pl
JOIN measurement_equipment me ON me.equipment_code IN ('EQ-RF-001', 'EQ-AMP-002')
WHERE pl.leg_code IN ('LEG_00A', 'LEG_00C')
  AND pl.project_id IN (1, 2, 3);

INSERT IGNORE INTO leg_equipment_assignments (leg_id, equipment_id)
SELECT pl.id, me.id
FROM project_legs pl
JOIN measurement_equipment me ON me.equipment_code = 'EQ-ESD-003'
WHERE pl.test_type = 'ESD';

INSERT IGNORE INTO leg_equipment_assignments (leg_id, equipment_id)
SELECT pl.id, me.id
FROM project_legs pl
JOIN measurement_equipment me ON me.equipment_code IN ('EQ-LISN-004', 'EQ-OSC-005', 'EQ-PSU-006')
WHERE pl.test_type IN ('FT', 'ELE');
