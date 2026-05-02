# EMC Management System

Desktop application for planning, executing and documenting EMC laboratory projects.  
The system manages EWR projects, LEG structures, test flow, DUT samples, measurement equipment, climatic data, audit history and PDF reports.

## What the project contains

- Java desktop application built with Swing
- MySQL-based data model accessed through JDBC
- role-based access for `ADMIN`, `VE`, `TE`, `TT`
- project explorer with EWR filtering and sorting
- LEG and test-flow management
- DUT assignment, per-test exceptions and result tracking
- equipment catalog and reservation calendar
- climatic data import and chart generation
- audit log
- media storage for DUT photos, test setup photos and verification captures
- EMC PDF report generator with PCA accreditation handling and draft watermark
- automated tests for selected core services and utilities

## Tech stack

- Java 17
- Swing
- JDBC
- MySQL / MariaDB
- Maven
- Apache PDFBox
- JUnit 5

## Two ways to test the application

### 1. Source version

Recommended for developers, reviewers and anyone who wants to inspect the code.

Requirements:

- JDK 17+
- Maven 3.9+
- local MySQL/MariaDB instance, or XAMPP

### 2. Portable Windows release

Recommended for non-technical testers.

The portable package:

- does **not** require a separately installed MySQL server
- does **not** require a separately installed JRE
- starts from `EMC Management System.exe`
- includes a local bundled database runtime and preloaded demo data

This package should be published on **GitHub Releases**, not committed into the repository.

## Project structure

```text
src/main/java          Application source code
src/main/resources     SQL scripts, images, default config
src/test/java          Automated tests
.github/workflows      GitHub Actions CI
```

Ignored at repository level:

```text
target/                Maven build output
out/                   Local verification output
production/            Local IDE build output
generated-reports/     Runtime generated PDFs
release/               Local packaging/release artifacts
app-local.properties   Machine-specific configuration
```

## Configuration

Default configuration lives in:

- [src/main/resources/app.properties](src/main/resources/app.properties)

Machine-specific overrides should be placed in:

- `app-local.properties`

Template:

- [app-local.properties.example](app-local.properties.example)

Configuration priority:

1. `app.properties`
2. optional `app-local.properties`
3. JVM system properties
4. environment variables, for example:
   - `DB_URL`
   - `DB_USER`
   - `DB_PASSWORD`
   - `BRANDFETCH_API_KEY`
   - `BRANDFETCH_CLIENT_ID`

## Important security note

This repository is prepared so that **no local secrets should be committed**.

Do **not** store in Git:

- production database credentials
- local database dumps containing private customer data
- API keys or Brandfetch credentials
- generated reports containing real customer data
- local portable release packages

The versioned `app.properties` file contains only safe defaults.  
If you need local credentials, place them in `app-local.properties`, which is ignored by Git.

## Database setup for source version

### Fresh local database

1. Create the database:

```sql
CREATE DATABASE emc_management_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. Import schema:

```bash
mysql -u root emc_management_system < src/main/resources/sql/01_schema.sql
```

3. Import demo data:

```bash
mysql -u root emc_management_system < src/main/resources/sql/02_demo_data.sql
```

4. Optionally import the expanded demo set:

```bash
mysql -u root emc_management_system < src/main/resources/sql/08_expanded_demo_seed.sql
```

### Existing local development database

If the database already exists from an older version, apply migrations in order:

- `03_upgrade_existing_db.sql`
- `04_admin_and_step_equipment.sql`
- `05_step_dut_and_climate.sql`
- `06_leg_documents_in_db.sql`
- `07_project_dates.sql`
- `09_equipment_and_dut_metadata.sql`
- `10_audit_log_and_reservations.sql`
- `11_climate_sensor_code.sql`
- `12_dut_serials_and_media.sql`
- `13_dut_serial_backfill_fix.sql`

## Build and run

### Run tests

```bash
mvn test
```

### Run the application from sources

```bash
mvn exec:java
```

### Build artifacts

```bash
mvn clean package
```

Build output:

- regular JAR:
  `target/emc-management-system-1.0-SNAPSHOT.jar`
- runnable JAR with dependencies:
  `target/emc-management-system-1.0-SNAPSHOT-jar-with-dependencies.jar`

Run the packaged application:

```bash
java -jar target/emc-management-system-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## Portable release for testers

The best way to let anyone test the application from GitHub is:

1. publish the **source code** in the repository,
2. publish the **portable ZIP** on the **Releases** page.

Recommended release asset name:

- `EMC-Management-System-Portable.zip`

That package should contain:

- `EMC Management System.exe`
- bundled runtime
- bundled local database runtime
- preloaded demo data
- README for testers

This keeps the repository clean while still making the app easy to test.

## Demo accounts

The demo dataset contains example accounts such as:

- `karolnawrocki / test123`
- `joannaskrzypek / test123`
- `piotrgrodny / test123`
- `admin / admin123`

These are **demo-only** credentials for seeded example data.

## Helper runners

The project contains helper entry points used during development:

- `pl.emcmanagement.app.MainApp`
- `pl.emcmanagement.app.PortableLauncher`
- `pl.emcmanagement.app.ProjectReportRunner`
- `pl.emcmanagement.app.WorkflowSyncRunner`
- `pl.emcmanagement.app.DemoDatasetRunner`
- `pl.emcmanagement.app.DemoClimateImportRunner`
- `pl.emcmanagement.app.DemoMediaSeederRunner`
- `pl.emcmanagement.app.PasswordMigrationRunner`

## Notes

- On startup the application may verify DB connectivity, migrate legacy plaintext passwords, synchronize workflow state and import desktop climate files if available.
- PDF reports are generated into `generated-reports/`, which is intentionally ignored by Git.
