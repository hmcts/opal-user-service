# Bruno API Collection

This directory contains the Bruno collections and environments used to explore, document,
and test the project’s REST APIs.

Bruno is a fast, Git-friendly API client designed for teams that prefer version-controlled,
text-based API collections.

```text
bruno/
├── collections/
│   ├── health/
│   │   └── HealthCheck.bru
│   ├── Auth - User1.bru
│   └── ...
│
├── environments/
│   ├── env.template        # Example env file (committed)
│   ├── local.env           # Developer-specific (ignored)
│   ├── dev.env             # Dev environment (ignored)
│   └── staging.env         # Staging environment (ignored)
│
└── config.json

```

## Getting Started

1. Install Bruno

```bash
   brew install --cask bruno
```

2. Create your environment file

Copy the template:

```bash
cp environments/env.bru.template environments/local.bru
```

Edit local.bru environment file and fill in values such as:

```bash
vars {
  baseURL: http://localhost:4555
}
vars:secret [
  BEARER_TOKEN
]
```
⚠️ Never commit .env files, or `.bru` files in the `environments` folder especially those with API keys or tokens.

3. Launch Bruno and open the collection

- Launch Bruno.
- Click on the `+` icon next to `Collections` and select `Open Collection`.
- Navigate to the `bruno` folder and click the `Open` button.

## Running Requests

Each .bru file represents a request.

You can:

- Run individual requests
- Run an entire folder as a suite
- Pass environment variables using {{VAR_NAME}} syntax

Example:

```text
GET {{BASE_URL}}/users
Authorization: Bearer {{AUTH_TOKEN}}
```

## Git & Security Guidelines

✔ Commit:

- collections/
- config.json
- env.template

❌ Do not commit:

- Any *.env file / `environments/*.bru` (files in the `environments` folder) with real values
- Sensitive tokens in request headers

## Tips for Contributors

- Keep requests small and focused.
- Group related requests into folders (users/, auth/, orders/, etc.).
- Update collections when API endpoints change.
- Include sample payloads (JSON) in the request body to help others test faster.
