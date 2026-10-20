#!/usr/bin/env python3
"""Read-only, cross-platform readiness report for Levyra's coding toolchain."""

from __future__ import annotations

import argparse
import json
import platform
import shutil
import sys
from pathlib import Path
from typing import Callable, Sequence

ROOT = Path(__file__).resolve().parents[1]
REQUIRED_FILES = {
    "root_contract": "AGENTS.md",
    "claude_hooks": ".agents/claude/settings.json",
    "codex_hooks": ".agents/codex/hooks.json",
    "context_filters": ".rtk/filters.toml",
    "context_optimizer": "scripts/levyra_context_optimizer.py",
    "agent_harness": "scripts/agent_harness.py",
    "quality_gate": "scripts/ai_quality_gate.py",
}
OPTIONAL_TOOLS = ("rtk", "claude", "codex", "agy", "headroom", "grok")


def inspect_readiness(
    root: Path | None = None,
    which: Callable[[str], str | None] | None = None,
) -> dict[str, object]:
    """Check only local files and executables, without starting any provider."""
    repo = ROOT if root is None else root
    locate = shutil.which if which is None else which
    required = {
        name: (repo / relative).is_file()
        for name, relative in REQUIRED_FILES.items()
    }
    available = {name: bool(locate(name)) for name in OPTIONAL_TOOLS}
    missing = [name for name, present in required.items() if not present]
    python_ok = sys.version_info >= (3, 10)
    return {
        "schema_version": 1,
        "platform": platform.system(),
        "python": f"{sys.version_info.major}.{sys.version_info.minor}",
        "repository_ready": not missing and python_ok,
        "required_contracts": required,
        "optional_executables": available,
        "missing_contracts": missing,
        "notes": (
            "File presence and executable discovery only: no provider logins, "
            "subscriptions, models, or live sessions are verified."
        ),
    }


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Levyra agent readiness (read-only)")
    parser.add_argument("--json", action="store_true", help="Print machine-readable status")
    parser.add_argument("--strict", action="store_true", help="Fail on missing required contracts")
    args = parser.parse_args(argv)
    report = inspect_readiness()
    if args.json:
        print(json.dumps(report, indent=2, sort_keys=True))
    else:
        state = "PASS" if report["repository_ready"] else "INCOMPLETE"
        print("Levyra Agent Toolchain Readiness")
        print(f"Repository contracts: {state}")
        for name, available in report["optional_executables"].items():
            status = "detected" if available else "not detected (optional)"
            print(f"  {name}: {status}")
        print(report["notes"])
    return 1 if args.strict and not report["repository_ready"] else 0


if __name__ == "__main__":
    raise SystemExit(main())
