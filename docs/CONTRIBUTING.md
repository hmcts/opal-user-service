# Contributing

## Branches

Start the first branch from an up-to-date `master`. Use the Jira key for
ticketed work, for example `PO-1234`; an execution environment may require a
namespace such as `codex/PO-1234`. Use short kebab-case names for non-ticket
maintenance.

## Stacked pull requests

Use a stack only when the change separates into independently reviewable and
buildable slices with a real dependency order.

1. Create the first branch from updated `master`.
2. Create each downstream branch from its immediate predecessor.
3. Target each pull request at its immediate predecessor until that pull
   request merges.
4. In every pull request, name the target branch, preceding dependency,
   downstream dependants, and independent verification evidence.
5. Keep each commit and pull request coherent enough to review and verify alone.
6. When an earlier branch changes, rebase downstream branches in dependency
   order and repeat affected checks.
7. When an earlier pull request merges, retarget the next pull request to
   `master`; continue through the stack in order.

Do not rewrite a shared branch or force-push without explicit approval. Agents
must not push or open a pull request unless the user asks.

## Commits

Use Conventional Commits: `<type>(<optional-scope>): <imperative summary>`.
Accepted types are `feat`, `fix`, `test`, `docs`, `refactor`, `chore`, and `ci`.
Keep subjects at or below 72 characters and exclude sensitive information.

Never stage or commit ignored Codex, Superpowers, plan, specification, evidence,
review-diff, or linked-worktree paths. Do not use `git add -f` to bypass these
rules without explicit approval for the exact file.

## Pull requests and evidence

Keep pull requests focused. Map each applicable Acceptance Criterion to its
implementation and verification. Include exact commands and results, manual
scenarios, relevant checks not run and their reasons, security assessment, and
API, migration, configuration, compatibility, deployment, rollout, rollback,
or operational impact.

Complete agent review before handoff. Validate feedback technically, resolve
blocking findings, and repeat checks affected by review fixes. Identify all
human-only verification and the environment, access, data, and expected result
needed to perform it.
