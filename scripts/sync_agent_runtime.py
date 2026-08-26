#!/usr/bin/env python3
"""Materialize ignored Claude/Codex native runtime projections from .agents/."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import shutil
import stat
import tempfile
from dataclasses import dataclass
from pathlib import Path, PurePosixPath

ROOT = Path(__file__).resolve().parents[1]
MANIFEST_NAME = ".levyra-runtime-manifest.json"
MANIFEST_SCHEMA_VERSION = 1
SHA256_RE = re.compile(r"^[0-9a-f]{64}$")


class ProjectionError(RuntimeError):
    pass


@dataclass(frozen=True)
class Mapping:
    source: Path
    target_relative: Path


@dataclass(frozen=True)
class RuntimeSpec:
    target: Path
    mappings: tuple[Mapping, ...]
    owned_files: tuple[Path, ...]
    owned_directories: tuple[Path, ...]


RUNTIMES = {
    "claude": RuntimeSpec(
        target=ROOT / ".claude",
        mappings=(
            Mapping(ROOT / ".agents" / "claude" / "CLAUDE.md", Path("CLAUDE.md")),
            Mapping(ROOT / ".agents" / "claude" / "settings.json", Path("settings.json")),
            Mapping(ROOT / ".agents" / "claude" / "agents", Path("agents")),
            Mapping(ROOT / ".agents" / "claude" / "rules", Path("rules")),
            Mapping(ROOT / ".agents" / "skills", Path("skills")),
        ),
        owned_files=(Path("CLAUDE.md"), Path("settings.json")),
        owned_directories=(Path("agents"), Path("rules"), Path("skills")),
    ),
    "codex": RuntimeSpec(
        target=ROOT / ".codex",
        mappings=(
            Mapping(ROOT / ".agents" / "codex" / "config.toml", Path("config.toml")),
            Mapping(ROOT / ".agents" / "codex" / "hooks.json", Path("hooks.json")),
        ),
        owned_files=(Path("config.toml"), Path("hooks.json")),
        owned_directories=(),
    ),
}


def _relative(path: Path) -> str:
    try:
        return path.relative_to(ROOT).as_posix()
    except ValueError:
        return str(path)


def _sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _is_link_like(path: Path) -> bool:
    if path.is_symlink():
        return True
    junction_check = getattr(path, "is_junction", None)
    if callable(junction_check):
        try:
            if junction_check():
                return True
        except OSError:
            return True
    try:
        attributes = path.lstat().st_file_attributes
    except (AttributeError, OSError):
        return False
    reparse_flag = getattr(stat, "FILE_ATTRIBUTE_REPARSE_POINT", 0)
    return bool(reparse_flag and attributes & reparse_flag)


def _parse_managed_relative(value: str) -> Path:
    if not value or "\\" in value:
        raise ProjectionError(f"unsafe runtime manifest path: {value!r}")
    pure = PurePosixPath(value)
    if pure.is_absolute() or pure.as_posix() != value:
        raise ProjectionError(f"unsafe runtime manifest path: {value!r}")
    if any(part in ("", ".", "..") for part in pure.parts):
        raise ProjectionError(f"unsafe runtime manifest path: {value!r}")
    return Path(*pure.parts)


def _is_owned_relative(spec: RuntimeSpec, relative: Path) -> bool:
    if relative in spec.owned_files:
        return True
    for directory in spec.owned_directories:
        try:
            nested = relative.relative_to(directory)
        except ValueError:
            continue
        if nested.parts:
            return True
    return False


def _assert_owned_relative(spec: RuntimeSpec, relative: Path) -> None:
    if not _is_owned_relative(spec, relative):
        raise ProjectionError(
            f"runtime manifest path is outside managed namespaces: {relative.as_posix()}"
        )


def _assert_runtime_root(spec: RuntimeSpec, *, create: bool) -> None:
    target = spec.target
    if _is_link_like(target):
        raise ProjectionError(f"native runtime root must not be a link or junction: {_relative(target)}")
    if target.exists():
        if not target.is_dir():
            raise ProjectionError(f"native runtime root is not a directory: {_relative(target)}")
        return
    if create:
        target.mkdir(parents=True, exist_ok=False)


def _assert_safe_parent_chain(
    spec: RuntimeSpec, relative: Path, *, create: bool
) -> None:
    _assert_owned_relative(spec, relative)
    current = spec.target
    for part in relative.parts[:-1]:
        current = current / part
        if _is_link_like(current):
            raise ProjectionError(
                f"runtime projection parent must not be a link or junction: {_relative(current)}"
            )
        if current.exists():
            if not current.is_dir():
                raise ProjectionError(
                    f"runtime projection parent is not a directory: {_relative(current)}"
                )
        elif create:
            current.mkdir()


def _assert_regular_target(spec: RuntimeSpec, relative: Path) -> Path:
    _assert_safe_parent_chain(spec, relative, create=False)
    target = spec.target / relative
    if _is_link_like(target):
        raise ProjectionError(
            f"runtime projection target must not be a link or junction: {_relative(target)}"
        )
    if target.exists() and not target.is_file():
        raise ProjectionError(
            f"runtime projection target conflicts with local non-file content: {_relative(target)}"
        )
    return target


def _source_files(mapping: Mapping) -> list[tuple[Path, Path]]:
    source = mapping.source
    if _is_link_like(source):
        raise ProjectionError(f"canonical runtime source must not be a link or junction: {_relative(source)}")
    if not source.exists():
        raise ProjectionError(f"missing canonical runtime source: {_relative(source)}")

    if source.is_file():
        return [(source, mapping.target_relative)]

    if not source.is_dir():
        raise ProjectionError(f"unsupported canonical runtime source: {_relative(source)}")

    files: list[tuple[Path, Path]] = []
    for child in sorted(source.rglob("*")):
        if _is_link_like(child):
            raise ProjectionError(
                f"canonical runtime source must not contain links or junctions: {_relative(child)}"
            )
        if child.is_file():
            files.append((child, mapping.target_relative / child.relative_to(source)))
    return files


def _manifest_path(spec: RuntimeSpec) -> Path:
    return spec.target / MANIFEST_NAME


def _load_previous_manifest(name: str, spec: RuntimeSpec) -> dict[str, str]:
    path = _manifest_path(spec)
    if _is_link_like(path):
        raise ProjectionError(f"runtime manifest must not be a link or junction: {_relative(path)}")
    if not path.exists():
        return {}
    if not path.is_file():
        raise ProjectionError(f"runtime manifest is not a regular file: {_relative(path)}")

    try:
        document = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, UnicodeError, json.JSONDecodeError) as exc:
        raise ProjectionError(f"runtime manifest is invalid: {_relative(path)}: {exc}") from exc

    if not isinstance(document, dict):
        raise ProjectionError(f"runtime manifest must contain a JSON object: {_relative(path)}")
    if document.get("schema_version") != MANIFEST_SCHEMA_VERSION:
        raise ProjectionError(f"runtime manifest has an unsupported schema: {_relative(path)}")
    if document.get("runtime") != name or document.get("source") != ".agents":
        raise ProjectionError(f"runtime manifest identity does not match {name}: {_relative(path)}")

    managed = document.get("managed_files")
    if not isinstance(managed, dict):
        raise ProjectionError(f"runtime manifest managed_files must be an object: {_relative(path)}")

    parsed: dict[str, str] = {}
    for raw_path, digest in managed.items():
        if not isinstance(raw_path, str) or not isinstance(digest, str):
            raise ProjectionError(f"runtime manifest contains an invalid managed entry: {_relative(path)}")
        relative = _parse_managed_relative(raw_path)
        _assert_owned_relative(spec, relative)
        if SHA256_RE.fullmatch(digest) is None:
            raise ProjectionError(
                f"runtime manifest contains an invalid SHA-256 for {raw_path}: {_relative(path)}"
            )
        parsed[relative.as_posix()] = digest
    return parsed


def _collect_sources(spec: RuntimeSpec) -> list[tuple[Path, Path]]:
    sources: list[tuple[Path, Path]] = []
    seen: set[str] = set()
    for mapping in spec.mappings:
        for source, relative in _source_files(mapping):
            _assert_owned_relative(spec, relative)
            key = relative.as_posix()
            if key in seen:
                raise ProjectionError(f"duplicate runtime projection target: {key}")
            seen.add(key)
            sources.append((source, relative))
    return sources


def _preflight_current_targets(
    spec: RuntimeSpec, sources: list[tuple[Path, Path]]
) -> None:
    for _, relative in sources:
        _assert_regular_target(spec, relative)


def _stale_removal_plan(
    spec: RuntimeSpec, previous: dict[str, str], current: set[str]
) -> list[Path]:
    removals: list[Path] = []
    for raw_relative in sorted(set(previous) - current):
        relative = _parse_managed_relative(raw_relative)
        _assert_owned_relative(spec, relative)
        target = _assert_regular_target(spec, relative)
        if not target.exists():
            continue
        if _sha256(target) != previous[raw_relative]:
            raise ProjectionError(
                f"stale generated file was modified locally; refusing to delete it: {_relative(target)}"
            )
        removals.append(target)
    return removals


def _remove_empty_parents(spec: RuntimeSpec, removals: list[Path]) -> None:
    parents = sorted(
        {parent for target in removals for parent in target.parents if parent != spec.target},
        key=lambda path: len(path.parts),
        reverse=True,
    )
    for directory in parents:
        try:
            directory.relative_to(spec.target)
        except ValueError:
            continue
        if _is_link_like(directory):
            raise ProjectionError(
                f"runtime projection parent became a link or junction: {_relative(directory)}"
            )
        try:
            directory.rmdir()
        except OSError:
            pass


def _copy_atomic(source: Path, target: Path) -> None:
    temporary_path: Path | None = None
    try:
        with tempfile.NamedTemporaryFile(
            mode="wb",
            dir=target.parent,
            prefix=f".{target.name}.",
            suffix=".tmp",
            delete=False,
        ) as handle:
            temporary_path = Path(handle.name)
        shutil.copy2(source, temporary_path)
        os.replace(temporary_path, target)
        temporary_path = None
    finally:
        if temporary_path is not None:
            try:
                temporary_path.unlink()
            except FileNotFoundError:
                pass


def _write_manifest(name: str, spec: RuntimeSpec, managed: dict[str, str]) -> None:
    path = _manifest_path(spec)
    if _is_link_like(path):
        raise ProjectionError(f"runtime manifest must not be a link or junction: {_relative(path)}")
    if path.exists() and not path.is_file():
        raise ProjectionError(f"runtime manifest conflicts with local content: {_relative(path)}")

    document = {
        "schema_version": MANIFEST_SCHEMA_VERSION,
        "runtime": name,
        "source": ".agents",
        "managed_files": dict(sorted(managed.items())),
    }
    temporary_path: Path | None = None
    try:
        with tempfile.NamedTemporaryFile(
            mode="w",
            encoding="utf-8",
            dir=spec.target,
            prefix=f".{MANIFEST_NAME}.",
            suffix=".tmp",
            delete=False,
        ) as handle:
            temporary_path = Path(handle.name)
            json.dump(document, handle, indent=2, sort_keys=True)
            handle.write("\n")
        os.replace(temporary_path, path)
        temporary_path = None
    finally:
        if temporary_path is not None:
            try:
                temporary_path.unlink()
            except FileNotFoundError:
                pass


def sync_runtime(name: str, *, quiet: bool) -> None:
    spec = RUNTIMES[name]
    _assert_runtime_root(spec, create=True)
    previous = _load_previous_manifest(name, spec)
    sources = _collect_sources(spec)
    _preflight_current_targets(spec, sources)

    managed = {relative.as_posix(): _sha256(source) for source, relative in sources}
    removals = _stale_removal_plan(spec, previous, set(managed))

    for target in removals:
        target.unlink()
    _remove_empty_parents(spec, removals)

    for source, relative in sources:
        _assert_safe_parent_chain(spec, relative, create=True)
        target = _assert_regular_target(spec, relative)
        _copy_atomic(source, target)

    _write_manifest(name, spec, managed)

    if not quiet:
        print(f"{name}: refreshed {_relative(spec.target)} from canonical .agents sources")


def check_runtime(name: str) -> list[str]:
    spec = RUNTIMES[name]
    try:
        _assert_runtime_root(spec, create=False)
        if not spec.target.is_dir():
            return [f"{name}: native runtime projection is missing: {_relative(spec.target)}"]

        sources = _collect_sources(spec)
        expected = {relative.as_posix(): _sha256(source) for source, relative in sources}
        for source, relative in sources:
            target = _assert_regular_target(spec, relative)
            if not target.is_file():
                return [f"{name}: missing projected file {_relative(target)}"]
            if _sha256(source) != _sha256(target):
                return [f"{name}: stale projected file {_relative(target)}"]

        manifest = _load_previous_manifest(name, spec)
        if manifest != expected:
            return [f"{name}: runtime projection manifest is stale"]
        return []
    except ProjectionError as exc:
        return [f"{name}: {exc}"]


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

    try:
        for name in names:
            sync_runtime(name, quiet=args.quiet)
    except ProjectionError as exc:
        print(f"runtime projection refused: {exc}")
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
