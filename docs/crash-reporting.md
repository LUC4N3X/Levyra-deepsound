# Android crash reports and Player Focus

Player Focus is available in the player's More menu. It uses the existing
player, artwork loader, seek control and transport commands. The same shared
entry point serves every skin. Exit with the visible control or Back; the
preference is stored with interface settings. Entering Focus never changes the
audio/video mode, queue or playback intent.

## Capturing a crash

`CrashReporting` installs an uncaught Java/Kotlin exception handler at the start
of `LevyraApplication.onCreate`. The handler saves a redacted report in private
`files/crash-reports/` storage, delegates to the prior handler and terminates the
process if that handler returns. Concurrent fatal exceptions produce one capture
per process. Recursive failures never launch another activity from the dying
process.

The launcher and report viewer run in `:diagnostics`, where the application's
media/network bootstrap is skipped. The next launcher entry atomically claims
the pending report before opening it. Closing the report continues to the main
app; subsequent launches do not offer the same report again. The viewer's own
main-thread failures are not captured recursively.

Reports remain available in Settings → Preferences → Crash reports and can be
deleted there. At most five reports are retained, with a one-MiB file limit.
Ordinary stack traces include their complete cause and suppressed-exception
chains. A pathological trace exceeding 262144 characters is explicitly marked
as truncated. Storage failure, memory exhaustion, or a crash before handler
installation can prevent capture. The handler cannot guarantee a report when
the operating system cannot complete the write.

Native signals, ANRs, force-stop, low-memory kills, power loss and other system
terminations are not claimed as captured Java/Kotlin exceptions. This feature
does not register invasive permissions or read system logcat.

## Diagnostic context and privacy

The report includes the actual app/build, Android/API level, manufacturer/model,
UTC time, failing thread and exception trace. The main UI publishes its last
known screen and skin. `PlaybackDiagnosticHistory` reuses `PlaybackDiagnostics`
formatting for player state, formats and the latest eight playback error codes.
An existing network/resolver capture is included only when one has actually
been made, with its capture time; otherwise it is identified as unavailable.
Track titles, artists, media IDs and listening history are excluded from this
context.

Presentation metadata stores only language, font, animation preference and
palette colors. The diagnostic process never opens the main process's
single-process DataStore. Settings passes its current presentation explicitly;
recovery uses the presentation attached to the crash report.

Redaction removes credential/header patterns, URLs, email addresses, JWTs,
private IP addresses and common personal-file paths. Free-form exception text
can still contain context that automated patterns cannot identify. The report
is visible before sharing and users should review it. Nothing is uploaded,
sent, or copied to the clipboard automatically. Crash files are excluded from
Android cloud backup and device transfer.

Sharing exports a sanitized text file under `cache/share/diagnostics/` using
the existing non-exported FileProvider. Only the selected URI receives temporary
read permission. The private report directory is not a provider root.

## Preparing a GitHub issue

Report on GitHub copies the complete English report and opens the existing
`bug_report.yml` Issue Form. The generated URL uses the form's real field IDs
for its description, reproduction prompt, actual/expected behavior, version,
system information and logs. Users still select the required dropdown values,
confirm the preflight checkboxes and describe their own reproduction steps.
No token is requested and no issue is submitted by the app.

GitHub documents [issue URL query parameters](https://docs.github.com/en/issues/tracking-your-work-with-issues/using-issues/creating-an-issue#creating-an-issue-from-a-url-query)
and [form field identifiers](https://docs.github.com/en/communities/using-templates-to-encourage-useful-issues-and-pull-requests/syntax-for-githubs-form-schema#keys).
Because URL length is bounded, reports producing a URL longer than 6000
characters use a short link and an explicit instruction to paste the copied
report into Logs or error details. The clipboard retains the complete report.
The app does not claim it can prefill required checkboxes or every dropdown.

The new interface copy has English and Italian bundles, with an intentional
English fallback for other app languages. Diagnostic issue content is always
English.

## Reproducible debug checks

Use the repository wrapper and `-PlevyraFdroidBuild=true` for local debug builds.
`CrashReportingTest` covers persistence, one-time claims, retention, redaction,
cause chains, concurrent failures, delegation and GitHub preparation.
`PlayerFocusSettingsTest` checks the shared setting across every skin and its
presentation metadata.

Device checks are opt-in and target `com.luc4n3x.levyra.debug`:

- `CrashReportingDeviceTest#simulateFatalExceptionInDebugProcess` with
  `simulateFatalCrash=true` deliberately kills the debug process. A runner
  "Process crashed" result is expected; inspect persistence separately.
- `CrashReportingDeviceTest#recoveredReportIsPrivateSanitizedAndClaimedOnlyOnce`
  with `verifyFatalCrash=true` validates the report before the launcher consumes
  its pending marker.
- Simulate another crash and launch `diagnostics.LevyraLauncherActivity` to
  exercise the actual recovery screen, sharing, copying and GitHub navigation.
- `PlayerFocusDeviceTest` with `focusPlayback=true` exercises entry, exit,
  transport, seek and recreation against local audio and restores its saved
  interface/queue state afterwards.

Use an explicit device serial. Never run the fatal simulation against a
production installation or treat that intentional crash as a passing test by
itself. Physical-device, OEM, external browser authentication and native-crash
behavior require separate evidence.
