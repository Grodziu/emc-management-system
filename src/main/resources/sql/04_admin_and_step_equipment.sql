USE emc_management_system;

ALTER TABLE users
    MODIFY role ENUM('ADMIN', 'VE', 'TE', 'TT') NOT NULL;

CREATE TABLE IF NOT EXISTS step_equipment_assignments (
    leg_test_step_id INT NOT NULL,
    equipment_id INT NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (leg_test_step_id, equipment_id),
    CONSTRAINT fk_step_equipment_assignments_step FOREIGN KEY (leg_test_step_id) REFERENCES leg_test_steps(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_step_equipment_assignments_equipment FOREIGN KEY (equipment_id) REFERENCES measurement_equipment(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

INSERT IGNORE INTO users (first_name, last_name, email, login, password, role)
VALUES ('System', 'Administrator', 'admin@walidacja.com', 'admin', 'admin123', 'ADMIN');

INSERT IGNORE INTO step_equipment_assignments (leg_test_step_id, equipment_id)
SELECT lts.id, lea.equipment_id
FROM leg_equipment_assignments lea
JOIN leg_test_steps lts ON lts.leg_id = lea.leg_id;
