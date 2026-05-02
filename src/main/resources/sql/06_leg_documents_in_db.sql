USE emc_management_system;

ALTER TABLE project_legs
    ADD COLUMN iso_file_name VARCHAR(255) NULL AFTER iso_file_path,
    ADD COLUMN iso_file_data LONGBLOB NULL AFTER iso_file_name,
    ADD COLUMN client_file_name VARCHAR(255) NULL AFTER client_file_path,
    ADD COLUMN client_file_data LONGBLOB NULL AFTER client_file_name,
    ADD COLUMN test_plan_file_name VARCHAR(255) NULL AFTER test_plan_file_path,
    ADD COLUMN test_plan_file_data LONGBLOB NULL AFTER test_plan_file_name;

UPDATE project_legs
SET
    iso_file_name = COALESCE(
        NULLIF(TRIM(iso_file_name), ''),
        CASE
            WHEN NULLIF(TRIM(iso_file_path), '') IS NOT NULL THEN SUBSTRING_INDEX(REPLACE(iso_file_path, '\\', '/'), '/', -1)
            WHEN NULLIF(TRIM(iso_standard_name), '') IS NOT NULL THEN CONCAT(REPLACE(TRIM(iso_standard_name), ' ', '_'), '.txt')
            ELSE 'norma_iso.txt'
        END
    ),
    iso_file_data = COALESCE(
        iso_file_data,
        CONCAT(
            'Norma ISO', '\n',
            'Nazwa: ', COALESCE(NULLIF(TRIM(iso_standard_name), ''), '---'), '\n',
            'Sciezka importu: ', COALESCE(NULLIF(TRIM(iso_file_path), ''), '---'), '\n',
            '\n',
            'Placeholder dokumentu zapisany w bazie danych.'
        )
    )
WHERE NULLIF(TRIM(COALESCE(iso_standard_name, iso_file_path, '')), '') IS NOT NULL;

UPDATE project_legs
SET
    client_file_name = COALESCE(
        NULLIF(TRIM(client_file_name), ''),
        CASE
            WHEN NULLIF(TRIM(client_file_path), '') IS NOT NULL THEN SUBSTRING_INDEX(REPLACE(client_file_path, '\\', '/'), '/', -1)
            WHEN NULLIF(TRIM(client_standard_name), '') IS NOT NULL THEN CONCAT(REPLACE(TRIM(client_standard_name), ' ', '_'), '.txt')
            ELSE 'norma_klienta.txt'
        END
    ),
    client_file_data = COALESCE(
        client_file_data,
        CONCAT(
            'Norma klienta', '\n',
            'Nazwa: ', COALESCE(NULLIF(TRIM(client_standard_name), ''), '---'), '\n',
            'Sciezka importu: ', COALESCE(NULLIF(TRIM(client_file_path), ''), '---'), '\n',
            '\n',
            'Placeholder dokumentu zapisany w bazie danych.'
        )
    )
WHERE NULLIF(TRIM(COALESCE(client_standard_name, client_file_path, '')), '') IS NOT NULL;

UPDATE project_legs
SET
    test_plan_file_name = COALESCE(
        NULLIF(TRIM(test_plan_file_name), ''),
        CASE
            WHEN NULLIF(TRIM(test_plan_file_path), '') IS NOT NULL THEN SUBSTRING_INDEX(REPLACE(test_plan_file_path, '\\', '/'), '/', -1)
            WHEN NULLIF(TRIM(test_plan_name), '') IS NOT NULL THEN CONCAT(REPLACE(TRIM(test_plan_name), ' ', '_'), '.txt')
            ELSE 'test_plan.txt'
        END
    ),
    test_plan_file_data = COALESCE(
        test_plan_file_data,
        CONCAT(
            'Test plan', '\n',
            'Nazwa: ', COALESCE(NULLIF(TRIM(test_plan_name), ''), '---'), '\n',
            'Sciezka importu: ', COALESCE(NULLIF(TRIM(test_plan_file_path), ''), '---'), '\n',
            '\n',
            'Placeholder dokumentu zapisany w bazie danych.'
        )
    )
WHERE NULLIF(TRIM(COALESCE(test_plan_name, test_plan_file_path, '')), '') IS NOT NULL;
