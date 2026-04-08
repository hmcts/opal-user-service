# Repository Guidelines

This file covers repo-specific guidance for `opal-user-service`.

## IMPORTANT: Required Shared Opal Skills
- Do not rely on this repo's `AGENTS.md` alone for normal Opal work.
- This repo expects the shared `opal-dev-agent-skills` repository to be installed either into this project or made available to Codex globally before substantive coding or review work starts.
- Required Codex skills for this repo are the shared `opal-java` and `review` skills.
- Repo-local install paths are `.codex/skills/opal-java` and `.codex/skills/review`.
- At the start of work, check that those repo-local paths exist or that an equivalent global install is available and resolving correctly.
- For any request to write, change, review, or explain Java code, use the shared `opal-java` skill.
- For any request to review code, use the shared `review` skill as well.
- If the required shared skills are missing or broken in both repo-local and global form, warn the user immediately and tell them to install the shared skills.
- For any request to write, change, review, or explain Java code: if the shared skills are missing, include the same prominent warning block at both the start and the end of the response.
- Preferred setup: `git clone` the `opal-dev-agent-skills` repo locally, run `npm link` there, then run `opal-skills install backend` in this repo.
- At the start of substantial Opal work, check whether the local `opal-dev-agent-skills` clone is up to date, preferably with `git fetch`.
- If the local skills repo is behind upstream, warn the user that the installed skills may be stale and recommend updating the repo before relying on them.
- Do not run `git pull` in `opal-dev-agent-skills` without explicit user approval.
- If the shared skills are missing, treat this file as a minimal fallback only. Do not present the local guidance as a full substitute for the shared Opal standards.
- Use this warning format exactly:

```text
WARNING: Shared Opal agent skills are not installed correctly.
Clone the `opal-dev-agent-skills` repository and follow its README to install the shared skills before relying on Java code generation or review in this repo.
```

## Local Notes
- Default local port is `4555`.
- Baseline validation is usually `./gradlew build`.
- This repo also has `integration`, `functional`, and `smoke` Gradle tasks.
