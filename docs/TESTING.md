# Testing

## Test levels

- **Unit:** `src/test/java`; run `./gradlew test`. Unit tests must not require
  Docker, PostgreSQL, Redis, or external services.
- **Integration:** `src/integrationTest/java`; run `./gradlew integration`.
  These tests start a Spring context and use Testcontainers, so Docker is
  required.
- **Functional Opal:** run `./gradlew functionalOpal` against a suitable running
  service. Scenarios come from `features/functional`.
- **Functional toggle:** run `./gradlew functionalOpalToggle` only against an
  environment with the expected feature-flag state. Scenarios come from
  `features/toggle`.
- **Functional legacy:** run `./gradlew functionalLegacy` against a compatible
  legacy-mode environment.
- **Functional aggregate:** run `./gradlew functional` for Opal and legacy
  execution plus Serenity report aggregation.
- **Smoke:** run `./gradlew smoke` against a suitable running service. Smoke
  scenarios come from `features/smoke`.

External suites use `TEST_URL`, defaulting to `http://localhost:4555`. Never put
tokens, credentials, or sensitive values in tracked files or command arguments.

## Baseline and focused checks

`./gradlew build` is the baseline for source, build, and runtime-configuration
changes. The Gradle `check` lifecycle includes integration tests. Run focused
unit or integration tests with `--tests 'fully.qualified.Pattern'` before the
baseline build when developing a narrow change.

Use `./gradlew runAllStyleChecks` for all configured source-set Checkstyle tasks
and `./gradlew jacocoTestReport` for combined unit and integration coverage.

For documentation-only changes, validate links, formatting, and repository
rules directly. Record why an application build was not run.

## OpenAPI verification

After changing source contracts, bundling, templates, or generation settings,
run:

```bash
./gradlew bundleOpenApi
./gradlew openApiGenerate compileJava --no-daemon
./gradlew build --no-daemon
```

Inspect `build/openapi-bundled.yaml` and generated model/interface names. Do not
treat YAML parsing alone as sufficient contract validation.

## Test design and evidence

- Assert observable behaviour rather than private implementation details.
- Cover changed success, validation, error, empty, authorisation, transaction,
  cache, configuration, and integration paths where applicable.
- Use real DTOs and value objects; mock collaborators and external boundaries.
- Keep tests deterministic and independent of developer-owned data.
- Record exact commands, results, environment, and manual scenarios.
- For every relevant check not run, record the reason, required setup, scenario,
  and expected result.
- A successful pipeline is not evidence that every changed scenario executed.
