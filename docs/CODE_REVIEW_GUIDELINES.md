# Code Review Guidelines

## Objective

Review changed code and affected behaviour. Report concrete correctness,
security, data, API, integration, migration, operational, or delivery risks
introduced or worsened by the change. Do not report deterministic formatting,
unsupported preferences, or unrelated legacy issues as findings.

## Severity

- **P0 critical:** widespread or irreversible harm, sensitive-data exposure,
  or critical compromise.
- **P1 high:** supported behaviour, security, data integrity, API compatibility,
  migration safety, or deployment is materially broken.
- **P2 medium:** a reproducible material defect with limited impact.

Optional improvements remain advisory unless requested.

## Review checks

- Request/response validation, authorisation, exception mapping, and HTTP/API
  compatibility.
- Transaction boundaries, query and fetch behaviour, cache consistency,
  locking, affected-row scope, and data integrity.
- Flyway ordering, immutability, environment scope, rollback/recovery planning,
  and deployment compatibility.
- OpenFeign, Redis, SFTP, file, blob-storage, and other integration timeout,
  retry, idempotency, and failure behaviour.
- Secret and personal-data handling, log safety, configuration defaults,
  operational endpoints, dependency changes, and suppressions.
- Test coverage for likely regressions and evidence for required checks.
- For stacked work, correctness of the declared base, dependency order, and
  cross-branch interfaces.

## Output

Order findings by severity. Each finding must identify its severity, concise
title, smallest useful line range, triggering scenario, impact, and practical
correction. Separate assumptions and advisory feedback from findings, and state
explicitly when no qualifying findings exist.
