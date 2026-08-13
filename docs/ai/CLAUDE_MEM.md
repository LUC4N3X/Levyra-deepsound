# Claude-Mem integration

Levyra can use `thedotmack/claude-mem` as an optional persistent-memory layer
for coding agents. It is a navigation aid, not a repository source of truth.

Pinned bootstrap package:

```text
claude-mem@13.15.0
```

The pin is intentional. Update it only after reviewing the upstream release,
Windows behavior, hook semantics, and this integration's validation.

## What Levyra uses it for

When memory tools are available, agents should use them automatically when a
task depends on prior work, an earlier investigation, a previous failed
approach, or cross-session continuity.

Use progressive disclosure:

```text
search
→ timeline when chronological context matters
→ get_observations only for the relevant IDs
→ verify against the current repository
```

Do not preload a large memory dump. Query only the project/history needed for
the current question.

Memory can suggest where to look, what was tried, and why a previous decision
was made. Current `AGENTS.md`, current code, current tests, approved planning,
current CI, and direct runtime evidence always win when they disagree with
memory.

## Supported local runtimes

The Levyra bootstrap uses claude-mem's current native integrations for:

- Claude Code: plugin hooks and memory search;
- Codex CLI: native hooks/plugin marketplace plus MCP search;
- Google Antigravity: hooks plus MCP search.

The repository owner gives standing authorization for these runtimes to install
the pinned package automatically only when prior-session context is materially
useful and claude-mem memory tools are not already available. The agent must not
ask the owner to type the setup command first.

Automatic-on-need flow:

```text
memory is useful
→ memory tools already available? use them
→ otherwise run one pinned local setup attempt
→ retry focused memory discovery/search
→ if unavailable or unhealthy, continue without memory
```

Do not repeatedly reinstall during one task. A failed memory setup is reported,
but normal Levyra work continues. Persistent memory must never become a
prerequisite for builds, tests, code review, or repository work.

The dedicated setup scripts are:

Windows:

```powershell
.\scripts\setup-claude-mem.ps1
```

Linux/macOS/WSL:

```bash
./scripts/setup-claude-mem.sh
```

The broader AI setup can still force the same integration manually for repair,
machine provisioning, or diagnostics:

```powershell
.\scripts\setup-ai.ps1 -ClaudeMem
```

```bash
./scripts/setup-ai.sh --claude-mem
```

The setup detects installed supported runtimes and configures only those it
finds. It uses the pinned claude-mem package, Claude subscription authentication
for the compression worker, Haiku for the memory-compression workload, disables
claude-mem anonymous telemetry, starts the local worker, and runs `doctor`.

## Windows fail-open guard

Upstream issue `thedotmack/claude-mem#3481` remains open as of the integration
pin and describes a Claude Code `UserPromptSubmit` failure mode where repeated
worker failures can eventually write to stderr and block prompts.

The Windows bootstrap therefore sets:

```text
CLAUDE_MEM_HOOK_FAIL_LOUD_THRESHOLD=999999999
```

in the existing local claude-mem settings after installation. This preserves
the intended fail-open behavior: if memory is down, coding continues without
memory. Health is checked separately with `doctor`, so hook failure is not used
as the health-notification channel.

If upstream fixes the blocking path, review and remove this compatibility guard
rather than carrying it indefinitely.

## Privacy and storage

Levyra does not enable claude-mem cloud sync. On a fresh upstream installation,
memory data is local under `~/.claude-mem/` and the worker binds locally.

The bootstrap also disables claude-mem anonymous telemetry.

Do not put claude-mem databases, logs, transcripts, tokens, or generated memory
files in the repository. Do not use persistent memory to retain secrets,
keystores, `.env` values, cookies, private URLs, access tokens, signing
material, or `local.properties`.

If sensitive text must pass through a supported claude-mem session, use the
upstream privacy exclusion mechanism where applicable, but still avoid reading
or sending secret material in the first place.

Cloud sync is an owner choice outside this integration. Never enable it
implicitly just to share memory between agents.

Experimental semantic injection is intentionally not enabled by Levyra. Native
runtime hooks and explicit memory search are sufficient; extra always-on
injection should be evaluated separately before activation.

## ChatGPT

The repository can make ChatGPT memory-aware, but it cannot make the ChatGPT
product reach a local `~/.claude-mem` worker by repository configuration alone.

When a claude-mem-compatible MCP app is actually connected and its memory tools
are available, ChatGPT should automatically use the same progressive workflow:

```text
search → timeline → get_observations → repository verification
```

If no compatible MCP app is connected, ChatGPT must continue with the connected
GitHub repository and current conversation context. It must never claim that it
queried claude-mem when the tools are unavailable.

Current ChatGPT custom-app behavior requires an MCP server reachable through a
supported remote connection; a local MCP server is not connected directly.
Secure MCP Tunnel is the supported route for private/local MCP services where
the user's ChatGPT plan and workspace support custom MCP apps. Treat this as an
optional external connection, not part of the Levyra repository bootstrap.

## Runtime behavior

Agents should query memory automatically only when it can reduce repeated
investigation or restore useful continuity. Good triggers include:

- "continue where we left off";
- a bug or PR investigated in an earlier session;
- a repeated build/CI failure;
- a previously rejected implementation approach;
- a cross-session architectural decision;
- a request whose answer depends on what was already tried.

Do not query memory for tiny self-contained edits where current code already
answers the task.

If memory tools are slow, unavailable, unhealthy, stale, or contradictory:

1. stop depending on them;
2. continue with repository evidence;
3. report the memory limitation once if it matters;
4. never block the task waiting for memory.

## Health and recovery

Normal checks:

```text
npx claude-mem@13.15.0 status
npx claude-mem@13.15.0 doctor
```

Official repair path:

```text
npx claude-mem@13.15.0 repair
npx claude-mem@13.15.0 start
npx claude-mem@13.15.0 doctor
```

Do not repeatedly reinstall or recycle the worker in a loop. If one repair does
not restore health, disable reliance on memory for the session and investigate
the worker separately.

## Updating the pin

Before changing the pinned version:

1. inspect upstream release notes and recent Windows/hook issues;
2. verify Claude Code, Codex CLI, and Antigravity installers still use the same
   supported IDE identifiers;
3. verify privacy/cloud-sync defaults and telemetry behavior;
4. review whether the Windows fail-open guard is still needed;
5. update both setup scripts and this document together;
6. run:

```text
python3 scripts/validate_claude_mem.py
python3 scripts/validate_agent_config.py
python3 scripts/validate_ai_efficiency.py
python3 scripts/validate_matt_skills.py
python3 scripts/ai_quality_gate.py --profile fast
```

Run the full AI quality gate before push or pull-request publication.
