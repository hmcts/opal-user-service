# Repository instructions

Opal User Service is a Java 21 Spring Boot service for user, role, permission,
and business-unit capabilities. It uses PostgreSQL, Redis, Flyway, generated
OpenAPI interfaces, and HTTP/SFTP integration boundaries.

The default local port is `4555`; the health endpoint is `/health`.

## Required shared Opal skills

- Do not rely on this repository's `AGENTS.md` alone for substantive Opal work.
- Before substantive coding or review, confirm the shared `opal-java` and
  `review` skills are available globally or at `.codex/skills/opal-java` and
  `.codex/skills/review`.
- Use `opal-java` for requests to write, change, review, or explain Java code.
- Use `review` as well for code-review requests.
- Preferred setup: clone `opal-dev-agent-skills`, run `npm link` there, then
  run `opal-skills install backend` in this repository.
- Check whether the local skills clone is current, preferably with `git fetch`.
  Do not run `git pull` in the skills repository without explicit approval.
- If the skills are missing or broken, show this warning at the start and end
  of every response involving Java generation, changes, explanation, or review:

```text
WARNING: Shared Opal agent skills are not installed correctly.
Clone the `opal-dev-agent-skills` repository and follow its README to install the shared skills before relying on Java code generation or review in this repo.
```

## Before making changes

- Read the ticket and Acceptance Criteria when present.
- Inspect `git status` and preserve unrelated work.
- Use a dedicated branch and follow [Contributing](docs/CONTRIBUTING.md),
  including its stacked pull-request rules when work is split across branches.
- Route implementation to [Repository Guidelines](docs/REPO_GUIDELINES.md),
  testing to [Testing](docs/TESTING.md), database work to
  [Database Migrations](docs/DATABASE_MIGRATIONS.md), OpenAPI work to
  [OpenAPI Guidelines](docs/OPENAPI_GUIDELINES.md), and review work to
  [Code Review Guidelines](docs/CODE_REVIEW_GUIDELINES.md).

## Always

- Keep changes focused, preserve behaviour, follow nearby maintained patterns,
  and avoid speculative abstractions and unrelated refactors.
- Never add secrets, credentials, tokens, personal data, or production-derived
  records to code, configuration, logs, comments, fixtures, evidence, or tests.
- Treat applied Flyway migrations as immutable.
- Add or update relevant tests and documentation.
- Do not change dependencies unless the task requires it.
- Do not force-add ignored files unless the user explicitly approves the exact
  file.
- Keep Superpowers specifications, plans, task briefs, reports, review diffs,
  and Codex-local files ignored and local-only. Never stage, commit, or relocate
  them to bypass ignore rules.
- Never push, open a pull request, rewrite shared history, or force-push unless
  the user explicitly asks.

## Commands

- `./gradlew test`
- `./gradlew integration`
- `./gradlew build`
- `./gradlew functionalOpal`
- `./gradlew functionalOpalToggle`
- `./gradlew functionalLegacy`
- `./gradlew functional`
- `./gradlew smoke`
- `./gradlew runAllStyleChecks`
- `./gradlew jacocoTestReport`
- `./gradlew bundleOpenApi`
- `./gradlew openApiGenerate`
- `docker compose up --build`

Follow [Testing](docs/TESTING.md) for suite semantics and prerequisites.

## Verification and handoff

- Review the final diff and run checks proportionate to the change.
- Report exact commands and results, and list checks not run with reasons.
- Record API, migration, configuration, security, deployment, and operational
  implications when relevant.
- Complete the repository-required agent review before handoff and resolve
  validated blocking findings.
- Do not claim unverified external steps.
