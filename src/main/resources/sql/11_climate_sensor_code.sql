USE emc_management_system;

ALTER TABLE measurement_equipment
    ADD COLUMN IF NOT EXISTS climate_sensor_code VARCHAR(120) NULL AFTER notes;
