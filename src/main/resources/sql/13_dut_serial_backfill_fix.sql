USE emc_management_system;

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
WHERE serial_number IS NULL
   OR TRIM(serial_number) = ''
   OR serial_number REGEXP '^SNX+$';
