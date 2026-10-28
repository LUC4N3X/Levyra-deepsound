# Experimental TIDAL authentication probe

This first experiment checks whether TIDAL accepts an owner-supplied refresh
credential. It does not add a TIDAL playback provider, verify lossless access,
or build an APK. Existing lossless providers and ordinary Quick APK builds are
unchanged.

## Setup and run

1. Revoke the previously shared credential and generate a replacement.
2. Under repository **Settings → Secrets and variables → Actions**, create the
   repository Secret `LEVYRA_TIDAL_REFRESH_TOKEN`. Paste the original credential
   without spaces or line breaks. Do not paste it into a PR, issue, or workflow input.
3. If required by the issuing client, set the repository variable
   `LEVYRA_TIDAL_CLIENT_ID` to that client's ID. Do not guess another client's ID.
4. As the repository owner, open **Actions → Quick APK → Run workflow**, select
   this PR's branch, and enable **Test the TIDAL Secret only; do not build an APK**.
   The other build options are ignored in probe mode.

The existing Quick APK workflow can also be dispatched against this PR branch
with GitHub CLI:

```bash
gh workflow run quick-apk.yml --repo LUC4N3X/Levyra-deepsound \
  --ref codex/tidal-secret-probe -f tidal_auth_probe=true
```

## Results and limitations

- `PASS`: the refresh exchange returned an access credential. This proves
  authentication only, not a subscription entitlement, FLAC availability, or playback.
- `FAIL`: an HTTP error or invalid response prevented authentication. Logs show
  only a status and fixed diagnostic text, never the provider response or credentials.
- `BLOCKED`: setup, connectivity, or credential rotation requires intervention.
  GitHub marks the probe job unsuccessful; the summary states why.

Refresh exchanges can rotate or invalidate credentials. If TIDAL returns a
replacement, the probe discards it and reports `BLOCKED`; it does not update
repository Secrets. Generate and save a new credential before testing again.

The probe runs only on manual dispatch by the repository owner. It performs no
checkout, follows no redirects, and has no GitHub token permissions. Credentials
are used only in runner memory and are never embedded into an APK, written to a
file, uploaded as artifacts, or included in summaries. Nothing runs on PR events.

After authentication is confirmed, full lossless playback still needs a separate
provider integration and authorized playback testing. The authentication reference
is [TIDAL Authorization](https://developer.tidal.com/documentation/api-sdk/api-sdk-authorization).
