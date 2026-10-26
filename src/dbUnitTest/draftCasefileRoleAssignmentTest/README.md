# Draft Casefiles database tests

From the repository root, run:

```sh
./gradlew dbUnitTest --no-daemon
```

Docker is required. The task builds PostgreSQL 17 with pgTAP and `pg_prove`,
then uses the project's pinned Flyway version against that disposable container.
No existing database is used. External database environment settings and `-Pdburl`
are rejected. Containers are removed on success or failure. Images remain cached.

The task applies the full `ddl`, `data/allEnvs`, and `data/nle` migration chain,
validates Flyway history, checks that a second migrate adds no migration entries,
and then runs pgTAP. There is no staged upgrade target or pre-migration capture.
The V1_66 role and V1_67/V1_68 assignment SQL references in the suites must follow any future renumbering.

`create_manage_draft_casefiles_role_assignment_pgtap_tests.sql` checks:

- The exact enum, NLE test-role name, Maintenance domain, version and sole permission.
- Sequence-generated IDs, including a deliberately different allocation.
- Exactly the two approved users at BU44, with no extra BU/system grants.
- Preservation of unrelated users, Business Units, domains, events, roles,
  memberships and assignments during the controlled SQL test scenarios.
- New memberships, alternative existing IDs, partial grants and another existing
  permission; repeat assignments leave stored rows unchanged.
- Duplicate role creation fails; membership-ID collisions and an injected real
  second-insert constraint failure roll back the complete assignment operation.

The suite no longer compares the entire dataset before and after the initial
Flyway migration. Its preservation checks cover controlled post-migration fixtures.

Fixtures are rolled back. Sequence gaps are expected because PostgreSQL sequence
allocations do not roll back. Prerequisite guards deliberately removed from the
simple migrations are not asserted as migration failure cases.

The task always verifies that the deliberate failing framework fixture is rejected
by `pg_prove`; it is excluded from the normal test-file discovery. `dbUnitTest` is
part of `check` and therefore `build`. Reports are in `build/reports/dbUnitTest/`:
`pg-prove.log`, `framework-self-test.log`, `flyway.log`, and `summary.txt`.
The old standalone Python runner and ASSERT-only script have been replaced.

Migration numbering remains provisional. Already-applied migrations require an
agreed forward migration; this test task does not repair deployed Flyway history.

## Checker role (PO-10661)

`check_validate_draft_casefiles_role_assignment_pgtap_tests.sql` checks the
`CHECK_VALIDATE_DRAFT_CASEFILES` enum and the NLE-only
`TEST ONLY - Check and validate draft Casefiles` role. Only `opal-test-10`
(user 500000009) receives the checker role in BU44. An explicit negative assertion
checks that `opal-test` (500000000) has no checker grant; both retain creator access.
The suite exercises new and existing memberships, repeat assignment, generated
role IDs, duplicate role rejection, constraint rollback and unrelated-row
preservation, including creator access on an existing membership.

The consolidated migration sequence is V1_65 (both permissions), V1_66 (both
NLE roles in one INSERT), V1_67 (creator assignments) and V1_68 (checker assignment). The Java/backend
permission mapping is a separate dependency; these database tests do not prove
that the checker permission is exposed through user state.
