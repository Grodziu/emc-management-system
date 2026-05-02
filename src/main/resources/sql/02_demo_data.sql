USE emc_management_system;

INSERT INTO users (first_name, last_name, email, login, password, role) VALUES
('Karol', 'Nawrocki', 'karol.nawrocki@walidacja.com', 'karolnawrocki', 'test123', 'VE'),
('Joanna', 'Skrzypek', 'joanna.skrzypek@walidacja.com', 'joannaskrzypek', 'test123', 'TE'),
('Piotr', 'Grodny', 'piotr.grodny@walidacja.com', 'piotrgrodny', 'test123', 'TT'),
('Mateusz', 'Trampek', 'mateusz.trampek@walidacja.com', 'mateusztrampek', 'test123', 'TT'),
('Natalia', 'Rudnik', 'natalia.rudnik@walidacja.com', 'nataliarudnik', 'test123', 'TT'),
('Adam', 'Kowalczyk', 'adam.kowalczyk@walidacja.com', 'adamkowalczyk', 'test123', 'VE'),
('Marta', 'Nowak', 'marta.nowak@walidacja.com', 'martanowak', 'test123', 'TE'),
('Kamil', 'Borys', 'kamil.borys@walidacja.com', 'kamilborys', 'test123', 'TT'),
('Ewa', 'Lis', 'ewa.lis@walidacja.com', 'ewalis', 'test123', 'TT'),
('Tomasz', 'Wojcik', 'tomasz.wojcik@walidacja.com', 'tomaszwojcik', 'test123', 'VE'),
('Anna', 'Maj', 'anna.maj@walidacja.com', 'annamaj', 'test123', 'TE'),
('Robert', 'Piatek', 'robert.piatek@walidacja.com', 'robertpiatek', 'test123', 'TT'),
('System', 'Administrator', 'admin@walidacja.com', 'admin', 'admin123', 'ADMIN');

INSERT INTO projects (ewr_number, brand, device_name, short_description, ve_id, te_id) VALUES
('EWR103612', 'VOLVO', 'IHU', 'Front Camera', 6, 7),
('EWR104207', 'BMW', 'SRR7/FLR7', 'Radar', 1, 2),
('EWR104892', 'Stellantis', 'P29', 'On-board computer', 10, 11);

INSERT INTO project_tt_assignments (project_id, tt_user_id) VALUES
(1, 8), (1, 9),
(2, 3), (2, 4), (2, 5),
(3, 4), (3, 12);

INSERT INTO project_legs (
    project_id, leg_code, test_type, accreditation, start_date, end_date, assigned_tt_id,
    iso_standard_name, iso_file_path,
    client_standard_name, client_file_path,
    test_plan_name, test_plan_file_path,
    pca_url
) VALUES
(1, 'LEG_00A', 'EMC', 'YES', '2026-02-10', '2026-03-12', 8,
 'ISO 11452', 'C:/EMC_Docs/ISO/ISO_11452.pdf',
 'VOLVO_CS_001_v2.0', 'C:/EMC_Docs/Client/VOLVO_CS_001_v2.0.pdf',
 'EMC_Volvo_IHU_TP_v1.0', 'C:/EMC_Docs/TestPlans/EMC_Volvo_IHU_TP_v1.0.pdf',
 'https://www.pca.gov.pl/'),
(1, 'LEG_00B', 'ESD', 'YES', '2026-03-15', NULL, 9,
 'ISO 10605', 'C:/EMC_Docs/ISO/ISO_10605.pdf',
 'VOLVO_ESD_004_v1.1', 'C:/EMC_Docs/Client/VOLVO_ESD_004_v1.1.pdf',
 'ESD_Volvo_IHU_TP_v1.1', 'C:/EMC_Docs/TestPlans/ESD_Volvo_IHU_TP_v1.1.pdf',
 'https://www.pca.gov.pl/'),
(1, 'LEG_00C', 'FT', 'YES', '2026-01-20', '2026-02-02', 8,
 'ISO 7637', 'C:/EMC_Docs/ISO/ISO_7637.pdf',
 'VOLVO_FT_002_v1.3', 'C:/EMC_Docs/Client/VOLVO_FT_002_v1.3.pdf',
 'FT_Volvo_IHU_TP_v1.0', 'C:/EMC_Docs/TestPlans/FT_Volvo_IHU_TP_v1.0.pdf',
 'https://www.pca.gov.pl/'),
(2, 'LEG_00A', 'ESD', 'YES', '2026-03-01', '2026-03-05', 3,
 'ISO 10605', 'C:/EMC_Docs/ISO/ISO_10605.pdf',
 'BMW_ST_00003_v1.1', 'C:/EMC_Docs/Client/BMW_ST_00003_v1.1.pdf',
 'EMCTestPlan_v1.1', 'C:/EMC_Docs/TestPlans/EMCTestPlan_v1.1.pdf',
 'https://www.pca.gov.pl/'),
(2, 'LEG_00B', 'FT', 'YES', '2026-03-06', '2026-03-20', 3,
 'ISO 7637', 'C:/EMC_Docs/ISO/ISO_7637.pdf',
 'BMW_ST_00003_v1.1', 'C:/EMC_Docs/Client/BMW_ST_00003_v1.1.pdf',
 'FastTransientTestPlan_SRRFLR_v1.5', 'C:/EMC_Docs/TestPlans/FastTransientTestPlan_SRRFLR_v1.5.pdf',
 'https://www.pca.gov.pl/'),
(2, 'LEG_00C', 'EMC', 'YES', '2026-03-21', NULL, 5,
 'ISO 11452', 'C:/EMC_Docs/ISO/ISO_11452.pdf',
 'BMW_ST_00002_v1.0', 'C:/EMC_Docs/Client/BMW_ST_00002_v1.0.pdf',
 'EMCTestPlan_v1.1', 'C:/EMC_Docs/TestPlans/EMCTestPlan_v1.1.pdf',
 'https://www.pca.gov.pl/'),
(2, 'LEG_00D', 'ELE', 'NO', '2026-03-10', '2026-06-12', 4,
 'ISO 2678', 'C:/EMC_Docs/ISO/ISO_2678.pdf',
 'BMW_ST_00001_v1.4', 'C:/EMC_Docs/Client/BMW_ST_00001_v1.4.pdf',
 'ElectricalTP_v1.1', 'C:/EMC_Docs/TestPlans/ElectricalTP_v1.1.pdf',
 'https://www.pca.gov.pl/'),
(3, 'LEG_00A', 'EMC', 'YES', '2026-01-12', '2026-02-15', 12,
 'ISO 11452', 'C:/EMC_Docs/ISO/ISO_11452.pdf',
 'STLA_EMC_001_v3.0', 'C:/EMC_Docs/Client/STLA_EMC_001_v3.0.pdf',
 'STLA_EMC_TP_v2.0', 'C:/EMC_Docs/TestPlans/STLA_EMC_TP_v2.0.pdf',
 'https://www.pca.gov.pl/'),
(3, 'LEG_00B', 'ELE', 'YES', '2026-02-20', '2026-03-18', 4,
 'ISO 16750', 'C:/EMC_Docs/ISO/ISO_16750.pdf',
 'STLA_ELE_004_v1.2', 'C:/EMC_Docs/Client/STLA_ELE_004_v1.2.pdf',
 'STLA_ELE_TP_v1.3', 'C:/EMC_Docs/TestPlans/STLA_ELE_TP_v1.3.pdf',
 'https://www.pca.gov.pl/'),
(3, 'LEG_00C', 'ESD', 'YES', '2026-03-19', '2026-03-28', 12,
 'ISO 10605', 'C:/EMC_Docs/ISO/ISO_10605.pdf',
 'STLA_ESD_002_v1.0', 'C:/EMC_Docs/Client/STLA_ESD_002_v1.0.pdf',
 'STLA_ESD_TP_v1.0', 'C:/EMC_Docs/TestPlans/STLA_ESD_TP_v1.0.pdf',
 'https://www.pca.gov.pl/');

UPDATE project_legs
SET
    iso_file_name = CASE
        WHEN NULLIF(TRIM(iso_file_path), '') IS NOT NULL THEN SUBSTRING_INDEX(REPLACE(iso_file_path, '\\', '/'), '/', -1)
        ELSE CONCAT(REPLACE(TRIM(iso_standard_name), ' ', '_'), '.txt')
    END,
    iso_file_data = CONCAT(
        'Norma ISO', '\n',
        'Nazwa: ', COALESCE(NULLIF(TRIM(iso_standard_name), ''), '---'), '\n',
        'Sciezka importu: ', COALESCE(NULLIF(TRIM(iso_file_path), ''), '---'), '\n',
        '\n',
        'Placeholder dokumentu zapisany w bazie danych.'
    ),
    client_file_name = CASE
        WHEN NULLIF(TRIM(client_file_path), '') IS NOT NULL THEN SUBSTRING_INDEX(REPLACE(client_file_path, '\\', '/'), '/', -1)
        ELSE CONCAT(REPLACE(TRIM(client_standard_name), ' ', '_'), '.txt')
    END,
    client_file_data = CONCAT(
        'Norma klienta', '\n',
        'Nazwa: ', COALESCE(NULLIF(TRIM(client_standard_name), ''), '---'), '\n',
        'Sciezka importu: ', COALESCE(NULLIF(TRIM(client_file_path), ''), '---'), '\n',
        '\n',
        'Placeholder dokumentu zapisany w bazie danych.'
    ),
    test_plan_file_name = CASE
        WHEN NULLIF(TRIM(test_plan_file_path), '') IS NOT NULL THEN SUBSTRING_INDEX(REPLACE(test_plan_file_path, '\\', '/'), '/', -1)
        ELSE CONCAT(REPLACE(TRIM(test_plan_name), ' ', '_'), '.txt')
    END,
    test_plan_file_data = CONCAT(
        'Test plan', '\n',
        'Nazwa: ', COALESCE(NULLIF(TRIM(test_plan_name), ''), '---'), '\n',
        'Sciezka importu: ', COALESCE(NULLIF(TRIM(test_plan_file_path), ''), '---'), '\n',
        '\n',
        'Placeholder dokumentu zapisany w bazie danych.'
    );

INSERT INTO leg_test_steps (leg_id, step_order, step_name, status, start_date, end_date) VALUES
(1, 1, 'Radiated immunity setup', 'PASSED', '2026-02-10', '2026-02-12'),
(1, 2, 'Bulk current injection', 'DATA_IN_ANALYSIS', '2026-02-13', NULL),
(1, 3, 'Final EMC review', 'NOT_STARTED', NULL, NULL),
(2, 1, 'Contact discharge', 'NOT_STARTED', NULL, NULL),
(2, 2, 'Air discharge', 'NOT_STARTED', NULL, NULL),
(2, 3, 'Post-check', 'NOT_STARTED', NULL, NULL),
(3, 1, 'Pulse 1', 'PASSED', '2026-01-20', '2026-01-21'),
(3, 2, 'Pulse 2a/2b', 'PASSED', '2026-01-22', '2026-01-25'),
(3, 3, 'Pulse 3a/3b', 'PASSED', '2026-01-26', '2026-02-02'),
(4, 1, 'Contact discharge', 'PASSED', '2026-03-01', '2026-03-02'),
(4, 2, 'Air discharge', 'PASSED', '2026-03-03', '2026-03-04'),
(4, 3, 'Functional check', 'PASSED', '2026-03-05', '2026-03-05'),
(5, 1, 'Pulse 1', 'NOT_STARTED', NULL, NULL),
(5, 2, 'Pulse 2a/2b', 'NOT_STARTED', NULL, NULL),
(5, 3, 'Pulse 3a/3b', 'NOT_STARTED', NULL, NULL),
(6, 1, 'Radiated immunity setup', 'PASSED', '2026-03-21', '2026-03-23'),
(6, 2, 'Bulk current injection', 'ONGOING', '2026-03-24', NULL),
(6, 3, 'Monitoring and logging', 'NOT_STARTED', NULL, NULL),
(7, 1, 'Long-term overvoltage', 'PASSED', '2026-03-10', '2026-03-22'),
(7, 2, 'Quiescent current', 'PASSED', '2026-03-23', '2026-04-01'),
(7, 3, 'Voltage Curve', 'ONGOING', '2026-04-02', NULL),
(7, 4, 'Short Circuit', 'NOT_STARTED', NULL, NULL),
(7, 5, 'Insulation Resistance', 'NOT_STARTED', NULL, NULL),
(8, 1, 'Radiated immunity setup', 'PASSED', '2026-01-12', '2026-01-18'),
(8, 2, 'Bulk current injection', 'PASSED', '2026-01-19', '2026-01-30'),
(8, 3, 'Final EMC review', 'PASSED', '2026-02-01', '2026-02-15'),
(9, 1, 'Overvoltage', 'PASSED', '2026-02-20', '2026-02-28'),
(9, 2, 'Reverse polarity', 'FAILED', '2026-03-01', '2026-03-10'),
(9, 3, 'Load dump review', 'PASSED', '2026-03-11', '2026-03-18'),
(10, 1, 'Contact discharge', 'PASSED', '2026-03-19', '2026-03-22'),
(10, 2, 'Air discharge', 'PASSED', '2026-03-23', '2026-03-26'),
(10, 3, 'Functional check', 'PASSED', '2026-03-27', '2026-03-28');

INSERT INTO dut_samples (project_id, sample_code, serial_number) VALUES
(1, '103612001', 'SN103612001X'), (1, '103612002', 'SN103612002X'), (1, '103612003', 'SN103612003X'),
(2, '104207001', 'SN104207001X'), (2, '104207002', 'SN104207002X'), (2, '104207003', 'SN104207003X'), (2, '104207004', 'SN104207004X'), (2, '104207005', 'SN104207005X'), (2, '104207006', 'SN104207006X'),
(3, '104892001', 'SN104892001X'), (3, '104892002', 'SN104892002X'), (3, '104892003', 'SN104892003X'), (3, '104892004', 'SN104892004X');

INSERT INTO leg_dut_assignments (leg_id, dut_sample_id) VALUES
((SELECT id FROM project_legs WHERE project_id = 1 AND leg_code = 'LEG_00A'), (SELECT id FROM dut_samples WHERE sample_code = '103612001')),
((SELECT id FROM project_legs WHERE project_id = 1 AND leg_code = 'LEG_00A'), (SELECT id FROM dut_samples WHERE sample_code = '103612002')),
((SELECT id FROM project_legs WHERE project_id = 1 AND leg_code = 'LEG_00A'), (SELECT id FROM dut_samples WHERE sample_code = '103612003')),
((SELECT id FROM project_legs WHERE project_id = 1 AND leg_code = 'LEG_00B'), (SELECT id FROM dut_samples WHERE sample_code = '103612001')),
((SELECT id FROM project_legs WHERE project_id = 1 AND leg_code = 'LEG_00B'), (SELECT id FROM dut_samples WHERE sample_code = '103612002')),
((SELECT id FROM project_legs WHERE project_id = 1 AND leg_code = 'LEG_00B'), (SELECT id FROM dut_samples WHERE sample_code = '103612003')),
((SELECT id FROM project_legs WHERE project_id = 1 AND leg_code = 'LEG_00C'), (SELECT id FROM dut_samples WHERE sample_code = '103612001')),
((SELECT id FROM project_legs WHERE project_id = 1 AND leg_code = 'LEG_00C'), (SELECT id FROM dut_samples WHERE sample_code = '103612002')),
((SELECT id FROM project_legs WHERE project_id = 1 AND leg_code = 'LEG_00C'), (SELECT id FROM dut_samples WHERE sample_code = '103612003')),
((SELECT id FROM project_legs WHERE project_id = 2 AND leg_code = 'LEG_00A'), (SELECT id FROM dut_samples WHERE sample_code = '104207001')),
((SELECT id FROM project_legs WHERE project_id = 2 AND leg_code = 'LEG_00A'), (SELECT id FROM dut_samples WHERE sample_code = '104207002')),
((SELECT id FROM project_legs WHERE project_id = 2 AND leg_code = 'LEG_00A'), (SELECT id FROM dut_samples WHERE sample_code = '104207003')),
((SELECT id FROM project_legs WHERE project_id = 2 AND leg_code = 'LEG_00B'), (SELECT id FROM dut_samples WHERE sample_code = '104207001')),
((SELECT id FROM project_legs WHERE project_id = 2 AND leg_code = 'LEG_00B'), (SELECT id FROM dut_samples WHERE sample_code = '104207002')),
((SELECT id FROM project_legs WHERE project_id = 2 AND leg_code = 'LEG_00B'), (SELECT id FROM dut_samples WHERE sample_code = '104207003')),
((SELECT id FROM project_legs WHERE project_id = 2 AND leg_code = 'LEG_00C'), (SELECT id FROM dut_samples WHERE sample_code = '104207001')),
((SELECT id FROM project_legs WHERE project_id = 2 AND leg_code = 'LEG_00C'), (SELECT id FROM dut_samples WHERE sample_code = '104207002')),
((SELECT id FROM project_legs WHERE project_id = 2 AND leg_code = 'LEG_00C'), (SELECT id FROM dut_samples WHERE sample_code = '104207003')),
((SELECT id FROM project_legs WHERE project_id = 2 AND leg_code = 'LEG_00D'), (SELECT id FROM dut_samples WHERE sample_code = '104207001')),
((SELECT id FROM project_legs WHERE project_id = 2 AND leg_code = 'LEG_00D'), (SELECT id FROM dut_samples WHERE sample_code = '104207002')),
((SELECT id FROM project_legs WHERE project_id = 2 AND leg_code = 'LEG_00D'), (SELECT id FROM dut_samples WHERE sample_code = '104207003')),
((SELECT id FROM project_legs WHERE project_id = 2 AND leg_code = 'LEG_00D'), (SELECT id FROM dut_samples WHERE sample_code = '104207004')),
((SELECT id FROM project_legs WHERE project_id = 2 AND leg_code = 'LEG_00D'), (SELECT id FROM dut_samples WHERE sample_code = '104207005')),
((SELECT id FROM project_legs WHERE project_id = 2 AND leg_code = 'LEG_00D'), (SELECT id FROM dut_samples WHERE sample_code = '104207006')),
((SELECT id FROM project_legs WHERE project_id = 3 AND leg_code = 'LEG_00A'), (SELECT id FROM dut_samples WHERE sample_code = '104892001')),
((SELECT id FROM project_legs WHERE project_id = 3 AND leg_code = 'LEG_00A'), (SELECT id FROM dut_samples WHERE sample_code = '104892002')),
((SELECT id FROM project_legs WHERE project_id = 3 AND leg_code = 'LEG_00A'), (SELECT id FROM dut_samples WHERE sample_code = '104892003')),
((SELECT id FROM project_legs WHERE project_id = 3 AND leg_code = 'LEG_00A'), (SELECT id FROM dut_samples WHERE sample_code = '104892004')),
((SELECT id FROM project_legs WHERE project_id = 3 AND leg_code = 'LEG_00B'), (SELECT id FROM dut_samples WHERE sample_code = '104892001')),
((SELECT id FROM project_legs WHERE project_id = 3 AND leg_code = 'LEG_00B'), (SELECT id FROM dut_samples WHERE sample_code = '104892002')),
((SELECT id FROM project_legs WHERE project_id = 3 AND leg_code = 'LEG_00B'), (SELECT id FROM dut_samples WHERE sample_code = '104892003')),
((SELECT id FROM project_legs WHERE project_id = 3 AND leg_code = 'LEG_00B'), (SELECT id FROM dut_samples WHERE sample_code = '104892004')),
((SELECT id FROM project_legs WHERE project_id = 3 AND leg_code = 'LEG_00C'), (SELECT id FROM dut_samples WHERE sample_code = '104892001')),
((SELECT id FROM project_legs WHERE project_id = 3 AND leg_code = 'LEG_00C'), (SELECT id FROM dut_samples WHERE sample_code = '104892002')),
((SELECT id FROM project_legs WHERE project_id = 3 AND leg_code = 'LEG_00C'), (SELECT id FROM dut_samples WHERE sample_code = '104892003')),
((SELECT id FROM project_legs WHERE project_id = 3 AND leg_code = 'LEG_00C'), (SELECT id FROM dut_samples WHERE sample_code = '104892004'));

INSERT INTO measurement_equipment (
    equipment_code, equipment_name, category, manufacturer, model, serial_number, calibration_valid_until, location, notes
) VALUES
('EQ-RF-001', 'EMI Receiver', 'RF', 'Rohde & Schwarz', 'ESR7', 'RS-ESR7-001', '2026-12-31', 'Lab A', 'Main receiver for radiated EMC tests'),
('EQ-AMP-002', 'RF Power Amplifier', 'RF', 'Amplifier Research', '150W1000B', 'AR-150W-002', '2026-11-15', 'Lab A', 'Used for immunity tests'),
('EQ-ESD-003', 'ESD Simulator', 'ESD', 'EM Test', 'DITO', 'EMT-DITO-003', '2026-10-20', 'Lab B', 'Gun for contact and air discharge'),
('EQ-LISN-004', 'LISN', 'Conducted', 'Teseq', 'NNBL 8227', 'TSQ-LISN-004', '2026-09-10', 'Lab C', 'Line impedance stabilization network'),
('EQ-OSC-005', 'Oscilloscope', 'Measurement', 'Tektronix', 'MSO64', 'TEK-MSO64-005', '2026-08-30', 'Lab C', 'General purpose analysis scope'),
('EQ-PSU-006', 'Programmable Power Supply', 'Power', 'Keysight', 'N7900', 'KEY-N7900-006', '2026-12-05', 'Lab D', 'Supply for electrical tests');

INSERT INTO leg_equipment_assignments (leg_id, equipment_id) VALUES
((SELECT id FROM project_legs WHERE project_id = 1 AND leg_code = 'LEG_00A'), (SELECT id FROM measurement_equipment WHERE equipment_code = 'EQ-RF-001')),
((SELECT id FROM project_legs WHERE project_id = 1 AND leg_code = 'LEG_00A'), (SELECT id FROM measurement_equipment WHERE equipment_code = 'EQ-AMP-002')),
((SELECT id FROM project_legs WHERE project_id = 1 AND leg_code = 'LEG_00B'), (SELECT id FROM measurement_equipment WHERE equipment_code = 'EQ-ESD-003')),
((SELECT id FROM project_legs WHERE project_id = 1 AND leg_code = 'LEG_00C'), (SELECT id FROM measurement_equipment WHERE equipment_code = 'EQ-LISN-004')),
((SELECT id FROM project_legs WHERE project_id = 1 AND leg_code = 'LEG_00C'), (SELECT id FROM measurement_equipment WHERE equipment_code = 'EQ-OSC-005')),
((SELECT id FROM project_legs WHERE project_id = 2 AND leg_code = 'LEG_00A'), (SELECT id FROM measurement_equipment WHERE equipment_code = 'EQ-ESD-003')),
((SELECT id FROM project_legs WHERE project_id = 2 AND leg_code = 'LEG_00B'), (SELECT id FROM measurement_equipment WHERE equipment_code = 'EQ-LISN-004')),
((SELECT id FROM project_legs WHERE project_id = 2 AND leg_code = 'LEG_00B'), (SELECT id FROM measurement_equipment WHERE equipment_code = 'EQ-OSC-005')),
((SELECT id FROM project_legs WHERE project_id = 2 AND leg_code = 'LEG_00C'), (SELECT id FROM measurement_equipment WHERE equipment_code = 'EQ-RF-001')),
((SELECT id FROM project_legs WHERE project_id = 2 AND leg_code = 'LEG_00C'), (SELECT id FROM measurement_equipment WHERE equipment_code = 'EQ-AMP-002')),
((SELECT id FROM project_legs WHERE project_id = 2 AND leg_code = 'LEG_00D'), (SELECT id FROM measurement_equipment WHERE equipment_code = 'EQ-PSU-006')),
((SELECT id FROM project_legs WHERE project_id = 2 AND leg_code = 'LEG_00D'), (SELECT id FROM measurement_equipment WHERE equipment_code = 'EQ-OSC-005')),
((SELECT id FROM project_legs WHERE project_id = 3 AND leg_code = 'LEG_00A'), (SELECT id FROM measurement_equipment WHERE equipment_code = 'EQ-RF-001')),
((SELECT id FROM project_legs WHERE project_id = 3 AND leg_code = 'LEG_00A'), (SELECT id FROM measurement_equipment WHERE equipment_code = 'EQ-AMP-002')),
((SELECT id FROM project_legs WHERE project_id = 3 AND leg_code = 'LEG_00B'), (SELECT id FROM measurement_equipment WHERE equipment_code = 'EQ-PSU-006')),
((SELECT id FROM project_legs WHERE project_id = 3 AND leg_code = 'LEG_00B'), (SELECT id FROM measurement_equipment WHERE equipment_code = 'EQ-OSC-005')),
((SELECT id FROM project_legs WHERE project_id = 3 AND leg_code = 'LEG_00C'), (SELECT id FROM measurement_equipment WHERE equipment_code = 'EQ-ESD-003'));

INSERT INTO step_equipment_assignments (leg_test_step_id, equipment_id)
SELECT lts.id, lea.equipment_id
FROM leg_equipment_assignments lea
JOIN leg_test_steps lts ON lts.leg_id = lea.leg_id;

UPDATE leg_test_steps lts
JOIN (
    SELECT sea.leg_test_step_id,
           REPLACE(MAX(me.location), ' ', '') AS inferred_room
    FROM step_equipment_assignments sea
    JOIN measurement_equipment me ON me.id = sea.equipment_id
    GROUP BY sea.leg_test_step_id
) inferred ON inferred.leg_test_step_id = lts.id
SET lts.test_room = inferred.inferred_room;

INSERT INTO dut_test_results (dut_sample_id, leg_test_step_id, condition_status, result_status, comment) VALUES
((SELECT id FROM dut_samples WHERE sample_code='103612001'), (SELECT id FROM leg_test_steps WHERE leg_id=1 AND step_order=2), 'OK', 'DATA_IN_ANALYSIS', 'Waveform under review'),
((SELECT id FROM dut_samples WHERE sample_code='103612002'), (SELECT id FROM leg_test_steps WHERE leg_id=1 AND step_order=2), 'OK', 'DATA_IN_ANALYSIS', 'Awaiting confirmation'),
((SELECT id FROM dut_samples WHERE sample_code='103612003'), (SELECT id FROM leg_test_steps WHERE leg_id=1 AND step_order=2), 'OK', 'DATA_IN_ANALYSIS', 'Preliminary analysis only'),
((SELECT id FROM dut_samples WHERE sample_code='103612001'), (SELECT id FROM leg_test_steps WHERE leg_id=3 AND step_order=1), 'OK', 'PASSED', NULL),
((SELECT id FROM dut_samples WHERE sample_code='103612002'), (SELECT id FROM leg_test_steps WHERE leg_id=3 AND step_order=1), 'OK', 'PASSED', NULL),
((SELECT id FROM dut_samples WHERE sample_code='103612003'), (SELECT id FROM leg_test_steps WHERE leg_id=3 AND step_order=1), 'OK', 'PASSED', NULL),
((SELECT id FROM dut_samples WHERE sample_code='104207001'), (SELECT id FROM leg_test_steps WHERE leg_id=4 AND step_order=1), 'OK', 'PASSED', NULL),
((SELECT id FROM dut_samples WHERE sample_code='104207002'), (SELECT id FROM leg_test_steps WHERE leg_id=4 AND step_order=1), 'OK', 'PASSED', NULL),
((SELECT id FROM dut_samples WHERE sample_code='104207003'), (SELECT id FROM leg_test_steps WHERE leg_id=4 AND step_order=1), 'OK', 'PASSED', NULL),
((SELECT id FROM dut_samples WHERE sample_code='104207001'), (SELECT id FROM leg_test_steps WHERE leg_id=5 AND step_order=1), 'OK', 'NOT_STARTED', NULL),
((SELECT id FROM dut_samples WHERE sample_code='104207002'), (SELECT id FROM leg_test_steps WHERE leg_id=5 AND step_order=1), 'OK', 'NOT_STARTED', NULL),
((SELECT id FROM dut_samples WHERE sample_code='104207003'), (SELECT id FROM leg_test_steps WHERE leg_id=5 AND step_order=1), 'OK', 'NOT_STARTED', NULL),
((SELECT id FROM dut_samples WHERE sample_code='104207001'), (SELECT id FROM leg_test_steps WHERE leg_id=6 AND step_order=2), 'OK', 'ONGOING', 'Measurement in progress'),
((SELECT id FROM dut_samples WHERE sample_code='104207002'), (SELECT id FROM leg_test_steps WHERE leg_id=6 AND step_order=2), 'OK', 'ONGOING', 'Under test'),
((SELECT id FROM dut_samples WHERE sample_code='104207003'), (SELECT id FROM leg_test_steps WHERE leg_id=6 AND step_order=2), 'OK', 'ONGOING', 'Monitoring active'),
((SELECT id FROM dut_samples WHERE sample_code='104207001'), (SELECT id FROM leg_test_steps WHERE leg_id=7 AND step_order=3), 'OK', 'ONGOING', NULL),
((SELECT id FROM dut_samples WHERE sample_code='104207002'), (SELECT id FROM leg_test_steps WHERE leg_id=7 AND step_order=3), 'OK', 'ONGOING', NULL),
((SELECT id FROM dut_samples WHERE sample_code='104207003'), (SELECT id FROM leg_test_steps WHERE leg_id=7 AND step_order=3), 'OK', 'ONGOING', NULL),
((SELECT id FROM dut_samples WHERE sample_code='104207004'), (SELECT id FROM leg_test_steps WHERE leg_id=7 AND step_order=3), 'OK', 'ONGOING', NULL),
((SELECT id FROM dut_samples WHERE sample_code='104207005'), (SELECT id FROM leg_test_steps WHERE leg_id=7 AND step_order=3), 'OK', 'ONGOING', NULL),
((SELECT id FROM dut_samples WHERE sample_code='104207006'), (SELECT id FROM leg_test_steps WHERE leg_id=7 AND step_order=3), 'OK', 'ONGOING', NULL),
((SELECT id FROM dut_samples WHERE sample_code='104892001'), (SELECT id FROM leg_test_steps WHERE leg_id=8 AND step_order=1), 'OK', 'PASSED', NULL),
((SELECT id FROM dut_samples WHERE sample_code='104892002'), (SELECT id FROM leg_test_steps WHERE leg_id=8 AND step_order=1), 'OK', 'PASSED', NULL),
((SELECT id FROM dut_samples WHERE sample_code='104892003'), (SELECT id FROM leg_test_steps WHERE leg_id=8 AND step_order=1), 'OK', 'PASSED', NULL),
((SELECT id FROM dut_samples WHERE sample_code='104892004'), (SELECT id FROM leg_test_steps WHERE leg_id=8 AND step_order=1), 'OK', 'PASSED', NULL),
((SELECT id FROM dut_samples WHERE sample_code='104892001'), (SELECT id FROM leg_test_steps WHERE leg_id=9 AND step_order=2), 'OK', 'FAILED', 'Reset after reverse polarity'),
((SELECT id FROM dut_samples WHERE sample_code='104892002'), (SELECT id FROM leg_test_steps WHERE leg_id=9 AND step_order=2), 'OK', 'FAILED', 'Communication lost'),
((SELECT id FROM dut_samples WHERE sample_code='104892003'), (SELECT id FROM leg_test_steps WHERE leg_id=9 AND step_order=2), 'NOK', 'FAILED', 'Connector damage observed'),
((SELECT id FROM dut_samples WHERE sample_code='104892004'), (SELECT id FROM leg_test_steps WHERE leg_id=9 AND step_order=2), 'OK', 'FAILED', 'Out of limit current draw'),
((SELECT id FROM dut_samples WHERE sample_code='104892001'), (SELECT id FROM leg_test_steps WHERE leg_id=10 AND step_order=1), 'OK', 'PASSED', NULL),
((SELECT id FROM dut_samples WHERE sample_code='104892002'), (SELECT id FROM leg_test_steps WHERE leg_id=10 AND step_order=1), 'OK', 'PASSED', NULL),
((SELECT id FROM dut_samples WHERE sample_code='104892003'), (SELECT id FROM leg_test_steps WHERE leg_id=10 AND step_order=1), 'OK', 'PASSED', NULL),
((SELECT id FROM dut_samples WHERE sample_code='104892004'), (SELECT id FROM leg_test_steps WHERE leg_id=10 AND step_order=1), 'OK', 'PASSED', NULL);

UPDATE projects p
LEFT JOIN (
    SELECT
        pl.project_id,
        MIN(pl.start_date) AS earliest_start,
        CASE
            WHEN COUNT(pl.id) > 0
                 AND SUM(CASE WHEN vls.leg_status IN ('PASSED', 'FAILED') THEN 1 ELSE 0 END) = COUNT(pl.id)
                 AND SUM(CASE WHEN pl.end_date IS NOT NULL THEN 1 ELSE 0 END) = COUNT(pl.id)
                THEN MAX(pl.end_date)
            ELSE NULL
        END AS latest_end
    FROM project_legs pl
    LEFT JOIN vw_leg_status vls ON vls.leg_id = pl.id
    GROUP BY pl.project_id
) summary ON summary.project_id = p.id
SET
    p.start_date = summary.earliest_start,
    p.end_date = summary.latest_end;
