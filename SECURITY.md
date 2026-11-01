# Security Policy

## Reporting a vulnerability

Do not open a public issue for a suspected vulnerability. Use GitHub's private
vulnerability reporting feature for this repository when available. Include
reproduction steps, affected versions, and the expected impact.

Do not include live credentials, JWTs, database connection strings, or personal
data in a report. Revoke any credential that may have been exposed.

## Deployment guidance

- Use HTTPS for every non-local API deployment.
- Generate `JWT_SECRET` once with at least 32 random characters, store it in the
  deployment platform's secret settings, and reuse it across normal deploys.
- Restrict MongoDB network access and use a dedicated database account.
- Never commit `.env`, `.run/`, signing keys, `google-services.json`, or
  `android/local.properties`.
