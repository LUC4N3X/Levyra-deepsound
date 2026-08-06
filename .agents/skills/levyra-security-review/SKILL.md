---
name: levyra-security-review
description: Perform an evidence-based Levyra security review and Codex Security workflow covering threat modeling, attack paths, validation, remediation, revalidation, secrets, provider URLs, redirects, SSRF, MIME confusion, permissions, privacy, logging, workflows, dependency changes, update verification, and untrusted input. Use automatically for vulnerability scans, security findings, dependency risk, authentication, trust-boundary changes, sensitive data, or security-related pull requests.
---

# Levyra security review workflow

## Required context

1. Read the root `AGENTS.md` and the nearest applicable `AGENTS.md`.
2. Read `.claude/skills/levyra-security-review/SKILL.md`,
   `.claude/rules/security.md`, and `docs/ai/CODEX_SECURITY.md`.
3. Inspect the complete diff plus surrounding code, tests, build files,
   manifests, dependency catalogs, clients, parsers, shell commands,
   persistence, signing, release, update, and GitHub workflow configuration.
4. Load `levyra-context-efficiency` only for noisy non-sensitive output. Keep
   exploit evidence, security validation, signatures, checksums, secrets scans,
   and exact reproduction output raw.

## Closed-loop security method

Use this workflow whether the review is performed manually, through the Codex
Security plugin, or through the Codex Security CLI.

### 1. Threat model

Build or verify a codebase-specific threat model before claiming a finding:

- identify attacker-controlled entry points;
- identify trust boundaries and privileged components;
- identify secrets, accounts, user data, signing material, update channels, and
  other high-impact assets;
- identify Android, Desktop, CI, extractor, playback, storage, and network paths
  where untrusted data crosses a boundary;
- state deployment assumptions and distinguish verified facts from assumptions.

### 2. Identification

Trace realistic attack paths from an entry point to a sensitive outcome. Do not
promote a generic best-practice observation into a vulnerability without a
concrete path, trigger, and consequence.

### 3. Validation

Attempt to reproduce the issue safely in an isolated or controlled environment.
A suspected issue remains unconfirmed until evidence supports exploitability or
a concrete security failure. Preserve the exact command, input, output, exit
status, and relevant artifact or test result.

Never run destructive, persistence, credential-theft, external-target, or
production-impacting proof-of-concept activity. Use minimal local fixtures and
synthetic secrets.

### 4. Remediation

For a validated issue, propose the smallest compatible patch that fixes the
root cause. Preserve unrelated behavior and add a focused regression test or
verification. Do not weaken security controls merely to restore compatibility.

### 5. Human review

Codex Security findings and patches are proposals, not automatic authority.
Inspect the complete patch, run the normal Levyra review and CI gates, and keep
commit, push, PR, merge, release, and repository-setting actions under explicit
owner control.

### 6. Revalidation

After remediation, rerun the original safe reproduction or equivalent
regression test. State whether the attack path is closed, which checks passed,
which checks were blocked, and what residual risk remains.

## Review areas

- credentials, tokens, cookies, keys, signed URLs, keystores, private
  configuration, environment variables, and sensitive logs;
- provider-controlled URL scheme, host, port, user-info, DNS/IP destination,
  redirects, MIME, timeout, response-size, and file-name handling;
- automatic redirects that bypass explicit validation;
- SQL, shell, intent, deep-link, path, archive, and filename injection;
- Android permissions, exported components, pending intents, file providers,
  WebView behavior, and least privilege;
- Desktop local listeners, IPC, downloads, update channels, libVLC input, and
  filesystem boundaries;
- GitHub workflow permissions, pull-request trust boundaries, secret exposure,
  artifact handling, action pinning, and untrusted checkout execution;
- dependency additions, upgrades, transitive risk, license changes, known
  vulnerabilities, and supply-chain substitution;
- update manifests, download integrity, SHA-256/signature verification, and
  downgrade or substitution risks;
- local account, crash, analytics, history, library, and playback-data privacy.

## Codex Security integration

When `codex-security@openai-curated` is available, use it for security scans and
combine its output with this Levyra-specific skill. Review the generated threat
model and correct assumptions before accepting findings. Prefer validated
findings with a reproduced attack path and minimal remediation patch.

The repository also runs GitHub Dependency Review for pull requests. A green
dependency review does not replace threat modeling, source review, runtime
validation, or manual approval.

## Finding standard

Report only evidence-backed findings. Every finding must include:

- severity and confidence;
- exact file and line or symbol;
- attacker-controlled input or triggering condition;
- trust boundary crossed;
- concrete exploit or failure path;
- validation or reproduction evidence;
- user/system consequence;
- smallest compatible fix;
- regression test or revalidation needed;
- residual risk or blocked evidence.

Do not report generic best-practice observations without a concrete path to
harm. Do not label an unvalidated suspicion as confirmed.
