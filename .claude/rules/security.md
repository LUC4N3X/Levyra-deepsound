---
paths:
  - "app/src/main/java/com/luc4n3x/levyra/data/network/**/*.kt"
  - "app/src/main/java/com/luc4n3x/levyra/data/security/**/*.kt"
  - "app/src/main/java/com/luc4n3x/levyra/data/Youtube*.kt"
  - "app/src/main/java/com/luc4n3x/levyra/feature/motion/**/*.kt"
  - "app/src/main/AndroidManifest.xml"
  - ".github/workflows/**/*.yml"
  - "app/build.gradle.kts"
---

# Security and Privacy

- Do not commit secrets, credentials, cookies, tokens, private headers, signing material, or production API keys.
- Validate provider-controlled URLs before connecting: scheme, exact/suffix allowlist, port, user-info, and destination.
- Disable automatic redirects when the target is untrusted and validate every redirect hop before following it.
- Reject loopback, unspecified, link-local, multicast, private/LAN, CGNAT, and other non-public resolved addresses. Consider DNS rebinding and validate the actual connection path.
- Do not accept a media URL from its extension alone. Reject explicit non-media MIME types and cap probe size/time.
- Never log auth headers, signed URLs, tokens, cookies, full account payloads, or private user data.
- Use least-privilege Android permissions and GitHub workflow permissions.
- Keep listening statistics local unless a documented, user-controlled feature explicitly sends data.
- Sanitize external text and identifiers before using them in filenames, SQL fragments, logs, or shell commands.
- Review third-party code and update notices/licenses before inclusion.
