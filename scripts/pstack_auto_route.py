#!/usr/bin/env python3
"""Select one installed pstack skill for a focused Claude Code phase."""

from __future__ import annotations

import argparse
import json
import os
import re
from pathlib import Path

PLUGIN_ID = "pstack@cursor-plugins"
AUTOMATED_MARKERS = ("<github-webhook-activity", "<untrusted_external_data", "<system-reminder")

ROUTES = (
    ("technical-writing", r"(?:\b(?:readme|documentation|docs|rfc|technical writing)\b|documentazion|guida tecnica|техническ\w* документац).{0,80}(?:scriv|redig|riscriv|aggiorn|write|rewrite|draft|update|edit|polish|созда|напиши)|(?:scriv|redig|riscriv|aggiorn|write|rewrite|draft|update|edit|polish|созда|напиши).{0,80}(?:\b(?:readme|documentation|docs|rfc)\b|documentazion|guida tecnica|документац)"),
    ("unslop", r"(?:\b(?:unslop|humaniz|changelog|release notes)\b|note di rilascio|annuncio|comunicat|описание релиз|примечания к выпуску|пост для форума|descrizion\w*.{0,18}(?:\bpr\b|pull request)|(?:\bpr\b|pull request).{0,18}(?:description|body|descrizion)|(?:scriv|riscriv|traduci|write|rewrite|translate|polish|перевед|напиши|перепиши).{0,60}(?:messaggio|risposta|testo|post\b|forum\b|message\b|reply\b|text\b|forum post\b|сообщени|ответ|текст))"),
    ("blast-radius", r"blast radius|what (?:could|will) (?:this |it )?break|cosa (?:pu[oò]|potrebbe) rompere|rischi di regressione|impact analysis|analisi d.impatto|что может сломать"),
    ("interrogate", r"\binterrogate\b|adversarial review|multi.model review|find blind spots|tear this apart|revisione avversarial|revisione critica approfondita"),
    ("benchmark-checklist", r"\bbenchmark\b|speedup|measured regression|benchmarking|misur\w*.*(?:prestazion|velocit)|сравнени\w* производительности"),
    ("tdd", r"\btdd\b|test.driven|regression test|failing test first|test di regressione|test che fallisce prima|unit test.{0,60}(?:bug|fix)|тест на регресси"),
    ("architect", r"\barchitect this\b|design the architecture|architecture decision|decisione architettural|progett\w* l.architettura|disegn\w* l.architettura|архитектурн\w* решени"),
    ("correct", r"ripet\w*.{0,50}(?:errore|bug|sbagl)|same mistake again|recurring agent mistake|fix recurring mistakes|errori ricorrenti degli agenti"),
    ("how", r"how does .{2,70} work|come funziona.{2,70}|where should .{2,65} live|chi gestisce.{2,65}|как работает"),
    ("why", r"why (?:was|is) .{2,70} (?:designed|built|chosen)|perch[eé] (?:abbiamo|[eè] stato).{2,75}|decision rationale|почему выбрали"),
    ("statechart", r"\bstatechart\b|(?:tla\+|state machine).{0,50}(?:diagram|chart|verific)|diagramma degli stati"),
    ("show-me-your-work", r"show me your work|decision trail|registro delle decisioni|log delle decisioni"),
    ("figure-it-out", r"\bfigure.it.out\b|trova tu un piano.{0,45}(?:compless|migrazion)|design an auditable plan"),
    ("sharpen", r"\bsharpen\b|migliora questo prompt|make this prompt sharper"),
    ("ketchup", r"\bketchup\b|riepilog\w*.*(?:questa chat|questa sessione)|catch me up"),
    ("no-comments", r"\bno.comments\b|rimuov\w* commenti (?:al|nel) codice|remove code comments"),
)

RULES = tuple((name, re.compile(pattern, re.IGNORECASE)) for name, pattern in ROUTES)
EXPLICIT_COMMAND = re.compile(r"^\s*/pstack:[a-z][a-z0-9-]*(?=\s|$)", re.IGNORECASE)
COMMAND_MENTION = re.compile(r"/pstack:[a-z][a-z0-9-]*", re.IGNORECASE)
IMPLEMENTATION_ACTION = re.compile(
    r"(?:^|[;.!?]\s*|\b(?:and|then|also|e|poi)\s+)"
    r"\s*(?:(?:please|per favore|vai)\s+)?"
    r"(?:fix|debug|implement|refactor|repair|resolve|investigate|build|test|add|remove|modify|"
    r"correggi|sistema|risolvi|implementa|aggiungi|rimuovi|modifica|testa)\b",
    re.IGNORECASE,
)
WRITING_SKILLS = frozenset(("unslop", "technical-writing"))


def explicit_command(prompt: str) -> bool:
    return EXPLICIT_COMMAND.match(prompt) is not None


def pick_skill(prompt: str) -> str | None:
    lowered = prompt.strip().lower()
    if not lowered or any(marker in lowered for marker in AUTOMATED_MARKERS):
        return None
    if explicit_command(prompt):
        return None
    clean = COMMAND_MENTION.sub("", lowered)
    implementation = IMPLEMENTATION_ACTION.search(clean) is not None
    for name, pattern in RULES:
        if pattern.search(clean):
            if implementation and name in WRITING_SKILLS:
                continue
            return name
    return None


def installed_root(config: Path, project: Path) -> Path | None:
    registry = config / "plugins" / "installed_plugins.json"
    try:
        payload = json.loads(registry.read_text(encoding="utf-8"))
        records = payload.get("plugins", {}).get(PLUGIN_ID, [])
    except (OSError, ValueError, AttributeError, TypeError):
        return None
    if not isinstance(records, list):
        records = [records]
    for record in reversed(records):
        if not isinstance(record, dict):
            continue
        scope = record.get("scope")
        if scope in ("project", "local"):
            project_path = record.get("projectPath")
            if not isinstance(project_path, str):
                continue
            if Path(project_path).expanduser().resolve() != project.resolve():
                continue
        path_value = record.get("installPath")
        if not isinstance(path_value, str):
            continue
        candidate = Path(os.path.expandvars(path_value)).expanduser()
        if (candidate / "skills" / "unslop" / "SKILL.md").is_file():
            return candidate
    return None


def context_for(prompt: str, root: Path | None) -> str:
    if root is None:
        return ""
    if explicit_command(prompt):
        return (
            "An explicit pstack command already owns this phase. "
            "Do not load an overlapping Levyra or other marketplace workflow. "
            "Follow root/scoped AGENTS.md and retain mandatory validation and publication guards."
        )
    skill = pick_skill(prompt)
    if skill is None:
        return ""
    target = root / "skills" / skill / "SKILL.md"
    if not target.is_file():
        return ""
    adapter = root / "docs" / "harness" / "claude-code.md"
    result = (
        f"Selected primary workflow for this phase: pstack {skill}.\n"
        f"Read {target} and follow it for this phase. "
        "The plugin disables automatic Skill-tool invocation for many skills; "
        "read the installed skill file rather than calling a disabled skill command.\n"
        "Do not also invoke levyra-humanizer, another writing/review workflow, "
        "poteto-mode, Matt Pocock equivalents, or competing agents in this phase. "
        "Root and nearest scoped AGENTS.md, real tests, security and publication rules still apply. "
        "Do not automatically start subagents or multi-model panels."
    )
    if adapter.is_file():
        result += f"\nFor Cursor-only tool names, consult {adapter} only if needed."
    return result


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--prompt", default="")
    parser.add_argument("--plain", action="store_true")
    parser.add_argument("--check-installed", action="store_true")
    parser.add_argument("--plugin-root", type=Path)
    parser.add_argument("--project-root", type=Path)
    args = parser.parse_args()
    project = args.project_root or Path(os.environ.get("CLAUDE_PROJECT_DIR") or Path(__file__).resolve().parents[1])
    config = Path(os.environ.get("CLAUDE_CONFIG_DIR") or (Path.home() / ".claude")).expanduser()
    root = args.plugin_root if args.plugin_root is not None else installed_root(config, project)
    if args.check_installed:
        if root is None:
            return 1
        print(root)
        return 0
    result = context_for(args.prompt, root)
    if args.plain:
        print(result)
    else:
        print(json.dumps({"hookSpecificOutput": {"hookEventName": "UserPromptSubmit", "additionalContext": result}}))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
