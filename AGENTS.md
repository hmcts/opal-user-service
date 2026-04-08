# Repository Guidelines

This file covers repo-specific guidance for `opal-user-service`.

## IMPORTANT: Required Shared Opal Skills
- Do not rely on this repo's `AGENTS.md` alone for normal Opal work.
- This repo expects the shared `opal-dev-agent-skills` repository to be cloned locally and installed into this project before substantive coding or review work starts.
- Required Codex skills in this repo are `.codex/skills/opal-java` and `.codex/skills/review`.
- At the start of work, check that those paths exist and resolve correctly.
- For any request to write, change, review, or explain Java code, use `.codex/skills/opal-java`.
- For any request to review code, use `.codex/skills/review` as well.
- If either required skill is missing or broken, warn the user immediately and tell them to install the shared skills.
- For any request to write, change, review, or explain Java code: if the shared skills are missing, include the same prominent warning block at both the start and the end of the response.
- Preferred setup: `git clone` the `opal-dev-agent-skills` repo locally, run `npm link` there, then run `opal-skills install backend` in this repo.
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
