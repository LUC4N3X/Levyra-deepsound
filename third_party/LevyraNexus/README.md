# Levyra Nexus

Levyra Nexus is the dependency-light shared core for Levyra Android and Levyra Desktop.

It contains:

- typed results and platform-neutral gateway contracts;
- adaptive route health, address-family ordering and circuit breaking;
- playback transition classification that separates natural completion, manual skips, replay and error recovery;
- update artifact selection, staged rollout, SHA-256 validation and optional public-key signature verification.

The module never disables TLS verification, never installs a permissive trust manager, never forces HTTP/1.1 globally and does not collect telemetry. Platform-specific networking, package inspection and playback adapters remain in the host applications.

Levyra Nexus is distributed under the repository's GPL-3.0 license.
