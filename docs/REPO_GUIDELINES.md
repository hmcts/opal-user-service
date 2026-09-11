# Repository Guidelines

This document is the authoritative implementation guide for
`opal-user-service`. Executable configuration wins when it disagrees with a
documented fact; correct the documentation in the same change.

## Project structure

- Keep production Java under `src/main/java` and mirror its packages in the
  applicable test source set.
- Keep controllers, services, repositories, configuration, clients, DTOs,
  entities, mappers, and exception handling with their owning feature and
  existing package conventions.
- Keep runtime configuration, JSON schemas, OpenAPI source, and Flyway
  migrations under `src/main/resources` in their existing directories.
- Use `src/test/java` for unit tests, `src/integrationTest/java` for Spring and
  database integration tests, and `src/functionalTest` for Cucumber functional,
  toggle, and smoke scenarios.
- Treat `build/generated/openapi` and `build/openapi-bundled.yaml` as generated
  output. Do not edit or commit them.
- Keep Helm configuration under `charts/`, security and quality configuration
  under `config/`, OpenAPI templates under `openapi/templates/`, and local
  helpers under `bin/`.

## Toolchain and commands

Use Java 21 and the checked-in Gradle wrapper. Treat `build.gradle` and wrapper
configuration as authoritative for framework, plugin, and dependency versions.

- Unit: `./gradlew test`
- Integration: `./gradlew integration`
- Baseline: `./gradlew build`
- Style: `./gradlew runAllStyleChecks`
- Coverage: `./gradlew jacocoTestReport`
- Functional: `./gradlew functionalOpal`, `./gradlew functionalOpalToggle`,
  `./gradlew functionalLegacy`, or `./gradlew functional`
- Smoke: `./gradlew smoke`
- OpenAPI: `./gradlew bundleOpenApi openApiGenerate`
- Local service: `docker compose up --build`

Docker is required for Testcontainers-backed integration tests and for normal
local PostgreSQL and Redis dependencies. Functional and smoke checks require a
suitable running service. Compilation or task discovery alone is not evidence
that changed behaviour works.

## Java and Spring Boot

- Follow `.editorconfig`, configured Checkstyle rules, Java 21, and nearby
  maintained naming and package patterns.
- Prefer constructor injection and explicit, narrowly scoped configuration.
- Keep controllers responsible for HTTP translation, services responsible for
  orchestration and transaction boundaries, and repositories responsible for
  persistence.
- Keep API models separate from entities and integration models; map data
  deliberately across boundaries.
- Validate untrusted input at the boundary and preserve useful failure context.
- Avoid static mutable state, hidden network calls, field injection, wildcard
  imports, and work performed as an object-construction side effect.

## Persistence, Redis, and transactions

- Use configured datasource, transaction manager, repositories, and Redis
  infrastructure rather than creating independent clients or connections.
- Keep database and cache access out of controllers.
- Make query cardinality, fetch behaviour, locking, transaction ownership,
  cache keys, expiry, invalidation, and failure behaviour explicit.
- Default new JPA associations to lazy loading and fetch required graphs
  deliberately. Avoid unbounded reads and per-row query patterns.
- Preserve PostgreSQL numeric widths across persistence, DTO, API, and test
  boundaries.

## HTTP, security, and integrations

- Follow [OpenAPI Guidelines](OPENAPI_GUIDELINES.md) whenever an HTTP contract
  changes.
- Preserve `/health` and other established operational endpoints and keep the
  local port configurable with default `4555`.
- Keep authentication and authorisation deny-by-default unless an existing
  public endpoint or explicit requirement says otherwise.
- Validate values before using them in SQL, paths, URLs, logs, SFTP locations,
  file names, cache keys, or downstream requests.
- Keep OpenFeign, SFTP, file, blob-storage, and other external calls behind
  explicit boundaries with deterministic timeout and failure behaviour.
- Never log credentials, connection strings, tokens, personal data, or complete
  sensitive payloads.

## Flyway and configuration

- Follow [Database Migrations](DATABASE_MIGRATIONS.md) for every migration.
- Keep safe local defaults and preserve environment-variable and mounted-secret
  overrides. Do not hard-code environment-specific hosts or credentials.
- Keep Helm, Docker, and application configuration aligned when configuration
  changes. Document defaults, secret requirements, rollout, rollback, and
  compatibility implications.

## Testing, dependencies, and delivery

- Follow [Testing](TESTING.md); choose the narrowest suite that proves changed
  behaviour and run broader regression checks proportionate to risk.
- Do not weaken security, quality, test, or dependency controls to make a check
  pass. Keep suppressions narrow, justified, and time-bounded where supported.
- Do not change dependencies or generated dependency state unless required.
- Update documentation when behaviour, configuration, APIs, migrations,
  integrations, or workflows change.
- Follow [Contributing](CONTRIBUTING.md) and report exact verification evidence
  plus API, migration, configuration, deployment, and operational impact.
