# Security Policy

Levyra treats security reports seriously. This document explains how to report a suspected vulnerability responsibly and what to expect after a report is submitted.

## Supported Versions

Security fixes are generally developed for the latest supported Levyra release.

| Product | Supported |
| --- | --- |
| Latest Android release | Yes |
| Latest Windows Desktop release | Yes |
| Older releases | Best effort only |

Before reporting an issue, please verify whether it still affects the latest available release.

## Reporting a Vulnerability

**Do not disclose suspected security vulnerabilities in public GitHub Issues, Discussions, pull requests, or other public channels.**

The preferred reporting method is GitHub Private Vulnerability Reporting:

https://github.com/LUC4N3X/Levyra-deepsound/security/advisories/new

If the private reporting option is temporarily unavailable, open a public issue only to request a private contact channel. Do **not** include vulnerability details, proof-of-concept code, exploit steps, credentials, tokens, private URLs, or other sensitive material in that public issue.

## What to Include

A useful security report should include, where applicable:

- the affected Levyra version and platform;
- the affected component or feature;
- a clear description of the vulnerability;
- reproducible steps or a minimal proof of concept;
- the security impact and realistic attack scenario;
- whether user interaction or special configuration is required;
- relevant logs, screenshots, or traces with sensitive information removed;
- any suggested mitigation or remediation, if known.

Please keep the report focused and avoid accessing data, systems, or accounts that you do not own or have explicit permission to test.

## Scope

This policy covers security issues introduced by Levyra's own source code, build configuration, packaged release artifacts, update mechanisms, local data handling, networking logic, and application behavior.

Issues that exist exclusively in third-party services, operating systems, upstream libraries, external APIs, content providers, or infrastructure outside Levyra's control should normally be reported to the responsible upstream project or provider.

If Levyra meaningfully introduces, exposes, or amplifies an upstream issue, it may still be appropriate to report it here.

## Response Process

The project aims to acknowledge valid vulnerability reports within **14 days**, and usually sooner when possible.

After triage, the maintainer may:

1. confirm whether the report is in scope;
2. request additional reproduction details;
3. assess severity and affected versions;
4. prepare and validate a fix privately;
5. coordinate release timing and public disclosure.

Timelines depend on severity, reproducibility, upstream dependencies, and the complexity of a safe fix.

## Coordinated Disclosure

Please allow reasonable time for investigation and remediation before public disclosure.

When practical, confirmed vulnerabilities will be fixed and released before technical details are made public. Security-relevant release notes may include an assigned CVE or other public vulnerability identifier when one exists.

The project may credit reporters who contributed to a confirmed fix, unless they prefer to remain anonymous.

## Security Updates

Users should install the latest official Levyra release from a supported distribution channel to receive current security and reliability fixes.

Official releases are published at:

https://github.com/LUC4N3X/Levyra-deepsound/releases

## Responsible Research

Good-faith security research is welcome when it:

- avoids privacy violations and unnecessary data access;
- avoids destructive testing;
- does not intentionally degrade third-party services;
- uses only accounts, devices, and systems the researcher is authorized to test;
- reports findings privately and provides a reasonable remediation window.

This policy does not authorize testing against third-party services or infrastructure.

## No Bug Bounty

Levyra does not currently operate a paid bug bounty program. Acknowledgement or public credit may be offered for responsible disclosure, but monetary compensation is not promised.
