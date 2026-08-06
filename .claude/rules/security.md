---
paths:
  - "app/src/main/java/com/luc4n3x/levyra/data/network/**/*.kt"
  - "app/src/main/java/com/luc4n3x/levyra/data/security/**/*.kt"
  - "app/src/main/java/com/luc4n3x/levyra/data/Youtube*.kt"
  - "app/src/main/java/com/luc4n3x/levyra/feature/motion/**/*.kt"
  - "app/src/main/AndroidManifest.xml"
  - "desktop/src/**/*.kt"
  - ".github/workflows/**/*.yml"
  - "gradle/libs.versions.toml"
  - "app/build.gradle.kts"
---

# Security and Privacy

Invoke `levyra-security-review` before broad investigation or editing whenever
security, privacy, dependency, authentication, trust-boundary, update-integrity,
workflow-permission, or attacker-controlled input is involved.

Use the shared cross-runtime method:

1. **Threat model** — identify attacker-controlled entry points, trust
   boundaries, privileged components, sensitive assets, and verified versus
   assumed deployment conditions.
2. **Identification** — trace a concrete path from attacker input to a sensitive
   outcome; do not promote generic hardening advice into a vulnerability.
3. **Validation** — reproduce safely with local fixtures or isolated tests and
   preserve exact input, output, exit status, and assumptions.
4. **Remediation** — fix the root cause with the smallest compatible patch and
   add focused regression coverage.
5. **Human review** — inspect the complete diff and keep publication, merge,
   release, credential rotation, and repository settings owner-controlled.
6. **Revalidation** — rerun the original safe reproduction or equivalent test
   and report residual risk and blocked checks.

Keep security scans, exploit evidence, hashes, signatures, secret scans, and
exact reproduction output raw. Do not route them through RTK.

Additional invariants:

- Do not commit secrets, credentials, cookies, tokens, private headers, signing
  material, production API keys, keystores, `.env`, or `local.properties`.
- Validate provider-controlled URLs before connecting: scheme, exact/suffix
  allowlist, port, user-info, resolved destination, and every redirect hop.
- Reject loopback, unspecified, link-local, multicast, private/LAN, CGNAT, and
  other non-public destinations. Consider DNS rebinding and actual connection
  resolution.
- Do not accept media from an extension alone. Reject explicit non-media MIME
  types and cap probe size/time.
- Never log auth headers, signed URLs, tokens, cookies, full account payloads,
  or private user data.
- Use least-privilege Android and GitHub workflow permissions.
- Sanitize external text and identifiers before filenames, SQL fragments, logs,
  shell commands, intents, deep links, or archive paths.
- Review dependency additions/upgrades, action pinning, licenses, known
  vulnerabilities, and supply-chain substitution risk.
- Do not weaken security controls merely to restore compatibility.
- A suspected finding remains unconfirmed until evidence supports a concrete
  failure or attack path.

See `docs/ai/CODEX_SECURITY.md` for the shared workflow used by Claude Code,
Codex, ChatGPT Projects, and Antigravity.
