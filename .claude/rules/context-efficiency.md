# Context-efficient execution

Apply this before broad repository work:

- search path/symbol/call site first;
- read the smallest useful range or focused diff;
- expand only for a concrete unanswered question;
- do not reread unchanged evidence already in context;
- load only the skills routed for the current task.

Prefer project jCodeMunch for non-trivial symbol discovery, then available
LSP/AST tooling, then bounded Claude native Read/Grep/Glob/Bash when broader
evidence is needed.

Invoke `levyra-context-efficiency` for noisy builds, tests, lint, logs, broad
searches, dependency/Git/GitHub/CI output, or other high-volume work. Project
RTK filters remain in `.rtk/filters.toml`. Use RTK only when filtered output is
sufficient. Rerun the exact command raw for exact failures, stack traces,
security/signing evidence, Perfetto/R8 evidence, or ambiguous results.

Token savings never override correctness, validation, or publication controls.
