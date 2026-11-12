# Security policy

If you find a vulnerability in Levyra, report it privately. That gives me time to investigate and prepare a fix before the details become public.

This policy covers security flaws in Levyra. For ordinary bugs, playback problems, UI issues or feature requests, use the [issue tracker](https://github.com/LUC4N3X/Levyra-deepsound/issues/new/choose).

## Reporting a vulnerability

Use GitHub's private vulnerability reporting feature in this repository's Security tab when it's available.

If it isn't available, open a public issue only to ask for a secure contact channel. Don't include exploit details, proof-of-concept code, tokens, credentials, private URLs or logs in that issue.

Test the latest official release if you can. Reports about older releases are still useful when the affected code is present in the current version.

## Supported versions

| Version | Security support |
| --- | --- |
| Latest Android release | Supported |
| Latest Windows release | Supported |
| Older official releases | Best effort |
| Modified or third-party builds | Not supported |

<details>
<summary>What to include</summary>

Show me how to reproduce the problem and what an attacker could do with it. Include:

- The Levyra version code or commit hash.
- Your Android or Windows version.
- The affected feature or component.
- What the vulnerability does and its potential impact.
- Steps to reproduce it and a minimal proof of concept if needed.
- Any prerequisites, such as physical access, permissions or user interaction.
- Logs or screenshots with sensitive information removed.
- Any workaround or fix you've found.

Separate what you've reproduced from what you think might be possible.

</details>

<details>
<summary>What is in scope</summary>

Report problems such as:

- Exposed credentials, tokens, cookies or private user data.
- Unsafe storage or handling of sensitive files.
- Authentication or authorization bypasses in Levyra's code.
- Arbitrary code or shell command execution.
- Path traversal or unsafe file handling.
- Vulnerabilities in Android intents, deep links, URIs or IPC endpoints.
- Parsing or injection bugs with a demonstrable security impact.
- Problems in packaging, updates or network handling controlled by Levyra.
- Dependency vulnerabilities that are reachable and exploitable through Levyra.

Ordinary crashes, layout bugs, playback timeouts, provider outages, geoblocks and rate limits are usually outside this policy. So are flaws that exist entirely in another service or the operating system.

A scanner finding alone isn't enough to establish a vulnerability. For a dependency report, explain how Levyra reaches the affected code and what can happen as a result.

If you're unsure whether a problem belongs here, send a private report explaining your concern.

</details>

<details>
<summary>Third-party services</summary>

Levyra uses external APIs, websites and media providers. I don't operate those systems.

This policy doesn't authorize testing against their servers, CDNs, streaming endpoints or accounts. Only test systems, accounts and hardware you own or have explicit permission to audit.

Report a flaw entirely within another service to that provider. If Levyra causes the unsafe behavior or makes an upstream problem exploitable in the app, report it here too.

</details>

<details>
<summary>Response and disclosure</summary>

I aim to acknowledge credible reports within 14 days, often sooner.

I'll try to reproduce the problem, identify the affected platforms and versions, then develop and test a fix or mitigation. After an updated build is out and users have had time to install it, the technical details can be disclosed.

Please allow time for investigation and patching before publishing a report. Confirmed vulnerabilities may appear in release notes, security advisories or CVEs. I can credit you if you'd like.

</details>

<details>
<summary>Sensitive information</summary>

Include only the sensitive information needed to demonstrate the problem. Redact API tokens, cookies, passwords, account identifiers and personal data. Use test accounts rather than production credentials.

Treat an exposed credential as compromised. Rotate or invalidate it immediately.

</details>

<details>
<summary>Security notes</summary>

### Android credential storage

`AndroidKeystoreCredentialStore` encrypts credentials with Android Keystore-backed AES keys and `AES/GCM/NoPadding` before saving the ciphertext in app-private preferences.

See [AndroidKeystoreCredentialStore.kt](../app/src/main/java/com/luc4n3x/levyra/data/security/AndroidKeystoreCredentialStore.kt).

### Compatibility cryptography

Some integrations use older algorithms because the external protocol requires them:

- Last.fm uses MD5 for its `api_sig` parameter.
- Spotify compatibility uses HMAC-SHA1 TOTP in the anonymous token flow.
- The YouTube player cache uses MD5 as a cache key to detect script changes, not as a security mechanism.

See the [Last.fm authentication specification](https://www.last.fm/api/authspec) and the relevant code:

- [Scrobbling.kt](../app/src/main/java/com/luc4n3x/levyra/feature/scrobbling/Scrobbling.kt)
- [SpotifyArtistArtworkRepository.kt](../app/src/main/java/com/luc4n3x/levyra/data/SpotifyArtistArtworkRepository.kt)
- [YoutubeLocalDecoder.kt](../app/src/main/java/com/luc4n3x/levyra/data/YoutubeLocalDecoder.kt)

</details>

<details>
<summary>Responsible research</summary>

Keep testing to your own devices and accounts, or those you have explicit permission to test. Don't disrupt services, attempt denial of service or access other people's data.

I don't run a paid bug bounty program. Valid reports can receive public credit on request.

</details>

---

Use [official release builds](https://github.com/LUC4N3X/Levyra-deepsound/releases) for security audits.
