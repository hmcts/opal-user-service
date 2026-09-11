# Database Migrations

## Immutability and ordering

- Treat every committed or deployed Flyway migration as immutable. Correct an
  applied migration with a later forward migration; never edit, rename, delete,
  or renumber it.
- Discover the highest version across the complete migration tree before
  allocating a new version. Do not allocate versions independently by folder.
- Keep schema changes in the established `ddl` migration location and follow
  current environment-data conventions where they exist.
- Use descriptive file names and keep each migration focused on one coherent
  release-safe change.

## Safety analysis

Before implementation, assess supported starting schemas, PostgreSQL version,
locks, table rewrites, data volume, constraints, indexes, transactions,
concurrent application versions, environment scope, deployment order, failure
behaviour, and forward recovery.

Avoid destructive or irreversible operations unless the ticket explicitly
requires them and the delivery plan documents compatibility, backup, rollout,
and recovery. Never validate against production-derived data or expose database
credentials in commands or evidence.

## Application and contract alignment

Update affected entities, repositories, DTOs, mappings, OpenAPI contracts,
configuration, and tests with the migration. Preserve PostgreSQL numeric widths
across every boundary. Do not hide database compatibility problems with ad hoc
string or numeric conversion.

## Verification and evidence

Use the repository's configured Flyway and Gradle tasks. At minimum, verify the
new migration through the most focused applicable integration tests and the
baseline `./gradlew build`. For compatibility-sensitive changes, exercise both
a fresh database and a representative upgrade from the supported predecessor
state when the required environment is available.

Record PostgreSQL and Flyway versions, starting state, migrations applied,
assertions executed, timings, cleanup, and relevant checks not run. Evidence
must contain no credentials, reusable connection details, personal data, or
production-derived records.
