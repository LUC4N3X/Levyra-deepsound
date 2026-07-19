---
name: Levyra Security Review
description: Reviews Levyra changes for secrets, unsafe remote URLs and redirects, SSRF, MIME confusion, excessive permissions, privacy leaks, and workflow exposure.
context: fork
agent: levyra-reviewer
---

# Security review checklist

Inspect the current diff and surrounding code.

- Search for credentials, tokens, cookies, keys, signed URLs, keystores, and sensitive logging.
- For every provider-controlled URL, verify scheme, host allowlist, port, user-info rejection, public DNS/IP destination, redirect-hop validation, timeout, and response-size bound.
- Reject explicit non-media MIME responses even when the path ends in `.mp4` or `.m3u8`.
- Check HTTP clients for automatic redirects that can bypass validation.
- Check shell commands, filenames, SQL, logs, and intents for untrusted input injection.
- Check Android and GitHub permissions for least privilege.
- Check that listening/account data remains local or user-controlled as documented.
- Report only evidence-backed findings with file/line references and a concrete exploit or failure path.
