# Security Policy

If you discover a security vulnerability in Levyra, please report it privately so we have time to investigate and issue a patch before details become public.

This policy is for security flaws in Levyra itself. Regular bug reports, playback issues, UI problems, and feature requests should be submitted through our public issue tracker.

## Reporting a security issue

Please use GitHub's private vulnerability reporting feature from the repository's Security tab whenever possible.

If private vulnerability reporting is unavailable, open a public issue only to request a secure contact channel. Do not include exploit details, proof-of-concept code, tokens, credentials, private URLs, or logs in public threads.

Issue tracker: https://github.com/LUC4N3X/Levyra-deepsound/issues/new/choose

When possible, test against the latest official release before reporting. Reports for older versions are still helpful if the same code is present in the current release.

## Supported versions

| Version | Security support |
| --- | --- |
| Latest Android release | Supported |
| Latest Windows release | Supported |
| Older official releases | Best effort |
| Modified or third-party builds | Not supported |

---

<details>
<summary><strong>What to include in a report</strong></summary>

<br>

A concise, reproducible report is much more effective than a high-severity claim.

Please include:

- Levyra version code or commit hash
- Android or Windows operating system version
- Affected feature or component
- Description of the vulnerability and its potential impact
- Step-by-step reproduction instructions
- Minimal proof of concept if required
- Prerequisites (such as physical device access, specific permissions, or user interaction)
- Sanitized logs or screenshots with sensitive data redacted
- Any known workarounds or suggested code fixes

Please distinguish clearly between verified behaviors and theoretical attack vectors.

</details>

<details>
<summary><strong>Scope</strong></summary>

<br>

Examples of issues in scope:

- Exposure of credentials, auth tokens, cookies, or private user data
- Insecure storage or handling of sensitive local files
- Authentication or authorization bypasses in Levyra-owned code
- Arbitrary code or shell command execution
- Directory traversal or insecure file handling
- Vulnerabilities involving Android intents, deep links, URIs, or IPC endpoints
- Parsing or injection vulnerabilities with demonstrable security impact
- Issues in packaging, updates, or network handling controlled by Levyra
- Reachable, exploitable vulnerabilities in third-party dependencies

Issues typically out of scope include standard application crashes, UI layout bugs, playback timeouts, third-party provider outages, geoblocks, rate limits, and vulnerabilities originating solely in third-party services or the underlying OS.

Automated vulnerability scanner reports are not sufficient on their own. If flagging a dependency, explain how Levyra uses the affected code path and what the real-world impact is.

If you are uncertain whether an issue is in scope, report it privately with an explanation of your concern.

</details>

<details>
<summary><strong>Third-party services</strong></summary>

<br>

Levyra interacts with external APIs, websites, and media providers. Those remote systems are not operated by this project.

This policy does not grant permission to perform penetration testing or vulnerability research against third-party servers, CDNs, streaming endpoints, or accounts.

Only test systems, accounts, and hardware that you own or have explicit permission to audit.

If a vulnerability exists entirely within a third-party service, report it directly to that provider. If Levyra causes the unsafe behavior or turns an upstream issue into a client-side vulnerability, please report it here.

</details>

<details>
<summary><strong>Response and disclosure</strong></summary>

<br>

We aim to acknowledge credible reports within 14 days, and often sooner.

Our remediation process generally involves:

1. Reproducing the reported behavior
2. Identifying affected platforms and versions
3. Developing and testing a fix or mitigation
4. Releasing an updated build
5. Disclosing technical details after users have had time to update

Please allow adequate time for investigation and patching before publishing vulnerability details. Confirmed vulnerabilities may be documented in release notes, security advisories, or CVEs with credit given to the reporter if desired.

</details>

<details>
<summary><strong>Handling sensitive information</strong></summary>

<br>

Do not include more sensitive information than necessary to prove the vulnerability.

Redact API tokens, cookies, passwords, account identifiers, and personal data. Use test accounts rather than production credentials.

If a credential or secret has been exposed, consider it compromised and rotate or invalidate it immediately.

</details>

<details>
<summary><strong>Security notes</strong></summary>

<br>

### Android credential storage

Credentials managed via `AndroidKeystoreCredentialStore` use Android Keystore-backed AES keys and `AES/GCM/NoPadding` encryption before storing ciphertext in app-private preferences.

Reference: `app/src/main/java/com/luc4n3x/levyra/data/security/AndroidKeystoreCredentialStore.kt`

### Compatibility cryptography

A small number of third-party integrations rely on legacy hashing algorithms as required by external protocols:

- **Last.fm**: Uses MD5 to generate the service `api_sig` parameter.
- **Spotify compatibility**: Uses HMAC-SHA1 TOTP within the anonymous token flow.
- **YouTube player cache**: Uses MD5 strictly as a non-security cache key for script change detection.

References:
- https://www.last.fm/api/authspec
- `app/src/main/java/com/luc4n3x/levyra/feature/scrobbling/Scrobbling.kt`
- `app/src/main/java/com/luc4n3x/levyra/data/SpotifyArtistArtworkRepository.kt`
- `app/src/main/java/com/luc4n3x/levyra/data/YoutubeLocalDecoder.kt`

</details>

<details>
<summary><strong>Responsible research</strong></summary>

<br>

Please keep testing restricted to your own devices and accounts. Avoid disruptive testing, denial of service attempts, or accessing data belonging to other users.

Levyra does not operate a paid bug bounty program. Valid reports can be credited publicly upon request.

</details>

---

Official release packages are published at: https://github.com/LUC4N3X/Levyra-deepsound/releases

Always use official release builds when performing security audits.