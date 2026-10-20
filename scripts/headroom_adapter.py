#!/usr/bin/env python3
"""Levyra RTK Enhanced — Optional Headroom Integration Adapter.

Provides non-invasive, opt-in integration with Headroom (headroomlabs-ai/headroom)
for structural syntax-aware diffs (difftastic), code metrics (scc), and Model
Context Protocol (MCP) server configuration without introducing permanent resident
background processes or modifying default model providers.
"""

from __future__ import annotations

import argparse
import json
import os
import shutil
import socket
import subprocess
import sys
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Any, Sequence

ROOT = Path(__file__).resolve().parents[1]
# Local configuration lives in .levyra-tools/ (gitignored) so toggling
# Headroom does not dirty the shared Git working tree.
LOCAL_CONFIG_DIR = ROOT / ".levyra-tools"
HEADROOM_CONFIG_PATH = LOCAL_CONFIG_DIR / "headroom.json"


@dataclass
class HeadroomStatus:
    available: bool
    version: str | None
    enabled: bool
    bundled_tools: dict[str, str]
    mcp_sdk_installed: bool
    proxy_running: bool
    proxy_url: str


def get_headroom_executable() -> str | None:
    """Find headroom executable in PATH or standard Python script directories."""
    executable = shutil.which("headroom")
    if executable:
        return executable

    # Check common Windows Python script paths
    if sys.platform == "win32":
        local_app = os.environ.get("LOCALAPPDATA", "")
        if local_app:
            candidate = Path(local_app) / "Programs" / "Python" / "Python313" / "Scripts" / "headroom.exe"
            if candidate.is_file():
                return str(candidate)
            candidate11 = Path(local_app) / "Programs" / "Python" / "Python311" / "Scripts" / "headroom.exe"
            if candidate11.is_file():
                return str(candidate11)
    return None


def is_headroom_enabled() -> bool:
    """Check if repository configuration or environment enables Headroom."""
    if os.environ.get("LEVYRA_HEADROOM_ENABLED") in ("1", "true", "True", "yes"):
        return True

    if HEADROOM_CONFIG_PATH.is_file():
        try:
            data = json.loads(HEADROOM_CONFIG_PATH.read_text(encoding="utf-8"))
            return bool(data.get("enabled", False))
        except (OSError, json.JSONDecodeError):
            return False

    return False


def set_headroom_enabled(enabled: bool) -> None:
    """Update local repository configuration for Headroom without dirtying Git."""
    LOCAL_CONFIG_DIR.mkdir(parents=True, exist_ok=True)
    config = {
        "enabled": enabled,
        "updated_at": os.environ.get("SOURCE_DATE_EPOCH", ""),
        "description": "Levyra RTK Enhanced local optional Headroom configuration (gitignored)",
    }
    HEADROOM_CONFIG_PATH.write_text(json.dumps(config, indent=2), encoding="utf-8")


def is_proxy_listening(host: str = "127.0.0.1", port: int = 8787, timeout: float = 0.3) -> bool:
    """Check if a local Headroom proxy daemon is actively listening on TCP port.

    Uses raw TCP socket connection instead of HTTP URL requests to avoid
    Bandit B310 URL audit warnings and eliminate network scheme parsing.
    """
    try:
        with socket.create_connection((host, port), timeout=timeout):
            return True
    except (OSError, TimeoutError):
        return False


def probe_headroom_status(timeout: float = 2.0) -> HeadroomStatus:
    """Probe Headroom installation, version, bundled tools, and real MCP status."""
    exe = get_headroom_executable()
    if not exe:
        return HeadroomStatus(
            available=False,
            version=None,
            enabled=is_headroom_enabled(),
            bundled_tools={},
            mcp_sdk_installed=False,
            proxy_running=False,
            proxy_url="http://127.0.0.1:8787",
        )

    # 1. Version
    version: str | None = None
    try:
        proc = subprocess.run(
            [exe, "--version"],
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
            timeout=timeout,
            check=False,
        )
        if proc.returncode == 0 and proc.stdout:
            version = proc.stdout.strip().replace("headroom, version ", "").strip()
    except (OSError, subprocess.TimeoutExpired):
        version = None

    # 2. Bundled tools (difft, scc, ast-grep)
    bundled: dict[str, str] = {}
    try:
        proc = subprocess.run(
            [exe, "tools", "list"],
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
            timeout=timeout,
            check=False,
        )
        if proc.returncode == 0 and proc.stdout:
            if "difft" in proc.stdout:
                bundled["difftastic"] = "ready"
            if "scc" in proc.stdout:
                bundled["scc"] = "ready"
            if "ast-grep" in proc.stdout:
                bundled["ast-grep"] = "ready"
    except (OSError, subprocess.TimeoutExpired):
        pass

    # 3. Real MCP status probe (verify mcp CLI command exists without network hang)
    mcp_sdk_installed = False
    try:
        proc_mcp = subprocess.run(
            [exe, "mcp", "--help"],
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
            timeout=timeout,
            check=False,
        )
        if proc_mcp.returncode == 0 and "serve" in proc_mcp.stdout:
            mcp_sdk_installed = True
    except (OSError, subprocess.TimeoutExpired):
        mcp_sdk_installed = False

    # 4. Check proxy status via raw non-blocking socket
    proxy_running = is_proxy_listening("127.0.0.1", 8787, timeout=0.2)

    return HeadroomStatus(
        available=True,
        version=version,
        enabled=is_headroom_enabled(),
        bundled_tools=bundled,
        mcp_sdk_installed=mcp_sdk_installed,
        proxy_running=proxy_running,
        proxy_url="http://127.0.0.1:8787",
    )


def run_structural_diff(file_a: str, file_b: str) -> str | None:
    """Run structural diff via Headroom's bundled difftastic, falling back to None."""
    if not is_headroom_enabled():
        return None

    exe = get_headroom_executable()
    if not exe:
        return None

    try:
        proc = subprocess.run(
            [exe, "diff", file_a, file_b],
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
            timeout=10,
            check=False,
        )
        if proc.returncode in (0, 1) and proc.stdout.strip():
            return proc.stdout
    except (OSError, subprocess.TimeoutExpired):
        return None
    return None


def get_mcp_config() -> dict[str, Any]:
    """Generate Model Context Protocol (MCP) server configuration for Headroom."""
    exe = get_headroom_executable() or "headroom"
    return {
        "mcpServers": {
            "headroom": {
                "command": exe,
                "args": ["mcp", "serve"],
                "env": {
                    "LEVYRA_INTEGRATION": "1",
                },
            }
        }
    }


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Levyra RTK Enhanced Headroom Adapter")
    subparsers = parser.add_subparsers(dest="command")

    subparsers.add_parser("status", help="Probe Headroom status and capabilities")
    subparsers.add_parser("enable", help="Enable optional Headroom integration locally")
    subparsers.add_parser("disable", help="Disable optional Headroom integration locally")
    subparsers.add_parser("doctor", help="Check Headroom health and dependencies")
    subparsers.add_parser("mcp-config", help="Print MCP server JSON configuration")

    args = parser.parse_args(argv)

    if args.command == "enable":
        set_headroom_enabled(True)
        print("Levyra Headroom integration enabled (saved in .levyra-tools/headroom.json).")
        return 0

    elif args.command == "disable":
        set_headroom_enabled(False)
        print("Levyra Headroom integration disabled.")
        return 0

    elif args.command == "mcp-config":
        print(json.dumps(get_mcp_config(), indent=2))
        return 0

    elif args.command in ("doctor", "status") or args.command is None:
        status = probe_headroom_status()
        print("Levyra Headroom Integration Status")
        print("==================================")
        print(f"Installed:      {'Yes' if status.available else 'No'}")
        print(f"Version:        {status.version or 'N/A'}")
        print(f"Config Enabled: {'Yes' if status.enabled else 'No (default opt-in)'}")
        print(f"Bundled Tools:  {', '.join(status.bundled_tools.keys()) or 'None'}")
        print(f"MCP Support:    {'Available' if status.mcp_sdk_installed else 'No'}")
        print(f"Proxy Daemon:   {'Running' if status.proxy_running else 'Inactive (non-resident)'}")
        return 0

    return 0


if __name__ == "__main__":
    sys.exit(main())
