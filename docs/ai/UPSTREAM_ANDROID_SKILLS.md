# Upstream Kotlin and Material 3 skills

This is Levyra's integration of two upstream, separately maintained skill collections:

- [maxrave-dev/kotlin-footguns](https://github.com/maxrave-dev/kotlin-footguns): Kotlin, Compose, Media3, state, Room, remote APIs, KMP, build pitfalls and an optional diff scanner.
- [hamen/material-3-skill](https://github.com/hamen/material-3-skill): Compose-first Material Design 3 / Material 3 Expressive components, theme, typography, shapes, motion, navigation, layout and accessibility.

Do not vendor these repositories or create a second canonical skill tree in Levyra. The repository's root/path `AGENTS.md`, existing architecture, product invariants and tested implementation outrank generic upstream examples. Follow the repository's **maximum two active skill bodies per phase** rule: do not preload the complete upstream collections.

## Claude Code

The canonical `.agents/claude/settings.json` registers upstream marketplace sources and enables `kotlin-footguns@maxrave` and `material-3@material-3-skill` for the project. `scripts/sync_agent_runtime.py --runtime claude` projects these settings to ignored native `.claude/settings.json`.

Claude Code's official manual commands, if marketplace/plugin discovery does not activate them:

```text
/plugin marketplace add maxrave-dev/kotlin-footguns
/plugin install kotlin-footguns@maxrave
/plugin marketplace add hamen/material-3-skill
/plugin install material-3@material-3-skill
```

Restart Claude Code after plugin installation. Project configuration declares desired plugins; it cannot guarantee that a particular machine has downloaded them.

## Codex

The project setup scripts install upstream packages **globally for Codex**, without modifying tracked `.agents/skills/` or writing generated files into the repository:

```bash
npx skills@latest add maxrave-dev/kotlin-footguns -g -a codex -y
npx skills@latest add hamen/material-3-skill -g -a codex -y -s material-3
```

Use `scripts/setup-ai.ps1` on Windows or `scripts/setup-ai.sh` on other systems. The Codex bootstrap `scripts/ensure-codex-tooling.*` also checks for missing skills and installs them when `npx` is available. Bootstrap is fail-open. Installation is not proof of skill execution; the agent must actually consult the relevant skill body for the task. The expected Codex global path is `~/.agents/skills/<skill-name>/SKILL.md`.

## Focused automatic application

1. **Implementation phase:** select at most two relevant Levyra skills through `scripts/agent_skill_router.py` and inspect current project code. For UI changes use `levyra-compose` and `levyra-design-taste` when routed.
2. **Material 3 design/compliance phase:** for a new or materially modified Android UI, open the installed upstream `material-3` skill and only the relevant reference files. Check Material 3 Expressive patterns against the APIs actually supported by Levyra's current Compose version. Preserve branding, behavior, accessibility, UX and theme semantics; do not force experimental migrations.
3. **Kotlin footgun review phase:** for Kotlin/KMP code changes, open only the applicable upstream area index and then just the trap documents whose symptoms match the changed code. Choose `compose-screens-footguns` or `compose-visuals-footguns` for Compose UI, `media-playback-footguns` for players/audio, `data-layer-footguns` for Room/cache, `state-and-background-footguns` for Flow/services, `remote-api-footguns` for network/auth, `kmp-architecture-footguns` for architecture, `desktop-and-build-footguns` for desktop/build, or `engineering-method-footguns` for process.
4. **Validation phase:** run the smallest real relevant tests and the repository gates. If `footgun-scan` is installed, apply it to a meaningful Kotlin diff when useful; inspect each finding against its trap before accepting it. Do not rewrite code based only on a heuristic hit.

A phase is a targeted read/review, not a request to launch extra agents or spend tokens reading all 224 traps. When an upstream package is unavailable, record the missed upstream review as **UNAVAILABLE/UNVERIFIED**, continue other permitted validation, and do not claim that the scan or review passed. Do not retry unavailable network installs indefinitely.

## Quality bar for new Android UI

Material 3 Expressive is the default direction for any newly created or materially updated UI, but it is not permission to redesign untouched pages. Aim for distinctive music-first hierarchy, coherent rhythm, appropriate vibrant accents, fluid but economical motion, thoughtful empty/loading/offline/error states and immediate usability. Avoid neon, random green/red status cards, gratuitous chips, inconsistent typography/rounding, default AI-looking layouts and expensive animation that threatens playback. Inspect actual screenshots or emulator/device rendering when available. A compile pass cannot verify visual polish, accessibility or interaction.

## Provenance and maintenance

Both sources are upstream projects, installed through their own published mechanisms. No third-party trap files, design-reference text or executable code is copied into Levyra. Keep installs current deliberately after reviewing upstream changes, and never treat an external skill as authority over Levyra's security, publication, licensing or build rules.
