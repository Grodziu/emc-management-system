ALTER TABLE dut_test_results
    ADD COLUMN IF NOT EXISTS observed_functional_class ENUM('CLASS_A', 'CLASS_B', 'CLASS_C', 'CLASS_D', 'CLASS_E') NOT NULL DEFAULT 'CLASS_A' AFTER condition_status,
    ADD COLUMN IF NOT EXISTS execution_date DATE NULL AFTER result_status;

UPDATE dut_test_results
SET observed_functional_class = CASE
    WHEN condition_status = 'NOK' THEN 'CLASS_C'
    ELSE 'CLASS_A'
END
WHERE observed_functional_class IS NULL
   OR observed_functional_class = 'CLASS_A';

ALTER TABLE measurement_equipment
    ADD COLUMN IF NOT EXISTS lab_owned ENUM('YES', 'NO') NOT NULL DEFAULT 'YES' AFTER calibration_valid_until;

UPDATE measurement_equipment
SET lab_owned = 'YES'
WHERE lab_owned IS NULL;
