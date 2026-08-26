#!/usr/bin/env python3
"""Materialize ignored Claude/Codex native runtime projections from .agents/."""

from __future__ import annotations

import argparse
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RUNTIMES = {
    "claude": {
        "source": ROOT / ".agents" / "claude",
        "target": ROOT / ".claude",
        "entries": ("CLAUDE.md", "README.md", "settings.json", "agents", "hooks", "rules", "skills"),
    },
    "codex": {
        "source": ROOT / ".agents" / "codex",
        "target": ROOT / ".codex",
        "entries": ("config.toml", "hooks.json"),
    },
}


def sync_runtime(name: str, *, quiet: bool) -> None:
    spec = RUNTIMES[name]
    source: Path = spec["source"]
    target: Path = spec["target"]
    entries: tuple[str, ...] = spec["entries"]

    if not source.is_dir():
        raise SystemExit(f"missing canonical runtime directory: {source.relative_to(ROOT)}")

    target.mkdir(parents=True, exist_ok=True)
    for entry in entries:
        source_path = source / entry
        target_path = target / entry
        if not source_path.exists():
            raise SystemExit(f"missing canonical runtime entry: {source_path.relative_to(ROOT)}")

        if target_path.is_dir():
            shutil.rmtree(target_path)
        elif target_path.exists():
            target_path.unlink()

        if source_path.is_dir():
            shutil.copytree(source_path, target_path)
        else:
            shutil.copy2(source_path, target_path)

    if not quiet:
        print(
            f"{name}: refreshed {target.relative_to(ROOT)} from "
            f"{source.relative_to(ROOT)}"
        )


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Refresh ignored native runtime adapters from canonical .agents sources."
    )
    parser.add_argument("--runtime", choices=("all", "claude", "codex"), default="all")
    parser.add_argument("--quiet", action="store_true")
    args = parser.parse_args()

    names = tuple(RUNTIMES) if args.runtime == "all" else (args.runtime,)
    for name in names:
        sync_runtime(name, quiet=args.quiet)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
