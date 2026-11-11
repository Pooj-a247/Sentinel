# Sentinel

**Digital Evidence Management and Chain-of-Custody Tracking System**

Sentinel is an academic software project for registering digital evidence, recording custody activity, and checking whether stored evidence files have changed. It provides a web interface backed by a Java/Spring Boot application and a MySQL database.

> **Project status:** Academic/demo application. It is not a certified digital-forensics platform and must not be used as the sole source of evidence in legal or operational investigations.

## Core capabilities

- User authentication and application access.
- Case and evidence management.
- Evidence registration with file metadata and SHA-256 integrity hashes.
- Evidence integrity verification against the recorded hash.
- Evidence custody and transfer records.
- Custody history/timeline views.
- Integrity alerts for detected file changes or missing files.
- Dashboard summaries and CSV export.

## Technology stack

- **Backend:** Java 21, Spring Boot, Maven
- **Database:** MySQL 8+
- **Frontend:** HTML, CSS, vanilla JavaScript
- **Integrity check:** SHA-256 hashing

## Prerequisites

- JDK 21
- MySQL 8 or later
- Maven 3.9 or later

## Run locally (Windows PowerShell)

1. Create a MySQL database named `sentinel`.
2. Review `database/schema.sql` and apply it for a new installation. If you already have a Sentinel database, back it up and verify its schema before making changes. `CREATE TABLE IF NOT EXISTS` does not migrate existing tables.
3. From the project root (the directory containing `pom.xml`), configure the database and first-run administrator credentials:

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/sentinel?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USER = "root"
$env:DB_PASSWORD = Read-Host "Enter your MySQL password"
$env:SENTINEL_ADMIN_USERNAME = "admin"
$env:SENTINEL_ADMIN_PASSWORD = Read-Host "Choose an administrator password"
```

4. Build and start the application:

```powershell
mvn clean test
mvn spring-boot:run
```

5. Open [http://localhost:8080](http://localhost:8080).

Keep passwords in environment variables or a local untracked configuration file; never commit real credentials.

## Evidence integrity: how it works

1. A user registers or uploads an evidence file through the application.
2. Sentinel calculates a SHA-256 digest and stores it with the evidence record.
3. During integrity verification, the current file is hashed again.
4. The calculated digest is compared with the stored digest. A mismatch or missing file can be recorded as an integrity issue or alert.

A matching hash indicates that the bytes checked match the bytes represented by the stored digest. It does **not** independently prove who created the file, when it was created, or that the original hash and database records were never altered.

## Demonstration flow

A typical demonstration can cover:

1. Sign in with a demo account.
2. Open or create a case.
3. Register an evidence item.
4. Show the evidence identifier and generated SHA-256 hash.
5. Run integrity verification and show the result.
6. Demonstrate a controlled change to a disposable test file and verify that the mismatch is detected.
7. Record a custody transfer and inspect the custody history.
8. Show any resulting integrity alert and the dashboard/export views.

Use synthetic evidence and demo accounts only. Do not use private, sensitive, or real investigative material in a public demonstration.

## Configuration

| Variable | Purpose |
|---|---|
| `DB_URL` | JDBC connection URL |
| `DB_USER` | Database username |
| `DB_PASSWORD` | Database password |
| `PORT` | HTTP port (defaults to `8080`) |
| `SENTINEL_ADMIN_USERNAME` | First-run administrator username |
| `SENTINEL_ADMIN_PASSWORD` | First-run administrator password |
| `SENTINEL_STORAGE_DIR` | Evidence storage directory |
| `SENTINEL_MONITOR_ENABLED` | Enable/disable integrity monitor |
| `SENTINEL_MONITOR_INTERVAL_MS` | Monitor interval in milliseconds |

## Tests

Run the test suite with `mvn test`.

## Security and limitations

Read [SECURITY.md](SECURITY.md) before running or demonstrating the application. Sentinel is a learning/demo implementation. A production-grade evidence platform would require independent security review, hardened authentication and authorization, immutable/auditable logging, protected key and hash management, secure backups, deployment hardening, and validation against applicable forensic procedures.

## License

No open-source license has been added yet. Unless a license is added, standard copyright restrictions apply; do not assume this repository grants permission to reuse or redistribute the code.
