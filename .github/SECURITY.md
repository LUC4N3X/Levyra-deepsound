# Security Policy

If you find a security issue in Levyra, please keep the details private until there has been time to investigate and fix it.

This page is for security problems in Levyra itself. Normal bugs, playback issues, UI problems, provider outages, and feature requests should go through the regular issue tracker.

## Reporting a security issue

Please use GitHub's private **Report a vulnerability** option from the repository Security tab when it is available.

If private reporting is not available, open a public issue only to ask for a private contact channel. Do not include exploit details, proof-of-concept code, credentials, tokens, cookies, private URLs, personal data, or sensitive logs in the public issue.

Issue tracker: https://github.com/LUC4N3X/Levyra-deepsound/issues/new/choose

When possible, check the latest official release before reporting. If the issue only affects an older release but the same code is still present in the current version, the report is still useful.

## Supported versions

| Version | Security support |
| --- | --- |
| Latest Android release | Supported |
| Latest Windows release | Supported |
| Older official releases | Best effort |
| Modified, repackaged, or unofficial builds | Not supported |

---

<details>
<summary><strong>What to include in a report</strong></summary>

<br>

A short, reproducible report is much more useful than a long severity claim.

Include whatever is relevant:

- Levyra version or commit;
- Android or Windows version;
- affected feature or component;
- what happens and why you believe it is a security issue;
- clear reproduction steps;
- a minimal proof of concept if one is needed;
- realistic impact and attacker requirements;
- whether user interaction, authentication, local access, or special permissions are required;
- logs or screenshots with private information removed;
- any workaround or suggested fix you already know about.

Please separate what you actually reproduced from what you think may also be possible.

</details>

<details>
<summary><strong>Scope</strong></summary>

<br>

Examples of issues that belong here include:

- exposure of credentials, tokens, cookies, or private user data caused by Levyra;
- unsafe storage or handling of sensitive local data;
- authentication or authorization bypasses in Levyra-owned functionality;
- unintended code or command execution;
- unsafe file access, path traversal, or similar file-handling issues;
- security problems involving intents, deep links, URIs, IPC, or app-controlled input;
- injection or unsafe parsing with a real security impact;
- update, packaging, release, networking, session, or credential-handling issues controlled by Levyra;
- a dependency problem that is actually reachable and exploitable through Levyra.

Things that are normally not Levyra security issues include ordinary crashes, UI bugs, playback failures, provider outages, geo-restrictions, rate limits, copyright or content-policy disputes, and vulnerabilities that exist only in a third-party service or operating system.

Scanner output by itself is not enough. If a dependency is flagged, explain how Levyra reaches the vulnerable code and what the real impact is.

If you are not sure whether something belongs here, report it privately and explain why you think it matters.

</details>

<details>
<summary><strong>Third-party services</strong></summary>

<br>

Levyra talks to external APIs, websites, providers, and other services. Those systems are not operated by this project.

This policy does not give permission to test third-party infrastructure, accounts, APIs, streaming services, CDNs, or other systems Levyra connects to.

When testing an integration, use only accounts, devices, content, and systems you own or are explicitly allowed to test.

If the problem belongs entirely to a third party, report it to that provider. If Levyra introduces the unsafe behavior or turns an upstream issue into a security problem for Levyra users, then it is reasonable to report it here as well.

</details>

<details>
<summary><strong>Response and disclosure</strong></summary>

<br>

For a credible report, the project aims to acknowledge it within 14 days, usually sooner when possible. This is a target, not a guaranteed response time.

The usual flow is:

1. reproduce the issue;
2. work out which versions and platforms are affected;
3. prepare and test a fix or mitigation;
4. release the fix when needed;
5. disclose technical details after users have had a reasonable chance to update.

Please allow time for investigation and release before publishing the vulnerability.

A confirmed issue may later be documented in a security advisory, release note, CVE, or another public record when appropriate. Reporters may be credited unless they prefer not to be.

</details>

<details>
<summary><strong>Handling secrets and user data</strong></summary>

<br>

Do not send more sensitive data than the report actually needs.

Redact API keys, cookies, tokens, session identifiers, account details, and personal information where possible. Prefer test accounts over real accounts and remove unrelated data from logs and screenshots.

If a real secret has already been exposed publicly, treat it as compromised and rotate or revoke it where possible.

</details>

<details>
<summary><strong>Security notes</strong></summary>

<br>

### Android credential storage

Credentials handled through `AndroidKeystoreCredentialStore` use Android Keystore-backed AES keys and `AES/GCM/NoPadding` before the encrypted value is stored in app-private preferences.

Implementation:

https://github.com/LUC4N3X/Levyra-deepsound/blob/main/app/src/main/java/com/luc4n3x/levyra/data/security/AndroidKeystoreCredentialStore.kt

### Compatibility cryptography

A few integrations use older algorithms because the external protocol expects them. These are compatibility paths, not Levyra's credential-storage or release-verification mechanisms.

- **Last.fm** uses MD5 when building the service's `api_sig` value.
- **Spotify compatibility** uses HMAC-SHA1 TOTP in the anonymous-token flow.
- **YouTube player fingerprinting** uses MD5 only as a non-security fingerprint for script identity/change detection.

References:

- https://www.last.fm/api/authspec
- https://github.com/LUC4N3X/Levyra-deepsound/blob/main/app/src/main/java/com/luc4n3x/levyra/feature/scrobbling/Scrobbling.kt
- https://github.com/LUC4N3X/Levyra-deepsound/blob/main/app/src/main/java/com/luc4n3x/levyra/data/SpotifyArtistArtworkRepository.kt
- https://github.com/LUC4N3X/Levyra-deepsound/blob/main/app/src/main/java/com/luc4n3x/levyra/data/YoutubeLocalDecoder.kt

If an upstream protocol provides a stronger compatible alternative in the future, Levyra should move to it when that can be done without breaking the integration.

</details>

<details>
<summary><strong>Responsible research and bug bounty</strong></summary>

<br>

Please keep testing limited to systems and accounts you are allowed to use. Avoid destructive testing, unnecessary access to other people's data, persistence, malware, service disruption, or high-volume traffic against third-party services.

Stop if testing exposes private data that is not needed to prove the issue.

Levyra does not currently run a paid bug bounty program. A valid report may be credited publicly, but payment or other compensation is not promised.

</details>

---

Official releases are published at:

https://github.com/LUC4N3X/Levyra-deepsound/releases

For security-sensitive testing, use an official Levyra build rather than an unknown repackaged or modified copy.