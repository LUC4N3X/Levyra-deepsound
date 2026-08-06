# Cross-Runtime Security Workflow for Levyra

## Purpose

Levyra uses one evidence-based security workflow across Codex, Claude Code,
ChatGPT Projects, Google Antigravity, OpenCode, and compatible coding agents.
Codex Security can be added as an optional security engine, but the canonical
project procedure lives in `levyra-security-review` and remains runtime-
independent.

```text
threat model
→ identification
→ validation in an isolated environment
→ minimal remediation
→ human review
→ revalidation
```

A suspected issue is not a confirmed vulnerability until evidence supports the
attack path or concrete security failure.

## Runtime integration

### Codex

Codex automatically discovers
`.agents/skills/levyra-security-review/SKILL.md`. Codex Security may be enabled
through the official Codex Security / Plugin Directory setup available to the
active account and workspace. Levyra does not invent a CLI manifest identifier
for it.

The verified CLI-installable plugin manifest remains `codex-plugins.txt` and is
installed only when explicitly requested through the setup scripts.

### Claude Code

Claude uses `.claude/rules/security.md` plus the `UserPromptSubmit` hook. The
hook recognizes vulnerability, exploit, CVE, trust-boundary, dependency,
supply-chain, secret, permission, privacy, integrity, and update-security work
and requires `levyra-security-review` before editing.

### ChatGPT Project

`docs/ai/CHATGPT_PROJECT_INSTRUCTIONS.md` requires ChatGPT to read matching
native skills, including `levyra-security-review`, before giving security
conclusions or preparing implementation tasks. ChatGPT must distinguish
assumptions, suspected findings, validated findings, proposed patches, applied
patches, CI results, and publication state.

### Google Antigravity

Antigravity uses `.agents/rules/levyra-workspace.md` and shared skills under
`.agents/skills/`. The always-on workspace rule automatically routes security
work to `levyra-security-review` and keeps exact security evidence raw.

## Threat model

Before accepting findings, identify:

- attacker-controlled entry points;
- Android, Desktop, extractor, CI, update, and distribution trust boundaries;
- privileged components and high-impact code paths;
- credentials, cookies, signing material, user data, downloads, update
  metadata, and other sensitive assets;
- verified, uncertain, and environment-specific assumptions.

Correct the threat model when it assumes nonexistent internet exposure,
privileges, account access, server behavior, or deployment topology.

## Identification

Trace a concrete path from attacker-controlled input to a sensitive outcome.
Examples include:

- a provider URL reaching a local or internal destination;
- a redirect bypassing host or destination validation;
- malicious MIME or filename data reaching storage or execution;
- an exported Android component accepting unauthorized input;
- Desktop IPC/listener behavior crossing an untrusted boundary;
- a workflow executing untrusted pull-request code with elevated permissions;
- a dependency change introducing known high-severity risk;
- an update or artifact accepted without integrity verification.

Generic hardening advice is not automatically a vulnerability.

## Validation

Validate safely in an isolated or controlled environment. Preserve:

- the exact reproduction command or test;
- attacker-controlled input or synthetic fixture;
- raw output and exit status;
- proof that the sensitive outcome is reachable;
- assumptions and environmental requirements.

Do not use RTK for exploit evidence, security scan results, secret scans, hashes,
signatures, signing evidence, or exact reproduction output.

Never perform destructive validation, persistence, credential theft, external
targeting, production-impacting tests, or uncontrolled proof-of-concept
activity. Use synthetic credentials and minimal local fixtures.

## Remediation

For a validated finding:

1. fix the root cause with the smallest compatible patch;
2. preserve unrelated behavior;
3. add a focused regression test or exact verification;
4. inspect the complete diff;
5. run applicable domain, security, and CI checks;
6. keep publication and repository actions owner-controlled.

Security engines and agents propose patches; they do not gain automatic authority
to commit, push, merge, release, rotate credentials, deploy, or change
repository settings.

## Human review

Every finding must state severity, confidence, exact location, attacker input,
trust boundary, exploit path, validation evidence, consequence, minimal fix,
regression coverage, and residual risk.

Review the full patch rather than only generated summaries. CodeRabbit,
SonarQube, CI, dependency review, and a security engine are separate signals;
none proves security alone.

## Revalidation

After remediation, rerun the original safe reproduction or equivalent
regression test. Report:

- whether the attack path is closed;
- which checks passed;
- which checks were blocked and why;
- whether assumptions changed;
- remaining or transferred risk.

A patch is not complete merely because it compiles or a scanner stops reporting
the finding.

## Dependency Review

`.github/workflows/dependency-review.yml` uses
`actions/dependency-review-action` and fails on high or critical newly
introduced known vulnerabilities when GitHub Dependency Graph is available.

The workflow first checks Dependency Graph availability. When GitHub has not
enabled it for the repository, the job reports **blocked, not passed** and does
not falsely claim a successful vulnerability review. Once Dependency Graph is
enabled, actual high-severity findings remain blocking.

Dependency Review is a supply-chain gate, not a replacement for source review,
license review, runtime validation, privacy analysis, or manual approval.

## Relationship with RTK and Superpowers

- Superpowers supports planning, debugging, tests, and delivery discipline.
- RTK reduces repetitive non-sensitive command output.
- `levyra-security-review` supplies project-specific threat boundaries and
  evidence standards across all supported runtimes.
- Codex Security may add specialized scanning and remediation assistance.
- GitHub Dependency Review checks known dependency risk when supported.
- CI, CodeRabbit, SonarQube, manual review, and owner approval remain separate
  gates.

## References

This approach is inspired by the security priority and dependency-review setup
in `ChrisTitusTech/titus-ai`, adapted to Levyra's Android, Desktop, extractor,
CI, update, privacy, and release boundaries.
