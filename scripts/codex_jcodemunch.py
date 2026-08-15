#!/usr/bin/env python3
"""Bootstrap and launch Levyra's pinned jCodeMunch MCP runtime."""

from __future__ import annotations

import argparse
import os
import shutil
import subprocess
import sys
from pathlib import Path

JCODEMUNCH_VERSION = "1.108.279"
JCODEMUNCH_WHEEL_SHA256 = "d192998a9abcd8afa1474f2a1637e260a9ab48fdd0080b6b690632b2af1e30f1"
JCODEMUNCH_WHEEL_URL = (
    "https://github.com/jgravelle/jcodemunch-mcp/releases/download/"
    f"v{JCODEMUNCH_VERSION}/jcodemunch_mcp-{JCODEMUNCH_VERSION}-py3-none-any.whl"
    f"#sha256={JCODEMUNCH_WHEEL_SHA256}"
)


def repo_root() -> Path:
    return Path(__file__).resolve().parents[1]


def cache_root() -> Path:
    if os.name == "nt":
        base = Path(os.environ.get("LOCALAPPDATA", Path.home() / "AppData" / "Local"))
    else:
        base = Path(os.environ.get("XDG_CACHE_HOME", Path.home() / ".cache"))
    return base / "Levyra" / "codex-tools" / "jcodemunch" / JCODEMUNCH_VERSION


def venv_dir() -> Path:
    return cache_root() / "venv"


def venv_python() -> Path:
    if os.name == "nt":
        return venv_dir() / "Scripts" / "python.exe"
    return venv_dir() / "bin" / "python"


def jcodemunch_binary() -> Path:
    if os.name == "nt":
        return venv_dir() / "Scripts" / "jcodemunch-mcp.exe"
    return venv_dir() / "bin" / "jcodemunch-mcp"


def _run_quiet(command: list[str], *, check: bool = True) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        command,
        check=check,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )


def installed_version() -> str | None:
    binary = jcodemunch_binary()
    if not binary.is_file():
        return None
    try:
        result = _run_quiet([str(binary), "--version"])
    except (OSError, subprocess.CalledProcessError):
        return None
    output = f"{result.stdout}\n{result.stderr}"
    return JCODEMUNCH_VERSION if JCODEMUNCH_VERSION in output else None


def ensure_installed(*, quiet: bool) -> Path:
    binary = jcodemunch_binary()
    if installed_version() == JCODEMUNCH_VERSION:
        return binary

    target = venv_dir()
    if target.exists():
        shutil.rmtree(target)
    target.parent.mkdir(parents=True, exist_ok=True)

    stream = subprocess.DEVNULL if quiet else sys.stderr
    subprocess.run([sys.executable, "-m", "venv", str(target)], check=True, stdout=stream, stderr=stream)
    subprocess.run(
        [
            str(venv_python()),
            "-m",
            "pip",
            "install",
            "--disable-pip-version-check",
            "--no-input",
            JCODEMUNCH_WHEEL_URL,
        ],
        check=True,
        stdout=stream,
        stderr=stream,
    )

    if installed_version() != JCODEMUNCH_VERSION:
        raise RuntimeError(f"jCodeMunch {JCODEMUNCH_VERSION} installed but verification failed")
    return binary


def index_repository(*, quiet: bool) -> None:
    binary = ensure_installed(quiet=quiet)
    stream = subprocess.DEVNULL if quiet else sys.stderr
    subprocess.run(
        [str(binary), "index", str(repo_root())],
        check=True,
        cwd=repo_root(),
        stdout=stream,
        stderr=stream,
    )


def serve() -> int:
    try:
        binary = ensure_installed(quiet=True)
    except Exception as exc:  # fail closed for this optional MCP only; Codex itself stays usable.
        print(f"Levyra jCodeMunch bootstrap failed: {exc}", file=sys.stderr)
        return 1

    os.execv(str(binary), [str(binary)])
    return 0


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=("ensure", "index", "serve"))
    parser.add_argument("--quiet", action="store_true")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    try:
        if args.action == "serve":
            return serve()
        if args.action == "ensure":
            ensure_installed(quiet=args.quiet)
        else:
            index_repository(quiet=args.quiet)
    except Exception as exc:
        if not args.quiet:
            print(f"Levyra jCodeMunch bootstrap failed: {exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
