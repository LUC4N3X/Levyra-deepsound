# Context-efficient command execution

Use `.agents/skills/levyra-context-efficiency/SKILL.md` as the canonical Levyra
procedure for command-output efficiency.

Automatically apply it when work involves builds, tests, lint, logs, broad
searches, dependencies, Git/GitHub, CI, CodeRabbit, adb, setup, or other
high-volume shell output.

When `rtk` is available, prefer its supported compact wrappers. Keep short or
exact-output commands raw. Rerun the exact command raw whenever filtered output
is incomplete, a failure cannot be diagnosed, or security/signing/checksum/
release evidence must remain complete. Verify exit status and final
success/failure markers; compact output is not validation authority.

Project-specific filters live in `.rtk/filters.toml`. Setup and measurement are
documented in `docs/ai/RTK.md` and automated by `scripts/setup-ai.ps1` and
`scripts/setup-ai.sh`.

Follow the owner-authorized automatic RTK bootstrap in root `AGENTS.md` when
the pinned official RTK build is unavailable or is the wrong `rtk` project.
Do not install other executables or plugins without explicit owner
authorization. Do not infer permission to commit, push, open a pull request,
merge, tag, publish, or release.
