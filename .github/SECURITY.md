# Security Policy

Security issues deserve a different handling path from ordinary bugs.

Levyra is an open-source media application that interacts with local files, network services, third-party APIs, user-provided credentials, and external content providers. This policy explains which reports belong in the security process, how to report them safely, what information is useful during triage, and what reporters can expect after disclosure.

Security is an ongoing process, not a claim that Levyra is vulnerability-free. The goal of this policy is to make responsible reporting clear, predictable, and safe for both users and researchers.

## Quick Reporting Guide

If you believe you have found a security vulnerability:

1. **Do not publish the vulnerability publicly.**
2. Check whether the issue still affects the latest official Levyra release.
3. Use GitHub's **Report a vulnerability** option if a private reporting form is available in the repository's Security tab.
4. If no private reporting option is available, open a public issue **only to request a private contact channel**. Do not include technical vulnerability details in that issue.
5. Provide enough information privately for the issue to be reproduced and assessed safely.

Never place exploit details, proof-of-concept code, credentials, tokens, cookies, private URLs, signing material, personal information, or sensitive logs in a public GitHub issue, discussion, pull request, comment, screenshot, or attachment.

Issue tracker:

https://github.com/LUC4N3X/Levyra-deepsound/issues/new/choose

## Supported Versions

Security fixes are primarily developed for the latest supported Levyra releases.

| Product | Security support |
| --- | --- |
| Latest Android release | Supported |
| Latest Windows Desktop release | Supported |
| Older official releases | Best effort only |
| Unofficial forks, repackaged builds, or modified APKs/binaries | Not supported by this project |

Before reporting a vulnerability, please confirm that it can still be reproduced on the latest official release whenever reasonably possible.

A report affecting only an old version may still be useful when the same vulnerable code path exists in the current release.

## Reporting a Vulnerability

Please report suspected vulnerabilities privately whenever possible.

A good report does not need to be long, but it should make the security impact and reproduction path clear. Include the following when applicable:

- affected Levyra version or commit;
- platform and OS version;
- affected component, screen, service, or feature;
- concise vulnerability description;
- exact reproduction steps;
- minimal proof of concept, if one is necessary to demonstrate the issue;
- realistic security impact;
- attacker requirements or preconditions;
- whether user interaction is required;
- whether authentication, local access, special permissions, or a modified environment is required;
- logs, traces, screenshots, or recordings with sensitive information removed;
- whether the issue appears to affect Android, Windows, or both;
- any known mitigation or suggested fix.

Please distinguish clearly between what you have verified and what you believe may be possible. A precise, reproducible report is more useful than an exaggerated severity claim.

## What Is In Scope

A vulnerability is generally in scope when it is caused by Levyra itself or when Levyra turns an upstream weakness into a meaningful security problem for its users.

Examples include, but are not limited to:

- exposure of credentials, tokens, cookies, secrets, or private user data caused by Levyra;
- unsafe handling of locally stored sensitive information;
- authentication or authorization bypasses in Levyra-owned functionality;
- arbitrary code execution or unintended command execution through Levyra;
- unsafe file handling, path traversal, or unintended file access;
- insecure IPC, deep-link, intent, URI, or inter-process handling with a meaningful security impact;
- injection vulnerabilities in application-controlled input paths;
- unsafe deserialization or parsing when it creates a real security boundary violation;
- server-side request behavior introduced by Levyra that can reach unintended resources;
- vulnerabilities in update, release, packaging, or artifact-handling logic controlled by the project;
- privacy-impacting data exposure caused by application behavior;
- security-relevant misuse of a dependency or external API by Levyra;
- security issues in Levyra's own networking, session, credential, or local-storage logic.

Reports involving an upstream dependency are welcome when the vulnerability is exploitable through Levyra, when Levyra uses the dependency unsafely, or when project-specific mitigation is required.

## What Is Normally Out of Scope

The following are normally not treated as Levyra security vulnerabilities unless they demonstrate a separate vulnerability in Levyra itself:

- ordinary crashes, UI bugs, playback failures, or performance problems without a security impact;
- content availability, catalog differences, geo-restrictions, rate limits, provider blocking, or API behavior controlled by third parties;
- copyright, licensing, moderation, or content-policy disputes;
- vulnerabilities that exist exclusively in an operating system, device firmware, external API, upstream library, website, CDN, DNS provider, or other third-party infrastructure;
- issues that require an already fully compromised device or unrestricted attacker-controlled operating system, unless Levyra creates an additional security boundary violation;
- reports affecting only unofficial forks, modified builds, repackaged APKs, patched binaries, or third-party distributions that alter Levyra;
- theoretical weaknesses without a credible attack path or meaningful impact;
- missing hardening measures that do not create an exploitable vulnerability by themselves;
- denial-of-service testing against third-party services;
- social engineering, phishing, credential stuffing, or attacks against accounts and systems not operated by Levyra;
- automated scanner output without validation or evidence of exploitability.

If you are unsure whether something is in scope, report it privately with a short explanation of the suspected impact.

## Third-Party Services and Boundaries

Levyra integrates with external services and providers. Those services remain outside the project's control.

This policy does **not** authorize security testing against third-party infrastructure, APIs, user accounts, websites, streaming services, CDNs, or other systems that Levyra communicates with.

If a vulnerability belongs entirely to a third party, it should normally be reported to that provider through its own security process. If Levyra introduces, exposes, amplifies, or fails to safely contain the issue, a Levyra report may still be appropriate.

When testing an integration, use only accounts, devices, content, endpoints, and systems that you own or are explicitly authorized to test.

## Handling Secrets and Sensitive Data

Do not include real secrets in reports unless absolutely necessary.

Whenever possible:

- replace API keys, cookies, tokens, session identifiers, and credentials with redacted examples;
- remove personal data from logs and screenshots;
- use test accounts rather than real user accounts;
- minimize the amount of data collected during reproduction;
- do not retain user data after testing;
- do not upload sensitive artifacts to public file-sharing services.

If a secret has already been exposed publicly, assume it may be compromised and rotate or revoke it where possible.

## Triage and Severity

Reports are evaluated based on demonstrated impact, exploitability, affected users, required privileges, user interaction, attack complexity, and the security boundary that is crossed.

CVSS or another scoring system may be used as a reference, but a submitted score does not automatically determine project priority. Levyra may classify an issue differently after reproduction and impact analysis.

The project aims to:

- acknowledge credible security reports within **14 days**, usually sooner;
- reproduce and classify the issue as soon as practical;
- prioritize fixes according to real-world risk;
- keep the reporter informed when meaningful progress or additional information is needed;
- coordinate disclosure after an appropriate fix or mitigation is available.

These are targets rather than contractual service-level guarantees. Response time may vary based on severity, reproducibility, maintainer availability, upstream dependencies, and the complexity of producing a safe fix.

## Response Process

A typical security report moves through the following stages:

1. **Acknowledgement** — the report is received and checked for enough information to investigate.
2. **Validation** — the issue is reproduced and confirmed or rejected as a security vulnerability.
3. **Impact assessment** — affected platforms, versions, attack requirements, and likely severity are determined.
4. **Remediation** — a fix or mitigation is prepared, reviewed, and tested.
5. **Release** — the fix is shipped through an official Levyra release when necessary.
6. **Disclosure** — technical details may be published after users have had a reasonable opportunity to update.

Some reports may be closed as non-security bugs, upstream issues, duplicates, already-fixed problems, or non-reproducible reports. When possible, the reason will be explained to the reporter.

## Coordinated Disclosure

Please allow reasonable time for investigation, remediation, testing, and release before publishing vulnerability details.

For a confirmed issue, the preferred disclosure sequence is:

1. private report;
2. validation and impact assessment;
3. development and testing of a fix or mitigation;
4. release to users;
5. coordinated public disclosure when appropriate.

Public disclosure may include a security advisory, release note, CVE, or another vulnerability identifier when one is appropriate and available.

Reporters who materially help identify or resolve a confirmed vulnerability may be credited, unless they prefer to remain anonymous.

## Responsible Security Research

Good-faith security research is welcome when it is designed to demonstrate a vulnerability without creating unnecessary risk.

Please:

- test only systems and accounts you are authorized to use;
- avoid accessing data that is not needed to prove the issue;
- stop testing if you encounter private data belonging to another person;
- avoid destructive actions, persistence, malware, or unnecessary privilege escalation;
- avoid degrading availability for users or third-party services;
- avoid high-volume automated traffic that could disrupt external providers;
- do not use a vulnerability to access, modify, delete, or publish information beyond what is necessary to demonstrate impact;
- report findings privately and allow a reasonable remediation window.

This policy is intended to support responsible research into Levyra itself. It does not grant permission to test systems owned by other people or organizations.

## Security Architecture Notes

The following notes exist to make important security boundaries explicit. They are not a complete security design specification and should not be interpreted as a guarantee that every piece of application data uses the same mechanism.

### Android credential storage

Where Levyra stores supported external credentials through `AndroidKeystoreCredentialStore`, the application uses Android Keystore-backed AES keys and `AES/GCM/NoPadding` for authenticated encryption before persisting the encrypted value in application-private preferences.

Relevant implementation:

https://github.com/LUC4N3X/Levyra-deepsound/blob/main/app/src/main/java/com/luc4n3x/levyra/data/security/AndroidKeystoreCredentialStore.kt

### Compatibility cryptography

A small number of external compatibility paths use older cryptographic primitives because the corresponding upstream protocol or interoperability flow expects them. These uses are intentionally isolated from Levyra's own credential-encryption and security boundaries.

- **Last.fm API signing** — Last.fm's `api_sig` construction uses MD5 as part of its authentication protocol. Levyra uses MD5 only for that protocol-compatible signature construction over HTTPS. It is not used for password hashing, local authentication, local credential encryption, or release verification.
- **Spotify compatibility TOTP** — the anonymous-token compatibility flow uses HMAC-SHA1 TOTP. This is limited to the external Spotify compatibility path and is not used for Levyra credential encryption, password storage, or release verification.
- **YouTube player fingerprinting** — an MD5 digest is used as a compact, non-security fingerprint for player-script identity/change detection. It is not treated as a cryptographic authenticity or integrity guarantee.

Relevant implementation references:

- https://www.last.fm/api/authspec
- https://github.com/LUC4N3X/Levyra-deepsound/blob/main/app/src/main/java/com/luc4n3x/levyra/feature/scrobbling/Scrobbling.kt
- https://github.com/LUC4N3X/Levyra-deepsound/blob/main/app/src/main/java/com/luc4n3x/levyra/data/SpotifyArtistArtworkRepository.kt
- https://github.com/LUC4N3X/Levyra-deepsound/blob/main/app/src/main/java/com/luc4n3x/levyra/data/YoutubeLocalDecoder.kt

If an upstream protocol gains a stronger compatible alternative, Levyra should prefer migrating to it when that can be done without breaking interoperability.

## Dependencies and Upstream Vulnerabilities

Levyra depends on platform libraries, open-source dependencies, and external services. A vulnerability in one of those components does not automatically mean Levyra is exploitable.

Reports are most actionable when they explain:

- the affected dependency or service;
- the vulnerable version or behavior;
- how Levyra reaches the vulnerable code path;
- the realistic impact on a Levyra user;
- whether a patched upstream version or mitigation already exists.

Dependency scanner alerts without a demonstrated Levyra impact may be handled as maintenance issues rather than security incidents.

## Security Updates and Official Releases

Users should keep Levyra updated to the latest official release to receive current security, compatibility, and reliability fixes.

Official GitHub releases are published at:

https://github.com/LUC4N3X/Levyra-deepsound/releases

For security-sensitive verification, prefer artifacts published or linked by the Levyra project rather than unknown third-party re-hosts or modified packages.

## No Bug Bounty

Levyra does not currently operate a paid bug bounty program.

Submitting a report does not create an entitlement to payment, compensation, merchandise, or other rewards. Public acknowledgement or credit may be offered for responsible disclosure of a confirmed vulnerability.

## Public Discussions After a Fix

Once a vulnerability has been fixed and disclosure is considered safe, discussion is welcome when it helps users understand the impact, mitigation, or engineering lessons without exposing unnecessary private information.

Please avoid publishing secrets, user data, or unrelated third-party information even after the vulnerability itself is public.

## Policy Changes

This policy may evolve as Levyra's architecture, platforms, integrations, and release process change.

The version committed to the repository is the authoritative project policy. If a security-sensitive process changes, this document should be updated alongside that change.
