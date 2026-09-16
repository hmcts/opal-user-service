# OpenAPI Guidelines

## Sources and generated output

Source contracts live under `src/main/resources/openapi`. `bundleOpenApi`
combines them into `build/openapi-bundled.yaml`; OpenAPI Generator writes Java
interfaces and models under `build/generated/openapi`. Treat both build paths
as generated output and never edit or commit them.

Bundling logic lives in the `openapi` source set under `src/openApi/java`, and
custom generator templates live under `openapi/templates`. Changes to either
are compatibility-sensitive.

## Ownership and compatibility

- Keep reusable technical components in `common.yaml` and resource-specific
  paths, requests, responses, and schemas in the narrowest owning source file.
- Use relative references between source files. Avoid premature shared schemas
  and globally ambiguous component names.
- Keep request and response ownership explicit. Do not expose server-generated,
  authenticated-user, audit, or persistence fields in requests for reuse.
- Treat path, operation, parameter, status, requiredness, validation, schema,
  and generated Java-name changes as externally visible compatibility changes.
- Update application mappings and contract tests together with source changes.

## Verification

Run:

```bash
./gradlew bundleOpenApi
./gradlew openApiGenerate compileJava --no-daemon
./gradlew build --no-daemon
```

Inspect `build/openapi-bundled.yaml` for internal references, unique component
names, and expected paths. Inspect generated model and interface names for
collisions or misleading prefixes. Do not treat YAML parsing alone as enough.
