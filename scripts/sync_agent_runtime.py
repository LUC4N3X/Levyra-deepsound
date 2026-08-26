#!/usr/bin/env python3
"""Materialize ignored Claude/Codex native runtime projections from .agents/."""

from __future__ import annotations

import argparse
import hashlib
import json
import shutil
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MANIFEST_NAME = ".levyra-runtime-manifest.json"


@dataclass(frozen=True)
class Mapping:
    source: Path
    target_relative: Path


@dataclass(frozen=True)
class RuntimeSpec:
    target: Path
    mappings: tuple[Mapping, ...]


RUNTIMES = {
    "claude": RuntimeSpec(
        target=ROOT / ".claude",
        mappings=(
            Mapping(ROOT / ".agents" / "claude" / "CLAUDE.md", Path("CLAUDE.md")),
            Mapping(ROOT / ".agents" / "claude" / "settings.json", Path("settings.json")),
            Mapping(ROOT / ".agents" / "claude" / "agents", Path("agents")),
            Mapping(ROOT / ".agents" / "claude" / "rules", Path("rules")),
            # Claude Code does not discover .agents/skills directly. Project skills are
            # projected from the one canonical shared skill tree into .claude/skills.
            Mapping(ROOT / ".agents" / "skills", Path("skills")),
        ),
    ),
    "codex": RuntimeSpec(
        target=ROOT / ".codex",
        mappings=(
            Mapping(ROOT / ".agents" / "codex" / "config.toml", Path("config.toml")),
            Mapping(ROOT / ".agents" / "codex" / "hooks.json", Path("hooks.json")),
        ),
    ),
}


def _relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def _source_files(mapping: Mapping) -> list[tuple[Path, Path]]:
    source = mapping.source
    if not source.exists():
        raise SystemExit(f"missing canonical runtime source: {_relative(source)}")

    if source.is_file():
        return [(source, mapping.target_relative)]

    if not source.is_dir():
        raise SystemExit(f"unsupported canonical runtime source: {_relative(source)}")

    files: list[tuple[Path, Path]] = []
    for child in sorted(source.rglob("*")):
        if child.is_file():
            files.append((child, mapping.target_relative / child.relative_to(source)))
    return files


def _manifest_path(spec: RuntimeSpec) -> Path:
    return spec.target / MANIFEST_NAME


def _load_previous_manifest(spec: RuntimeSpec) -> set[str]:
    path = _manifest_path(spec)
    if not path.is_file():
        return set()
    try:
        document = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, UnicodeError, json.JSONDecodeError):
        return set()
    paths = document.get("managed_files") if isinstance(document, dict) else None
    if not isinstance(paths, list):
        return set()
    return {str(item) for item in paths if isinstance(item, str) and item}


def _remove_stale_managed_files(spec: RuntimeSpec, stale: set[str]) -> None:
    for relative in sorted(stale, key=lambda value: (value.count("/"), value), reverse=True):
        target = spec.target / Path(relative)
        if target.is_file() or target.is_symlink():
            target.unlink()

    # Only remove now-empty directories. Unknown local files are never deleted.
    for directory in sorted(
        (path for path in spec.target.rglob("*") if path.is_dir()),
        key=lambda path: len(path.parts),
        reverse=True,
    ):
        try:
            directory.rmdir()
        except OSError:
            pass


def _prepare_target_file(target: Path) -> None:
    parent = target.parent
    chain: list[Path] = []
    while parent != parent.parent and parent != ROOT:
        chain.append(parent)
        parent = parent.parent
    for directory in reversed(chain):
        if directory.exists() and not directory.is_dir():
            directory.unlink()
        directory.mkdir(exist_ok=True)

    if target.exists() and target.is_dir():
        shutil.rmtree(target)


def _sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def sync_runtime(name: str, *, quiet: bool) -> None:
    spec = RUNTIMES[name]
    spec.target.mkdir(parents=True, exist_ok=True)

    sources: list[tuple[Path, Path]] = []
    for mapping in spec.mappings:
        sources.extend(_source_files(mapping))

    managed = {relative.as_posix() for _, relative in sources}
    previous = _load_previous_manifest(spec)
    _remove_stale_managed_files(spec, previous - managed)

    for source, relative in sources:
        target = spec.target / relative
        _prepare_target_file(target)
        shutil.copy2(source, target)

    manifest = {
        "runtime": name,
        "source": ".agents",
        "managed_files": sorted(managed),
    }
    _manifest_path(spec).write_text(
        json.dumps(manifest, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )

    if not quiet:
        print(f"{name}: refreshed {_relative(spec.target)} from canonical .agents sources")


def check_runtime(name: str) -> list[str]:
    spec = RUNTIMES[name]
    errors: list[str] = []
    expected: dict[str, Path] = {}
    for mapping in spec.mappings:
        for source, relative in _source_files(mapping):
            expected[relative.as_posix()] = source

    if not spec.target.is_dir():
        return [f"{name}: native runtime projection is missing: {_relative(spec.target)}"]

    for relative, source in expected.items():
        target = spec.target / relative
        if not target.is_file():
            errors.append(f"{name}: missing projected file {target.relative_to(ROOT).as_posix()}")
        elif _sha256(source) != _sha256(target):
            errors.append(f"{name}: stale projected file {target.relative_to(ROOT).as_posix()}")

    manifest = _load_previous_manifest(spec)
    if manifest != set(expected):
        errors.append(f"{name}: runtime projection manifest is stale")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Refresh ignored native runtime adapters from canonical .agents sources."
    )
    parser.add_argument("--runtime", choices=("all", "claude", "codex"), default="all")
    parser.add_argument("--quiet", action="store_true")
    parser.add_argument(
        "--check",
        action="store_true",
        help="verify an already-materialized local projection without modifying it",
    )
    args = parser.parse_args()

    names = tuple(RUNTIMES) if args.runtime == "all" else (args.runtime,)
    if args.check:
        errors: list[str] = []
        for name in names:
            errors.extend(check_runtime(name))
        if errors:
            for error in errors:
                print(error)
            return 1
        if not args.quiet:
            print("runtime projections match canonical .agents sources")
        return 0

    for name in names:
        sync_runtime(name, quiet=args.quiet)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
