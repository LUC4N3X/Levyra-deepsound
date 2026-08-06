# Codex Security for Levyra

## Purpose

Levyra uses Codex Security as an optional security-research layer, combined with
the repository-native `levyra-security-review` skill and the normal human review
and CI process.

Codex Security is not treated as a generic static-analysis badge. Its useful
workflow is closed-loop:

```text
threat model
→ identification
→ validation in an isolated environment
→ minimal remediation
→ human review
→ revalidation
```

A suspected issue is not a confirmed vulnerability until the attack path or
security failure is supported by evidence.

## Installation

The recommended opt-in Codex plugins live in `codex-plugins.txt`:

```text
superpowers@openai-curated
codex-security@openai-curated
```

Install them through Levyra's setup scripts only when the current owner has
explicitly requested plugin installation.

Windows:

```powershell
.\scripts\setup-ai.ps1 -Plugins
```

Linux/macOS:

```bash
./scripts/setup-ai.sh --plugins
```

Plugin availability depends on the active Codex account, plan, workspace policy,
and service authorization. Installation does not grant new repository rights by
itself. Restart Codex or begin a new session after installation.

Codex Security may also be used through its official CLI when the account has
access and the required runtime is available. Authentication and scan execution
must remain explicit; do not add API keys or credentials to the repository.

## Automatic skill routing

Security work automatically matches
`.agents/skills/levyra-security-review/SKILL.md` when it involves:

- vulnerability scans or security findings;
- attacker-controlled input or trust-boundary changes;
- authentication, tokens, cookies, keys, signing, update integrity, or secrets;
- URLs, redirects, SSRF, MIME, file paths, archives, shell, SQL, or deep links;
- Android permissions, exported components, Desktop listeners, IPC, or local
  storage;
- workflow permissions, action pinning, artifacts, dependency changes, or
  supply-chain risk;
- privacy-sensitive logs, account data, history, or analytics.

Load the matching Levyra domain skill as well. For example, an extractor SSRF
review should load both `levyra-security-review` and `levyra-extractor`.

## Threat model

Before accepting findings, verify the threat model against Levyra's actual
architecture and deployment assumptions.

At minimum identify:

- attacker-controlled entry points;
- Android, Desktop, extractor, CI, update, and distribution trust boundaries;
- privileged components and high-impact code paths;
- credentials, cookies, signing material, user data, downloads, update
  metadata, and other sensitive assets;
- assumptions that are verified, uncertain, or environment-specific.

The threat model must remain inspectable and correctable. Do not accept a scan
that assumes nonexistent internet exposure, privileges, account access, or
server behavior.

## Identification

Trace a concrete path from attacker-controlled input to a sensitive outcome.
Examples include:

- a provider URL crossing into an internal or local destination;
- an unvalidated redirect bypassing host checks;
- a malicious MIME type or filename reaching storage or execution;
- an exported Android component accepting unauthorized input;
- a workflow executing untrusted pull-request code with elevated permissions;
- a dependency change introducing a known high-severity vulnerability;
- an update or artifact being accepted without integrity verification.

Generic hardening advice is useful documentation but is not automatically a
security finding.

## Validation

Validate safely in an isolated or controlled environment before treating a
finding as confirmed.

Preserve:

- the exact safe reproduction command or test;
- attacker-controlled input or fixture;
- output, exit status, and relevant logs;
- proof that the sensitive outcome is reachable;
- assumptions and environmental requirements.

Do not use RTK filtering for exact exploit evidence, security scan results,
signatures, hashes, secret scans, or validation output. Security evidence stays
raw.

Never perform destructive validation, persistence, credential theft, external
targeting, production-impacting tests, or uncontrolled proof-of-concept
activity. Use synthetic credentials and minimal local fixtures.

## Remediation

For validated findings:

1. fix the root cause with the smallest compatible patch;
2. preserve unrelated behavior;
3. add a focused regression test or exact verification;
4. inspect the full diff;
5. run the applicable Levyra domain, security, and CI checks;
6. keep the patch reviewable and owner-controlled.

Codex Security must propose changes for human review. It must not merge, publish,
release, rotate credentials, change repository settings, or deploy fixes without
separate explicit authorization.

## Revalidation

After the patch, rerun the original safe reproduction or an equivalent
regression test. Report:

- whether the original attack path is closed;
- which checks passed;
- which checks were blocked and why;
- whether assumptions changed;
- remaining or transferred risk.

A patch is not complete merely because it compiles or because the generated
finding disappears.

## Dependency Review gate

`.github/workflows/dependency-review.yml` runs
`actions/dependency-review-action` on pull requests and fails for newly
introduced dependencies with high or critical known severity.

This is a supply-chain gate, not a replacement for source review. It does not
prove that a dependency is trustworthy, correctly configured, free of unknown
vulnerabilities, or appropriate for Levyra's privacy and licensing needs.

## Relationship with RTK and Superpowers

- Superpowers supports planning, debugging, tests, and delivery discipline.
- RTK reduces repetitive non-sensitive command output.
- Codex Security builds and validates security findings.
- `levyra-security-review` supplies Levyra-specific trust boundaries and review
  requirements.
- GitHub Dependency Review blocks known high-severity dependency regressions.
- CI, CodeRabbit, SonarQube, manual review, and owner approval remain separate
  gates.

No one tool is treated as proof of security on its own.

## Reference implementation

This integration follows the security priority described by
`ChrisTitusTech/titus-ai` while keeping Levyra's repository-specific controls.
The Dependency Review workflow pins the same reviewed GitHub Action revision and
fails on high severity. Codex Security setup and behavior follow OpenAI's
published plugin and product workflow.
