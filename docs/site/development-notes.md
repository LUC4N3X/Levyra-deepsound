# How I work on Levyra

Levyra is a personal open-source project, and I am responsible for what gets merged and released.

I use AI-assisted tools for research, debugging, code review and drafting when they save time. I do not treat their output as finished just because it compiles. I read it, adapt it to Levyra, test what matters and drop it when it makes the code worse.

## Before a change ships

The checks depend on the change. A UI tweak and a playback change do not need the same test plan. A change still has to fit the existing codebase and avoid breaking behavior users already rely on.

When relevant, I check playback, queues, downloads, playlists, history, settings, privacy, licensing and platform-specific behavior.

For non-trivial engineering work, the repository defines two quality-gate commands:

```bash
python3 scripts/ai_quality_gate.py --profile fast
```

Before a push or pull request, the repository standard is the full profile:

```bash
python3 scripts/ai_quality_gate.py --profile full
```

If a check cannot run because a required SDK, device, signing setup or other dependency is missing, that is reported as blocked rather than treated as a pass.

## Testing on a real phone

Some Android problems only show up on an actual device. Levyra's PowerShell qualification script builds and installs a debug APK, checks MediaSession state, records memory data and saves diagnostic logs.

```powershell
.\scripts\levyra-device-qualification.ps1
```

The [development guide](development.md) documents the available options, including wireless debugging.

## Public history

Useful places to check:

- [Commits](https://github.com/LUC4N3X/Levyra-deepsound/commits/main/) show how the project changes over time.
- [Pull requests](https://github.com/LUC4N3X/Levyra-deepsound/pulls) show proposed changes and review history.
- [Issues](https://github.com/LUC4N3X/Levyra-deepsound/issues) show bugs, requests and technical discussion.
- [AGENTS.md](https://github.com/LUC4N3X/Levyra-deepsound/blob/main/AGENTS.md) contains the repository engineering rules.
- [The engineering workflow](https://github.com/LUC4N3X/Levyra-deepsound/blob/main/docs/ai/WORKFLOW.md) documents the implementation, review, CI, testing, merge and release stages.

If you are reviewing Levyra, the code and the app are better sources than this page. If something looks wrong, open an issue and point me to it.
