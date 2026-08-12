# Context-efficient execution

Apply this baseline immediately to every real Levyra coding task, before broad
repository reading or noisy commands:

- search/symbol/path first;
- read the smallest useful range or focused diff;
- expand only to answer a concrete unresolved question;
- do not reread unchanged evidence already in context;
- load only matching skills, never the whole skill tree;
- keep security, Perfetto, R8, signing, exact failures, and decisive diagnostics
  raw when compression could change the conclusion.

For non-trivial repository exploration, builds, tests, lint, logs, broad
searches, dependencies, Git/GitHub, CI, CodeRabbit, adb, setup, or other
high-volume work, invoke `levyra-context-efficiency` and follow
`.agents/skills/levyra-context-efficiency/SKILL.md` as the canonical procedure.

When RTK is available, prefer its supported compact wrappers for repetitive
success-heavy output. Rerun the exact command raw if compact output is
incomplete, ambiguous, or insufficient to diagnose a failure. Verify exit
status and final success/failure markers; compact output is not validation
authority.

Project filters live in `.rtk/filters.toml`; setup and measurement are in
`docs/ai/RTK.md` and `scripts/setup-ai.ps1` / `scripts/setup-ai.sh`.

Do not install another always-on compression proxy. Do not infer permission to
commit, push, open/merge a PR, tag, publish, or release.