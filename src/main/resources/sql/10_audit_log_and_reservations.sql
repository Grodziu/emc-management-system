CREATE TABLE IF NOT EXISTS audit_log (
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

CREATE INDEX idx_audit_log_project ON audit_log(project_id, created_at);
CREATE INDEX idx_audit_log_leg ON audit_log(leg_id, created_at);
CREATE INDEX idx_audit_log_actor ON audit_log(actor_user_id, created_at);

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
