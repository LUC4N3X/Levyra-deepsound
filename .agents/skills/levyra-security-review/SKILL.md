---
name: levyra-security-review
description: Perform an evidence-based Levyra security review covering secrets, provider URLs, redirects, SSRF, MIME confusion, permissions, privacy, logging, workflows, update verification, and untrusted input.
---

# Levyra security review workflow

## Required context

1. Read the root `AGENTS.md` and the nearest applicable `AGENTS.md`.
2. Read `.claude/skills/levyra-security-review/SKILL.md` and `.claude/rules/security.md`.
3. Inspect the complete diff plus surrounding code, tests, build files, manifests, clients, parsers, shell commands, persistence, and workflows.

## Review areas

- credentials, tokens, cookies, keys, signed URLs, keystores, private configuration, and sensitive logs;
- provider-controlled URL scheme, host, port, user-info, DNS/IP destination, redirects, MIME, timeout, response-size, and file-name handling;
- automatic redirects that bypass explicit validation;
- SQL, shell, intent, deep-link, path, archive, and filename injection;
- Android permissions, exported components, pending intents, file providers, and least privilege;
- GitHub workflow permissions, pull-request trust boundaries, secret exposure, artifact handling, and untrusted checkout execution;
- update manifests, download integrity, SHA-256/signature verification, and downgrade or substitution risks;
- local listening, account, crash, and library data privacy.

## Finding standard

Report only evidence-backed findings. Every finding must include:

- severity and confidence;
- exact file and line or symbol;
- attacker-controlled input or triggering condition;
- concrete exploit or failure path;
- user/system consequence;
- smallest compatible fix;
- regression test or verification needed.

Do not weaken security controls to restore compatibility. Do not report generic best-practice observations without a concrete path to harm.
