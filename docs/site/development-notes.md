# How I work on Levyra

Levyra is a personal open-source project, and I am responsible for what gets merged and released.

I use normal developer tools and, when useful, AI-assisted tools for research, debugging, code review or drafting. I do not consider their output finished just because it compiles. I read it, adapt it to Levyra, test what matters, and drop it if it makes the project worse.

## Before a change ships

The exact checks depend on the change, but the basic rule is simple: it has to belong in the existing codebase and it must not quietly break something users already rely on.

That means checking things like playback, queues, downloads, playlists, history, settings, privacy, licensing and platform-specific behavior when they are relevant to the change.

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

Some Android problems only show up on an actual device. Levyra includes a PowerShell qualification script that can build and install a debug APK, inspect MediaSession state, collect memory information and save diagnostic logs.

```powershell
.\scripts\levyra-device-qualification.ps1
```

The [development guide](development.md) documents the available options, including wireless debugging.

## You can inspect the work yourself

Nothing here needs to be taken on trust:

- [Commits](https://github.com/LUC4N3X/Levyra-deepsound/commits/main/) show how the project changes over time.
- [Pull requests](https://github.com/LUC4N3X/Levyra-deepsound/pulls) show proposed changes and review history.
- [Issues](https://github.com/LUC4N3X/Levyra-deepsound/issues) show bugs, requests and technical discussion.
- [AGENTS.md](https://github.com/LUC4N3X/Levyra-deepsound/blob/main/AGENTS.md) contains the repository engineering rules.
- [The engineering workflow](https://github.com/LUC4N3X/Levyra-deepsound/blob/main/docs/ai/WORKFLOW.md) documents the implementation, review, CI, testing, merge and release stages.

If you are reviewing Levyra, I would rather you look at the code and try the app than trust a claim on this page. Build it, break it, read the history, and open an issue if something is wrong.
