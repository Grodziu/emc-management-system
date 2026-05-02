USE emc_management_system;

INSERT INTO users (first_name, last_name, email, login, password, role) VALUES
('Daniel', 'Rutkowski', 'daniel.rutkowski@walidacja.com', 'danielrutkowski', 'test123', 'VE'),
('Monika', 'Ciesla', 'monika.ciesla@walidacja.com', 'monikaciesla', 'test123', 'VE'),
('Alicja', 'Wilk', 'alicja.wilk@walidacja.com', 'alicjawilk', 'test123', 'TE'),
('Pawel', 'Kaczmarek', 'pawel.kaczmarek@walidacja.com', 'pawelkaczmarek', 'test123', 'TE'),
('Michal', 'Urban', 'michal.urban@walidacja.com', 'michalurban', 'test123', 'TT'),
('Julia', 'Baran', 'julia.baran@walidacja.com', 'juliabaran', 'test123', 'TT'),
('Tomasz', 'Zych', 'tomasz.zych@walidacja.com', 'tomaszzych', 'test123', 'TT'),
('Milena', 'Wojda', 'milena.wojda@walidacja.com', 'milenawojda', 'test123', 'TT')
ON DUPLICATE KEY UPDATE
    first_name = VALUES(first_name),
    last_name = VALUES(last_name),
    email = VALUES(email),
    password = VALUES(password),
    role = VALUES(role);

INSERT INTO measurement_equipment (
    equipment_code, equipment_name, category, manufacturer, model,
    serial_number, calibration_valid_until, location, notes
) VALUES
('EQ-ANT-007', 'Biconical Antenna', 'RF', 'Rohde & Schwarz', 'HF907', 'RS-HF907-007', '2026-11-30', 'Lab A', 'Radiated immunity antenna'),
('EQ-ANT-008', 'Log-Periodic Antenna', 'RF', 'Rohde & Schwarz', 'HL562E', 'RS-HL562E-008', '2026-11-30', 'Lab A', 'Wideband EMC antenna'),
('EQ-CDN-009', 'Coupling Decoupling Network', 'Conducted', 'TESEQ', 'CDN M316', 'TSQ-CDN-009', '2026-10-14', 'Lab C', 'CDN for conducted immunity'),
('EQ-PRB-010', 'Current Probe', 'Measurement', 'Tekbox', 'TBCCP1', 'TB-PRB-010', '2026-09-28', 'Lab A', 'Current monitoring probe'),
('EQ-BAT-011', 'Battery Simulator', 'Power', 'Keysight', 'RP7972A', 'KEY-RP7972A-011', '2026-12-11', 'Lab D', 'Battery simulator'),
('EQ-TRN-012', 'Transient Generator', 'Power', 'AMETEK CTS', 'NSG 5500', 'AMT-NSG5500-012', '2026-11-07', 'Lab D', 'Generator for ISO 7637'),
('EQ-PWR-013', 'RF Power Meter', 'RF', 'Keysight', 'N1914A', 'KEY-N1914A-013', '2026-08-21', 'Lab A', 'Power monitoring for RF amplifier chain'),
('EQ-DAQ-014', 'DAQ Recorder', 'Measurement', 'NI', 'cDAQ-9189', 'NI-CDAQ-014', '2026-09-17', 'Lab E', 'DAQ for long-duration tests'),
('EQ-EMS-015', 'EMI Test Receiver', 'RF', 'Rohde & Schwarz', 'ESW8', 'RS-ESW8-015', '2026-12-19', 'Lab A', 'Receiver for advanced emission scans'),
('EQ-LDG-016', 'Load Dump Generator', 'Power', 'AMETEK CTS', 'LD 200N', 'AMT-LD200N-016', '2026-10-03', 'Lab E', 'Load dump source')
ON DUPLICATE KEY UPDATE
    equipment_name = VALUES(equipment_name),
    category = VALUES(category),
    manufacturer = VALUES(manufacturer),
    model = VALUES(model),
    serial_number = VALUES(serial_number),
    calibration_valid_until = VALUES(calibration_valid_until),
    location = VALUES(location),
    notes = VALUES(notes);

INSERT INTO measurement_equipment (
    equipment_code, equipment_name, category, manufacturer, model,
    serial_number, calibration_valid_until, location, notes, climate_sensor_code
) VALUES
('EQ-CHM-017', 'Climatic Chamber', 'Climate', 'Weiss Technik', 'WK-340', 'WT-WK340-017', '2026-12-31', 'Lab B', 'Climatic chamber using dedicated sensor log', 'EQ-SEN-001')
ON DUPLICATE KEY UPDATE
    equipment_name = VALUES(equipment_name),
    category = VALUES(category),
    manufacturer = VALUES(manufacturer),
    model = VALUES(model),
    serial_number = VALUES(serial_number),
    calibration_valid_until = VALUES(calibration_valid_until),
    location = VALUES(location),
    notes = VALUES(notes),
    climate_sensor_code = VALUES(climate_sensor_code);

DELETE FROM projects
WHERE ewr_number IN ('EWR105031', 'EWR105084', 'EWR105129', 'EWR105176');

INSERT INTO projects (ewr_number, brand, device_name, short_description, ve_id, te_id, start_date, end_date)
SELECT 'EWR105031', 'AUDI', 'MIB3', 'Infotainment head unit',
       (SELECT id FROM users WHERE login = 'danielrutkowski'),
       (SELECT id FROM users WHERE login = 'alicjawilk'),
       NULL, NULL
UNION ALL
SELECT 'EWR105084', 'MERCEDES-BENZ', 'ZGS', 'Central gateway',
       (SELECT id FROM users WHERE login = 'monikaciesla'),
       (SELECT id FROM users WHERE login = 'pawelkaczmarek'),
       NULL, NULL
UNION ALL
SELECT 'EWR105129', 'VOLKSWAGEN', 'BCM', 'Body control module',
       (SELECT id FROM users WHERE login = 'danielrutkowski'),
       (SELECT id FROM users WHERE login = 'pawelkaczmarek'),
       NULL, NULL
UNION ALL
SELECT 'EWR105176', 'PORSCHE', 'Front Camera ECU', 'ADAS controller',
       (SELECT id FROM users WHERE login = 'monikaciesla'),
       (SELECT id FROM users WHERE login = 'alicjawilk'),
       NULL, NULL;

INSERT INTO project_tt_assignments (project_id, tt_user_id)
SELECT p.id, u.id
FROM projects p
JOIN users u ON u.login = 'michalurban'
WHERE p.ewr_number = 'EWR105031'
UNION ALL
SELECT p.id, u.id
FROM projects p
JOIN users u ON u.login = 'juliabaran'
WHERE p.ewr_number = 'EWR105031'
UNION ALL
SELECT p.id, u.id
FROM projects p
JOIN users u ON u.login = 'tomaszzych'
WHERE p.ewr_number = 'EWR105031'
UNION ALL
SELECT p.id, u.id
FROM projects p
JOIN users u ON u.login = 'milenawojda'
WHERE p.ewr_number = 'EWR105084'
UNION ALL
SELECT p.id, u.id
FROM projects p
JOIN users u ON u.login = 'michalurban'
WHERE p.ewr_number = 'EWR105084'
UNION ALL
SELECT p.id, u.id
FROM projects p
JOIN users u ON u.login = 'juliabaran'
WHERE p.ewr_number = 'EWR105129'
UNION ALL
SELECT p.id, u.id
FROM projects p
JOIN users u ON u.login = 'tomaszzych'
WHERE p.ewr_number = 'EWR105129'
UNION ALL
SELECT p.id, u.id
FROM projects p
JOIN users u ON u.login = 'michalurban'
WHERE p.ewr_number = 'EWR105176'
UNION ALL
SELECT p.id, u.id
FROM projects p
JOIN users u ON u.login = 'milenawojda'
WHERE p.ewr_number = 'EWR105176'
UNION ALL
SELECT p.id, u.id
FROM projects p
JOIN users u ON u.login = 'tomaszzych'
WHERE p.ewr_number = 'EWR105176';

INSERT INTO project_legs (
    project_id, leg_code, test_type, accreditation, start_date, end_date, assigned_tt_id,
    iso_standard_name, iso_file_path, iso_file_name, iso_file_data,
    client_standard_name, client_file_path, client_file_name, client_file_data,
    test_plan_name, test_plan_file_path, test_plan_file_name, test_plan_file_data,
    pca_url
)
SELECT p.id, 'LEG_00A', 'EMC', 'YES', NULL, NULL, (SELECT id FROM users WHERE login = 'michalurban'),
       'ISO 11452', NULL, 'ISO_AUDI_MIB3_LEG_00A.txt',
       CONCAT('Norma ISO', '\n', 'Projekt: EWR105031 | AUDI MIB3 Infotainment head unit', '\n', 'LEG: LEG_00A | EMC', '\n', 'Dokument: ISO 11452', '\n\n', 'Przykladowy dokument demonstracyjny zapisany w bazie danych.'),
       'AUDI_EMC_002_v1.0', NULL, 'CLIENT_AUDI_MIB3_LEG_00A.txt',
       CONCAT('Norma klienta', '\n', 'Projekt: EWR105031 | AUDI MIB3 Infotainment head unit', '\n', 'LEG: LEG_00A | EMC', '\n', 'Dokument: AUDI_EMC_002_v1.0', '\n\n', 'Zakres badan i kryteria oceny klienta.'),
       'Audi_MIB3_EMC_TP_v1.0', NULL, 'TP_AUDI_MIB3_LEG_00A.txt',
       CONCAT('Test plan', '\n', 'Projekt: EWR105031 | AUDI MIB3 Infotainment head unit', '\n', 'LEG: LEG_00A | EMC', '\n', 'Dokument: Audi_MIB3_EMC_TP_v1.0', '\n\n', 'Sekwencja stanowiska, logowania i raportowania.'),
       'https://www.pca.gov.pl/'
FROM projects p WHERE p.ewr_number = 'EWR105031'
UNION ALL
SELECT p.id, 'LEG_00B', 'FT', 'YES', NULL, NULL, (SELECT id FROM users WHERE login = 'juliabaran'),
       'ISO 7637', NULL, 'ISO_AUDI_MIB3_LEG_00B.txt',
       CONCAT('Norma ISO', '\n', 'Projekt: EWR105031 | AUDI MIB3 Infotainment head unit', '\n', 'LEG: LEG_00B | FT', '\n', 'Dokument: ISO 7637', '\n\n', 'Przykladowy dokument demonstracyjny zapisany w bazie danych.'),
       'AUDI_FT_004_v1.2', NULL, 'CLIENT_AUDI_MIB3_LEG_00B.txt',
       CONCAT('Norma klienta', '\n', 'Projekt: EWR105031 | AUDI MIB3 Infotainment head unit', '\n', 'LEG: LEG_00B | FT', '\n', 'Dokument: AUDI_FT_004_v1.2', '\n\n', 'Zakres badan i kryteria oceny klienta.'),
       'Audi_MIB3_FT_TP_v1.2', NULL, 'TP_AUDI_MIB3_LEG_00B.txt',
       CONCAT('Test plan', '\n', 'Projekt: EWR105031 | AUDI MIB3 Infotainment head unit', '\n', 'LEG: LEG_00B | FT', '\n', 'Dokument: Audi_MIB3_FT_TP_v1.2', '\n\n', 'Sekwencja stanowiska, logowania i raportowania.'),
       'https://www.pca.gov.pl/'
FROM projects p WHERE p.ewr_number = 'EWR105031'
UNION ALL
SELECT p.id, 'LEG_00C', 'ESD', 'YES', NULL, NULL, (SELECT id FROM users WHERE login = 'tomaszzych'),
       'ISO 10605', NULL, 'ISO_AUDI_MIB3_LEG_00C.txt',
       CONCAT('Norma ISO', '\n', 'Projekt: EWR105031 | AUDI MIB3 Infotainment head unit', '\n', 'LEG: LEG_00C | ESD', '\n', 'Dokument: ISO 10605', '\n\n', 'Przykladowy dokument demonstracyjny zapisany w bazie danych.'),
       'AUDI_ESD_001_v1.1', NULL, 'CLIENT_AUDI_MIB3_LEG_00C.txt',
       CONCAT('Norma klienta', '\n', 'Projekt: EWR105031 | AUDI MIB3 Infotainment head unit', '\n', 'LEG: LEG_00C | ESD', '\n', 'Dokument: AUDI_ESD_001_v1.1', '\n\n', 'Zakres badan i kryteria oceny klienta.'),
       'Audi_MIB3_ESD_TP_v1.0', NULL, 'TP_AUDI_MIB3_LEG_00C.txt',
       CONCAT('Test plan', '\n', 'Projekt: EWR105031 | AUDI MIB3 Infotainment head unit', '\n', 'LEG: LEG_00C | ESD', '\n', 'Dokument: Audi_MIB3_ESD_TP_v1.0', '\n\n', 'Sekwencja stanowiska, logowania i raportowania.'),
       'https://www.pca.gov.pl/'
FROM projects p WHERE p.ewr_number = 'EWR105031'
UNION ALL
SELECT p.id, 'LEG_00A', 'ELE', 'YES', NULL, NULL, (SELECT id FROM users WHERE login = 'milenawojda'),
       'ISO 16750', NULL, 'ISO_MBZ_ZGS_LEG_00A.txt',
       CONCAT('Norma ISO', '\n', 'Projekt: EWR105084 | MERCEDES-BENZ ZGS Central gateway', '\n', 'LEG: LEG_00A | ELE', '\n', 'Dokument: ISO 16750', '\n\n', 'Przykladowy dokument demonstracyjny zapisany w bazie danych.'),
       'MBZ_ELE_003_v2.0', NULL, 'CLIENT_MBZ_ZGS_LEG_00A.txt',
       CONCAT('Norma klienta', '\n', 'Projekt: EWR105084 | MERCEDES-BENZ ZGS Central gateway', '\n', 'LEG: LEG_00A | ELE', '\n', 'Dokument: MBZ_ELE_003_v2.0', '\n\n', 'Zakres badan i kryteria oceny klienta.'),
       'MBZ_ZGS_ELE_TP_v1.4', NULL, 'TP_MBZ_ZGS_LEG_00A.txt',
       CONCAT('Test plan', '\n', 'Projekt: EWR105084 | MERCEDES-BENZ ZGS Central gateway', '\n', 'LEG: LEG_00A | ELE', '\n', 'Dokument: MBZ_ZGS_ELE_TP_v1.4', '\n\n', 'Sekwencja stanowiska, logowania i raportowania.'),
       'https://www.pca.gov.pl/'
FROM projects p WHERE p.ewr_number = 'EWR105084'
UNION ALL
SELECT p.id, 'LEG_00B', 'EMC', 'YES', NULL, NULL, (SELECT id FROM users WHERE login = 'michalurban'),
       'ISO 11452', NULL, 'ISO_MBZ_ZGS_LEG_00B.txt',
       CONCAT('Norma ISO', '\n', 'Projekt: EWR105084 | MERCEDES-BENZ ZGS Central gateway', '\n', 'LEG: LEG_00B | EMC', '\n', 'Dokument: ISO 11452', '\n\n', 'Przykladowy dokument demonstracyjny zapisany w bazie danych.'),
       'MBZ_EMC_005_v1.3', NULL, 'CLIENT_MBZ_ZGS_LEG_00B.txt',
       CONCAT('Norma klienta', '\n', 'Projekt: EWR105084 | MERCEDES-BENZ ZGS Central gateway', '\n', 'LEG: LEG_00B | EMC', '\n', 'Dokument: MBZ_EMC_005_v1.3', '\n\n', 'Zakres badan i kryteria oceny klienta.'),
       'MBZ_ZGS_EMC_TP_v1.1', NULL, 'TP_MBZ_ZGS_LEG_00B.txt',
       CONCAT('Test plan', '\n', 'Projekt: EWR105084 | MERCEDES-BENZ ZGS Central gateway', '\n', 'LEG: LEG_00B | EMC', '\n', 'Dokument: MBZ_ZGS_EMC_TP_v1.1', '\n\n', 'Sekwencja stanowiska, logowania i raportowania.'),
       'https://www.pca.gov.pl/'
FROM projects p WHERE p.ewr_number = 'EWR105084'
UNION ALL
SELECT p.id, 'LEG_00C', 'ESD', 'YES', NULL, NULL, (SELECT id FROM users WHERE login = 'milenawojda'),
       'ISO 10605', NULL, 'ISO_MBZ_ZGS_LEG_00C.txt',
       CONCAT('Norma ISO', '\n', 'Projekt: EWR105084 | MERCEDES-BENZ ZGS Central gateway', '\n', 'LEG: LEG_00C | ESD', '\n', 'Dokument: ISO 10605', '\n\n', 'Przykladowy dokument demonstracyjny zapisany w bazie danych.'),
       'MBZ_ESD_002_v1.0', NULL, 'CLIENT_MBZ_ZGS_LEG_00C.txt',
       CONCAT('Norma klienta', '\n', 'Projekt: EWR105084 | MERCEDES-BENZ ZGS Central gateway', '\n', 'LEG: LEG_00C | ESD', '\n', 'Dokument: MBZ_ESD_002_v1.0', '\n\n', 'Zakres badan i kryteria oceny klienta.'),
       'MBZ_ZGS_ESD_TP_v1.0', NULL, 'TP_MBZ_ZGS_LEG_00C.txt',
       CONCAT('Test plan', '\n', 'Projekt: EWR105084 | MERCEDES-BENZ ZGS Central gateway', '\n', 'LEG: LEG_00C | ESD', '\n', 'Dokument: MBZ_ZGS_ESD_TP_v1.0', '\n\n', 'Sekwencja stanowiska, logowania i raportowania.'),
       'https://www.pca.gov.pl/'
FROM projects p WHERE p.ewr_number = 'EWR105084';

INSERT INTO project_legs (
    project_id, leg_code, test_type, accreditation, start_date, end_date, assigned_tt_id,
    iso_standard_name, iso_file_path, iso_file_name, iso_file_data,
    client_standard_name, client_file_path, client_file_name, client_file_data,
    test_plan_name, test_plan_file_path, test_plan_file_name, test_plan_file_data,
    pca_url
)
SELECT p.id, 'LEG_00A', 'EMC', 'YES', NULL, NULL, (SELECT id FROM users WHERE login = 'juliabaran'),
       'ISO 11452', NULL, 'ISO_VW_BCM_LEG_00A.txt',
       CONCAT('Norma ISO', '\n', 'Projekt: EWR105129 | VOLKSWAGEN BCM Body control module', '\n', 'LEG: LEG_00A | EMC', '\n', 'Dokument: ISO 11452', '\n\n', 'Przykladowy dokument demonstracyjny zapisany w bazie danych.'),
       'VW_EMC_003_v1.1', NULL, 'CLIENT_VW_BCM_LEG_00A.txt',
       CONCAT('Norma klienta', '\n', 'Projekt: EWR105129 | VOLKSWAGEN BCM Body control module', '\n', 'LEG: LEG_00A | EMC', '\n', 'Dokument: VW_EMC_003_v1.1', '\n\n', 'Zakres badan i kryteria oceny klienta.'),
       'VW_BCM_EMC_TP_v1.0', NULL, 'TP_VW_BCM_LEG_00A.txt',
       CONCAT('Test plan', '\n', 'Projekt: EWR105129 | VOLKSWAGEN BCM Body control module', '\n', 'LEG: LEG_00A | EMC', '\n', 'Dokument: VW_BCM_EMC_TP_v1.0', '\n\n', 'Sekwencja stanowiska, logowania i raportowania.'),
       'https://www.pca.gov.pl/'
FROM projects p WHERE p.ewr_number = 'EWR105129'
UNION ALL
SELECT p.id, 'LEG_00B', 'FT', 'YES', NULL, NULL, (SELECT id FROM users WHERE login = 'tomaszzych'),
       'ISO 7637', NULL, 'ISO_VW_BCM_LEG_00B.txt',
       CONCAT('Norma ISO', '\n', 'Projekt: EWR105129 | VOLKSWAGEN BCM Body control module', '\n', 'LEG: LEG_00B | FT', '\n', 'Dokument: ISO 7637', '\n\n', 'Przykladowy dokument demonstracyjny zapisany w bazie danych.'),
       'VW_FT_002_v1.0', NULL, 'CLIENT_VW_BCM_LEG_00B.txt',
       CONCAT('Norma klienta', '\n', 'Projekt: EWR105129 | VOLKSWAGEN BCM Body control module', '\n', 'LEG: LEG_00B | FT', '\n', 'Dokument: VW_FT_002_v1.0', '\n\n', 'Zakres badan i kryteria oceny klienta.'),
       'VW_BCM_FT_TP_v1.0', NULL, 'TP_VW_BCM_LEG_00B.txt',
       CONCAT('Test plan', '\n', 'Projekt: EWR105129 | VOLKSWAGEN BCM Body control module', '\n', 'LEG: LEG_00B | FT', '\n', 'Dokument: VW_BCM_FT_TP_v1.0', '\n\n', 'Sekwencja stanowiska, logowania i raportowania.'),
       'https://www.pca.gov.pl/'
FROM projects p WHERE p.ewr_number = 'EWR105129'
UNION ALL
SELECT p.id, 'LEG_00C', 'ELE', 'YES', NULL, NULL, (SELECT id FROM users WHERE login = 'juliabaran'),
       'ISO 16750', NULL, 'ISO_VW_BCM_LEG_00C.txt',
       CONCAT('Norma ISO', '\n', 'Projekt: EWR105129 | VOLKSWAGEN BCM Body control module', '\n', 'LEG: LEG_00C | ELE', '\n', 'Dokument: ISO 16750', '\n\n', 'Przykladowy dokument demonstracyjny zapisany w bazie danych.'),
       'VW_ELE_006_v1.0', NULL, 'CLIENT_VW_BCM_LEG_00C.txt',
       CONCAT('Norma klienta', '\n', 'Projekt: EWR105129 | VOLKSWAGEN BCM Body control module', '\n', 'LEG: LEG_00C | ELE', '\n', 'Dokument: VW_ELE_006_v1.0', '\n\n', 'Zakres badan i kryteria oceny klienta.'),
       'VW_BCM_ELE_TP_v1.1', NULL, 'TP_VW_BCM_LEG_00C.txt',
       CONCAT('Test plan', '\n', 'Projekt: EWR105129 | VOLKSWAGEN BCM Body control module', '\n', 'LEG: LEG_00C | ELE', '\n', 'Dokument: VW_BCM_ELE_TP_v1.1', '\n\n', 'Sekwencja stanowiska, logowania i raportowania.'),
       'https://www.pca.gov.pl/'
FROM projects p WHERE p.ewr_number = 'EWR105129'
UNION ALL
SELECT p.id, 'LEG_00A', 'EMC', 'YES', NULL, NULL, (SELECT id FROM users WHERE login = 'michalurban'),
       'ISO 11452', NULL, 'ISO_POR_FCAM_LEG_00A.txt',
       CONCAT('Norma ISO', '\n', 'Projekt: EWR105176 | PORSCHE Front Camera ECU ADAS controller', '\n', 'LEG: LEG_00A | EMC', '\n', 'Dokument: ISO 11452', '\n\n', 'Przykladowy dokument demonstracyjny zapisany w bazie danych.'),
       'POR_EMC_001_v2.0', NULL, 'CLIENT_POR_FCAM_LEG_00A.txt',
       CONCAT('Norma klienta', '\n', 'Projekt: EWR105176 | PORSCHE Front Camera ECU ADAS controller', '\n', 'LEG: LEG_00A | EMC', '\n', 'Dokument: POR_EMC_001_v2.0', '\n\n', 'Zakres badan i kryteria oceny klienta.'),
       'Porsche_FCAM_EMC_TP_v1.2', NULL, 'TP_POR_FCAM_LEG_00A.txt',
       CONCAT('Test plan', '\n', 'Projekt: EWR105176 | PORSCHE Front Camera ECU ADAS controller', '\n', 'LEG: LEG_00A | EMC', '\n', 'Dokument: Porsche_FCAM_EMC_TP_v1.2', '\n\n', 'Sekwencja stanowiska, logowania i raportowania.'),
       'https://www.pca.gov.pl/'
FROM projects p WHERE p.ewr_number = 'EWR105176'
UNION ALL
SELECT p.id, 'LEG_00B', 'ELE', 'YES', NULL, NULL, (SELECT id FROM users WHERE login = 'milenawojda'),
       'ISO 16750', NULL, 'ISO_POR_FCAM_LEG_00B.txt',
       CONCAT('Norma ISO', '\n', 'Projekt: EWR105176 | PORSCHE Front Camera ECU ADAS controller', '\n', 'LEG: LEG_00B | ELE', '\n', 'Dokument: ISO 16750', '\n\n', 'Przykladowy dokument demonstracyjny zapisany w bazie danych.'),
       'POR_ELE_005_v1.0', NULL, 'CLIENT_POR_FCAM_LEG_00B.txt',
       CONCAT('Norma klienta', '\n', 'Projekt: EWR105176 | PORSCHE Front Camera ECU ADAS controller', '\n', 'LEG: LEG_00B | ELE', '\n', 'Dokument: POR_ELE_005_v1.0', '\n\n', 'Zakres badan i kryteria oceny klienta.'),
       'Porsche_FCAM_ELE_TP_v1.0', NULL, 'TP_POR_FCAM_LEG_00B.txt',
       CONCAT('Test plan', '\n', 'Projekt: EWR105176 | PORSCHE Front Camera ECU ADAS controller', '\n', 'LEG: LEG_00B | ELE', '\n', 'Dokument: Porsche_FCAM_ELE_TP_v1.0', '\n\n', 'Sekwencja stanowiska, logowania i raportowania.'),
       'https://www.pca.gov.pl/'
FROM projects p WHERE p.ewr_number = 'EWR105176'
UNION ALL
SELECT p.id, 'LEG_00C', 'FT', 'YES', NULL, NULL, (SELECT id FROM users WHERE login = 'tomaszzych'),
       'ISO 7637', NULL, 'ISO_POR_FCAM_LEG_00C.txt',
       CONCAT('Norma ISO', '\n', 'Projekt: EWR105176 | PORSCHE Front Camera ECU ADAS controller', '\n', 'LEG: LEG_00C | FT', '\n', 'Dokument: ISO 7637', '\n\n', 'Przykladowy dokument demonstracyjny zapisany w bazie danych.'),
       'POR_FT_002_v1.1', NULL, 'CLIENT_POR_FCAM_LEG_00C.txt',
       CONCAT('Norma klienta', '\n', 'Projekt: EWR105176 | PORSCHE Front Camera ECU ADAS controller', '\n', 'LEG: LEG_00C | FT', '\n', 'Dokument: POR_FT_002_v1.1', '\n\n', 'Zakres badan i kryteria oceny klienta.'),
       'Porsche_FCAM_FT_TP_v1.0', NULL, 'TP_POR_FCAM_LEG_00C.txt',
       CONCAT('Test plan', '\n', 'Projekt: EWR105176 | PORSCHE Front Camera ECU ADAS controller', '\n', 'LEG: LEG_00C | FT', '\n', 'Dokument: Porsche_FCAM_FT_TP_v1.0', '\n\n', 'Sekwencja stanowiska, logowania i raportowania.'),
       'https://www.pca.gov.pl/'
FROM projects p WHERE p.ewr_number = 'EWR105176';

INSERT INTO dut_samples (project_id, sample_code, serial_number)
SELECT p.id, '105031001', 'SN105031001X' FROM projects p WHERE p.ewr_number = 'EWR105031'
UNION ALL
SELECT p.id, '105031002', 'SN105031002X' FROM projects p WHERE p.ewr_number = 'EWR105031'
UNION ALL
SELECT p.id, '105031003', 'SN105031003X' FROM projects p WHERE p.ewr_number = 'EWR105031'
UNION ALL
SELECT p.id, '105031004', 'SN105031004X' FROM projects p WHERE p.ewr_number = 'EWR105031'
UNION ALL
SELECT p.id, '105084001', 'SN105084001X' FROM projects p WHERE p.ewr_number = 'EWR105084'
UNION ALL
SELECT p.id, '105084002', 'SN105084002X' FROM projects p WHERE p.ewr_number = 'EWR105084'
UNION ALL
SELECT p.id, '105084003', 'SN105084003X' FROM projects p WHERE p.ewr_number = 'EWR105084'
UNION ALL
SELECT p.id, '105084004', 'SN105084004X' FROM projects p WHERE p.ewr_number = 'EWR105084'
UNION ALL
SELECT p.id, '105129001', 'SN105129001X' FROM projects p WHERE p.ewr_number = 'EWR105129'
UNION ALL
SELECT p.id, '105129002', 'SN105129002X' FROM projects p WHERE p.ewr_number = 'EWR105129'
UNION ALL
SELECT p.id, '105129003', 'SN105129003X' FROM projects p WHERE p.ewr_number = 'EWR105129'
UNION ALL
SELECT p.id, '105129004', 'SN105129004X' FROM projects p WHERE p.ewr_number = 'EWR105129'
UNION ALL
SELECT p.id, '105176001', 'SN105176001X' FROM projects p WHERE p.ewr_number = 'EWR105176'
UNION ALL
SELECT p.id, '105176002', 'SN105176002X' FROM projects p WHERE p.ewr_number = 'EWR105176'
UNION ALL
SELECT p.id, '105176003', 'SN105176003X' FROM projects p WHERE p.ewr_number = 'EWR105176'
UNION ALL
SELECT p.id, '105176004', 'SN105176004X' FROM projects p WHERE p.ewr_number = 'EWR105176';

INSERT INTO leg_dut_assignments (leg_id, dut_sample_id)
SELECT pl.id, ds.id
FROM projects p
JOIN project_legs pl ON pl.project_id = p.id
JOIN dut_samples ds ON ds.project_id = p.id
WHERE p.ewr_number IN ('EWR105031', 'EWR105084', 'EWR105129', 'EWR105176');

INSERT INTO leg_test_steps (leg_id, step_order, step_name, status, start_date, end_date, test_room)
SELECT pl.id, 1, 'Radiated immunity setup', 'PASSED', '2026-01-05', '2026-01-07', 'LabA'
FROM projects p JOIN project_legs pl ON pl.project_id = p.id
WHERE p.ewr_number = 'EWR105031' AND pl.leg_code = 'LEG_00A'
UNION ALL
SELECT pl.id, 2, 'Bulk current injection', 'ONGOING', '2026-01-08', NULL, 'LabA'
FROM projects p JOIN project_legs pl ON pl.project_id = p.id
WHERE p.ewr_number = 'EWR105031' AND pl.leg_code = 'LEG_00A'
UNION ALL
SELECT pl.id, 3, 'Final EMC review', 'NOT_STARTED', '2026-01-12', '2026-01-14', 'LabA'
FROM projects p JOIN project_legs pl ON pl.project_id = p.id
WHERE p.ewr_number = 'EWR105031' AND pl.leg_code = 'LEG_00A'
UNION ALL
SELECT pl.id, 1, 'Pulse 1', 'NOT_STARTED', '2026-01-19', '2026-01-19', 'LabD'
FROM projects p JOIN project_legs pl ON pl.project_id = p.id
WHERE p.ewr_number = 'EWR105031' AND pl.leg_code = 'LEG_00B'
UNION ALL
SELECT pl.id, 2, 'Pulse 2a/2b', 'NOT_STARTED', '2026-01-20', '2026-01-21', 'LabD'
FROM projects p JOIN project_legs pl ON pl.project_id = p.id
WHERE p.ewr_number = 'EWR105031' AND pl.leg_code = 'LEG_00B'
UNION ALL
SELECT pl.id, 3, 'Pulse 3a/3b', 'NOT_STARTED', NULL, NULL, 'LabD'
FROM projects p JOIN project_legs pl ON pl.project_id = p.id
WHERE p.ewr_number = 'EWR105031' AND pl.leg_code = 'LEG_00B'
UNION ALL
SELECT pl.id, 1, 'Contact discharge', 'NOT_STARTED', NULL, NULL, 'LabB'
FROM projects p JOIN project_legs pl ON pl.project_id = p.id
WHERE p.ewr_number = 'EWR105031' AND pl.leg_code = 'LEG_00C'
UNION ALL
SELECT pl.id, 2, 'Air discharge', 'NOT_STARTED', NULL, NULL, 'LabB'
FROM projects p JOIN project_legs pl ON pl.project_id = p.id
WHERE p.ewr_number = 'EWR105031' AND pl.leg_code = 'LEG_00C'
UNION ALL
SELECT pl.id, 3, 'Functional check', 'NOT_STARTED', NULL, NULL, 'LabB'
FROM projects p JOIN project_legs pl ON pl.project_id = p.id
WHERE p.ewr_number = 'EWR105031' AND pl.leg_code = 'LEG_00C'
UNION ALL
SELECT pl.id, 1, 'Supply ramp-up', 'PASSED', '2026-01-12', '2026-01-14', 'LabE'
FROM projects p JOIN project_legs pl ON pl.project_id = p.id
WHERE p.ewr_number = 'EWR105084' AND pl.leg_code = 'LEG_00A'
UNION ALL
SELECT pl.id, 2, 'Voltage curve', 'DATA_IN_ANALYSIS', '2026-01-15', '2026-01-16', 'LabE'
FROM projects p JOIN project_legs pl ON pl.project_id = p.id
WHERE p.ewr_number = 'EWR105084' AND pl.leg_code = 'LEG_00A'
UNION ALL
SELECT pl.id, 3, 'Long-term endurance', 'NOT_STARTED', '2026-01-19', '2026-01-23', 'LabE'
FROM projects p JOIN project_legs pl ON pl.project_id = p.id
WHERE p.ewr_number = 'EWR105084' AND pl.leg_code = 'LEG_00A'
UNION ALL
SELECT pl.id, 1, 'Radiated immunity setup', 'PASSED', '2026-01-26', '2026-01-28', 'LabA'
FROM projects p JOIN project_legs pl ON pl.project_id = p.id
WHERE p.ewr_number = 'EWR105084' AND pl.leg_code = 'LEG_00B'
UNION ALL
SELECT pl.id, 2, 'Bulk current injection', 'DATA_IN_ANALYSIS', '2026-01-29', '2026-01-30', 'LabA'
FROM projects p JOIN project_legs pl ON pl.project_id = p.id
WHERE p.ewr_number = 'EWR105084' AND pl.leg_code = 'LEG_00B'
UNION ALL
SELECT pl.id, 3, 'Monitoring and logging', 'NOT_STARTED', '2026-02-02', '2026-02-04', 'LabA'
FROM projects p JOIN project_legs pl ON pl.project_id = p.id
WHERE p.ewr_number = 'EWR105084' AND pl.leg_code = 'LEG_00B'
UNION ALL
SELECT pl.id, 1, 'Contact discharge', 'PASSED', '2026-02-05', '2026-02-05', 'LabB'
FROM projects p JOIN project_legs pl ON pl.project_id = p.id
WHERE p.ewr_number = 'EWR105084' AND pl.leg_code = 'LEG_00C'
UNION ALL
SELECT pl.id, 2, 'Air discharge', 'PASSED', '2026-02-06', '2026-02-06', 'LabB'
FROM projects p JOIN project_legs pl ON pl.project_id = p.id
WHERE p.ewr_number = 'EWR105084' AND pl.leg_code = 'LEG_00C'
UNION ALL
SELECT pl.id, 3, 'Functional check', 'PASSED', '2026-02-09', '2026-02-09', 'LabB'
FROM projects p JOIN project_legs pl ON pl.project_id = p.id
WHERE p.ewr_number = 'EWR105084' AND pl.leg_code = 'LEG_00C'
UNION ALL
SELECT pl.id, 1, 'Radiated immunity setup', 'PASSED', '2026-02-09', '2026-02-11', 'LabA'
FROM projects p JOIN project_legs pl ON pl.project_id = p.id
WHERE p.ewr_number = 'EWR105129' AND pl.leg_code = 'LEG_00A'
UNION ALL
SELECT pl.id, 2, 'Bulk current injection', 'PASSED', '2026-02-12', '2026-02-13', 'LabA'
FROM projects p JOIN project_legs pl ON pl.project_id = p.id
WHERE p.ewr_number = 'EWR105129' AND pl.leg_code = 'LEG_00A'
UNION ALL
SELECT pl.id, 3, 'Final EMC review', 'PASSED', '2026-02-16', '2026-02-16', 'LabA'
FROM projects p JOIN project_legs pl ON pl.project_id = p.id
WHERE p.ewr_number = 'EWR105129' AND pl.leg_code = 'LEG_00A'
UNION ALL
SELECT pl.id, 1, 'Pulse 1', 'PASSED', '2026-02-17', '2026-02-17', 'LabC'
FROM projects p JOIN project_legs pl ON pl.project_id = p.id
WHERE p.ewr_number = 'EWR105129' AND pl.leg_code = 'LEG_00B'
UNION ALL
SELECT pl.id, 2, 'Pulse 2a/2b', 'PASSED', '2026-02-18', '2026-02-19', 'LabC'
FROM projects p JOIN project_legs pl ON pl.project_id = p.id
WHERE p.ewr_number = 'EWR105129' AND pl.leg_code = 'LEG_00B'
UNION ALL
SELECT pl.id, 3, 'Pulse 3a/3b', 'PASSED', '2026-02-20', '2026-02-20', 'LabC'
FROM projects p JOIN project_legs pl ON pl.project_id = p.id
WHERE p.ewr_number = 'EWR105129' AND pl.leg_code = 'LEG_00B'
UNION ALL
SELECT pl.id, 1, 'Supply ramp-up', 'PASSED', '2026-02-23', '2026-02-24', 'LabE'
FROM projects p JOIN project_legs pl ON pl.project_id = p.id
WHERE p.ewr_number = 'EWR105129' AND pl.leg_code = 'LEG_00C'
UNION ALL
SELECT pl.id, 2, 'Reverse polarity', 'PASSED', '2026-02-25', '2026-02-26', 'LabE'
FROM projects p JOIN project_legs pl ON pl.project_id = p.id
WHERE p.ewr_number = 'EWR105129' AND pl.leg_code = 'LEG_00C'
UNION ALL
SELECT pl.id, 3, 'Load dump review', 'PASSED', '2026-02-27', '2026-02-27', 'LabE'
FROM projects p JOIN project_legs pl ON pl.project_id = p.id
WHERE p.ewr_number = 'EWR105129' AND pl.leg_code = 'LEG_00C'
UNION ALL
SELECT pl.id, 1, 'Radiated immunity setup', 'PASSED', '2026-03-02', '2026-03-04', 'LabA'
FROM projects p JOIN project_legs pl ON pl.project_id = p.id
WHERE p.ewr_number = 'EWR105176' AND pl.leg_code = 'LEG_00A'
UNION ALL
SELECT pl.id, 2, 'Bulk current injection', 'PASSED', '2026-03-05', '2026-03-06', 'LabA'
FROM projects p JOIN project_legs pl ON pl.project_id = p.id
WHERE p.ewr_number = 'EWR105176' AND pl.leg_code = 'LEG_00A'
UNION ALL
SELECT pl.id, 3, 'Final EMC review', 'PASSED', '2026-03-09', '2026-03-09', 'LabA'
FROM projects p JOIN project_legs pl ON pl.project_id = p.id
WHERE p.ewr_number = 'EWR105176' AND pl.leg_code = 'LEG_00A'
UNION ALL
SELECT pl.id, 1, 'Supply ramp-up', 'PASSED', '2026-03-10', '2026-03-11', 'LabE'
FROM projects p JOIN project_legs pl ON pl.project_id = p.id
WHERE p.ewr_number = 'EWR105176' AND pl.leg_code = 'LEG_00B'
UNION ALL
SELECT pl.id, 2, 'Reverse polarity', 'FAILED', '2026-03-12', '2026-03-13', 'LabE'
FROM projects p JOIN project_legs pl ON pl.project_id = p.id
WHERE p.ewr_number = 'EWR105176' AND pl.leg_code = 'LEG_00B'
UNION ALL
SELECT pl.id, 3, 'Load dump review', 'PASSED', '2026-03-16', '2026-03-16', 'LabE'
FROM projects p JOIN project_legs pl ON pl.project_id = p.id
WHERE p.ewr_number = 'EWR105176' AND pl.leg_code = 'LEG_00B'
UNION ALL
SELECT pl.id, 1, 'Pulse 1', 'PASSED', '2026-03-17', '2026-03-17', 'LabD'
FROM projects p JOIN project_legs pl ON pl.project_id = p.id
WHERE p.ewr_number = 'EWR105176' AND pl.leg_code = 'LEG_00C'
UNION ALL
SELECT pl.id, 2, 'Pulse 2a/2b', 'PASSED', '2026-03-18', '2026-03-19', 'LabD'
FROM projects p JOIN project_legs pl ON pl.project_id = p.id
WHERE p.ewr_number = 'EWR105176' AND pl.leg_code = 'LEG_00C'
UNION ALL
SELECT pl.id, 3, 'Pulse 3a/3b', 'PASSED', '2026-03-20', '2026-03-20', 'LabD'
FROM projects p JOIN project_legs pl ON pl.project_id = p.id
WHERE p.ewr_number = 'EWR105176' AND pl.leg_code = 'LEG_00C';

INSERT IGNORE INTO step_equipment_assignments (leg_test_step_id, equipment_id)
SELECT lts.id, me.id
FROM projects p
JOIN project_legs pl ON pl.project_id = p.id
JOIN leg_test_steps lts ON lts.leg_id = pl.id
JOIN measurement_equipment me ON me.equipment_code IN ('EQ-RF-001', 'EQ-AMP-002', 'EQ-ANT-007', 'EQ-ANT-008', 'EQ-PWR-013')
WHERE p.ewr_number IN ('EWR105031', 'EWR105084', 'EWR105129', 'EWR105176')
  AND pl.test_type = 'EMC';

INSERT IGNORE INTO step_equipment_assignments (leg_test_step_id, equipment_id)
SELECT lts.id, me.id
FROM projects p
JOIN project_legs pl ON pl.project_id = p.id
JOIN leg_test_steps lts ON lts.leg_id = pl.id
JOIN measurement_equipment me ON me.equipment_code IN ('EQ-LISN-004', 'EQ-OSC-005', 'EQ-TRN-012', 'EQ-PRB-010')
WHERE p.ewr_number IN ('EWR105031', 'EWR105084', 'EWR105129', 'EWR105176')
  AND pl.test_type = 'FT';

INSERT IGNORE INTO step_equipment_assignments (leg_test_step_id, equipment_id)
SELECT lts.id, me.id
FROM projects p
JOIN project_legs pl ON pl.project_id = p.id
JOIN leg_test_steps lts ON lts.leg_id = pl.id
JOIN measurement_equipment me ON me.equipment_code IN ('EQ-ESD-003', 'EQ-DAQ-014')
WHERE p.ewr_number IN ('EWR105031', 'EWR105084', 'EWR105129', 'EWR105176')
  AND pl.test_type = 'ESD';

INSERT IGNORE INTO step_equipment_assignments (leg_test_step_id, equipment_id)
SELECT lts.id, me.id
FROM projects p
JOIN project_legs pl ON pl.project_id = p.id
JOIN leg_test_steps lts ON lts.leg_id = pl.id
JOIN measurement_equipment me ON me.equipment_code IN ('EQ-PSU-006', 'EQ-BAT-011', 'EQ-LDG-016', 'EQ-OSC-005')
WHERE p.ewr_number IN ('EWR105031', 'EWR105084', 'EWR105129', 'EWR105176')
  AND pl.test_type = 'ELE';

INSERT INTO dut_test_results (dut_sample_id, leg_test_step_id, condition_status, result_status, comment)
SELECT ds.id, lts.id, 'OK', 'PASSED', NULL
FROM projects p
JOIN project_legs pl ON pl.project_id = p.id
JOIN leg_test_steps lts ON lts.leg_id = pl.id AND lts.step_order = 1
JOIN dut_samples ds ON ds.project_id = p.id
WHERE p.ewr_number = 'EWR105031' AND pl.leg_code = 'LEG_00A';

INSERT INTO dut_test_results (dut_sample_id, leg_test_step_id, condition_status, result_status, comment)
SELECT ds.id, lts.id, 'OK', 'ONGOING', 'Measurement in progress'
FROM projects p
JOIN project_legs pl ON pl.project_id = p.id
JOIN leg_test_steps lts ON lts.leg_id = pl.id AND lts.step_order = 2
JOIN dut_samples ds ON ds.project_id = p.id
WHERE p.ewr_number = 'EWR105031' AND pl.leg_code = 'LEG_00A';

INSERT INTO dut_test_results (dut_sample_id, leg_test_step_id, condition_status, result_status, comment)
SELECT ds.id, lts.id, 'OK', 'PASSED', NULL
FROM projects p
JOIN project_legs pl ON pl.project_id = p.id
JOIN leg_test_steps lts ON lts.leg_id = pl.id AND lts.step_order = 1
JOIN dut_samples ds ON ds.project_id = p.id
WHERE p.ewr_number = 'EWR105084' AND pl.leg_code = 'LEG_00A';

INSERT INTO dut_test_results (dut_sample_id, leg_test_step_id, condition_status, result_status, comment)
SELECT ds.id, lts.id, 'OK', 'DATA_IN_ANALYSIS', 'Waveform under report review'
FROM projects p
JOIN project_legs pl ON pl.project_id = p.id
JOIN leg_test_steps lts ON lts.leg_id = pl.id AND lts.step_order = 2
JOIN dut_samples ds ON ds.project_id = p.id
WHERE p.ewr_number = 'EWR105084' AND pl.leg_code = 'LEG_00A';

INSERT INTO dut_test_results (dut_sample_id, leg_test_step_id, condition_status, result_status, comment)
SELECT ds.id, lts.id, 'OK', 'PASSED', NULL
FROM projects p
JOIN project_legs pl ON pl.project_id = p.id
JOIN leg_test_steps lts ON lts.leg_id = pl.id AND lts.step_order = 1
JOIN dut_samples ds ON ds.project_id = p.id
WHERE p.ewr_number = 'EWR105084' AND pl.leg_code = 'LEG_00B';

INSERT INTO dut_test_results (dut_sample_id, leg_test_step_id, condition_status, result_status, comment)
SELECT ds.id, lts.id, 'OK', 'DATA_IN_ANALYSIS', 'Awaiting final comparison'
FROM projects p
JOIN project_legs pl ON pl.project_id = p.id
JOIN leg_test_steps lts ON lts.leg_id = pl.id AND lts.step_order = 2
JOIN dut_samples ds ON ds.project_id = p.id
WHERE p.ewr_number = 'EWR105084' AND pl.leg_code = 'LEG_00B';

INSERT INTO dut_test_results (dut_sample_id, leg_test_step_id, condition_status, result_status, comment)
SELECT ds.id, lts.id, 'OK', 'PASSED', NULL
FROM projects p
JOIN project_legs pl ON pl.project_id = p.id
JOIN leg_test_steps lts ON lts.leg_id = pl.id AND lts.step_order IN (1, 2, 3)
JOIN dut_samples ds ON ds.project_id = p.id
WHERE p.ewr_number = 'EWR105084' AND pl.leg_code = 'LEG_00C';

INSERT INTO dut_test_results (dut_sample_id, leg_test_step_id, condition_status, result_status, comment)
SELECT ds.id, lts.id, 'OK', 'PASSED', NULL
FROM projects p
JOIN project_legs pl ON pl.project_id = p.id
JOIN leg_test_steps lts ON lts.leg_id = pl.id AND lts.step_order IN (1, 2, 3)
JOIN dut_samples ds ON ds.project_id = p.id
WHERE p.ewr_number = 'EWR105129' AND pl.leg_code IN ('LEG_00A', 'LEG_00B', 'LEG_00C');

INSERT INTO dut_test_results (dut_sample_id, leg_test_step_id, condition_status, result_status, comment)
SELECT ds.id, lts.id, 'OK', 'PASSED', NULL
FROM projects p
JOIN project_legs pl ON pl.project_id = p.id
JOIN leg_test_steps lts ON lts.leg_id = pl.id AND lts.step_order IN (1, 2, 3)
JOIN dut_samples ds ON ds.project_id = p.id
WHERE p.ewr_number = 'EWR105176' AND pl.leg_code IN ('LEG_00A', 'LEG_00C');

INSERT INTO dut_test_results (dut_sample_id, leg_test_step_id, condition_status, result_status, comment)
SELECT ds.id, lts.id, 'OK', 'PASSED', NULL
FROM projects p
JOIN project_legs pl ON pl.project_id = p.id
JOIN leg_test_steps lts ON lts.leg_id = pl.id AND lts.step_order IN (1, 3)
JOIN dut_samples ds ON ds.project_id = p.id
WHERE p.ewr_number = 'EWR105176' AND pl.leg_code = 'LEG_00B';

INSERT INTO dut_test_results (dut_sample_id, leg_test_step_id, condition_status, result_status, comment)
SELECT
    ds.id,
    lts.id,
    CASE WHEN ds.sample_code IN ('105176003', '105176004') THEN 'NOK' ELSE 'OK' END,
    'FAILED',
    CASE WHEN ds.sample_code IN ('105176003', '105176004') THEN 'Out of limit current draw' ELSE 'Reset after reverse polarity' END
FROM projects p
JOIN project_legs pl ON pl.project_id = p.id
JOIN leg_test_steps lts ON lts.leg_id = pl.id AND lts.step_order = 2
JOIN dut_samples ds ON ds.project_id = p.id
WHERE p.ewr_number = 'EWR105176' AND pl.leg_code = 'LEG_00B';
