#!/usr/bin/env python3
"""Run Levyra's cross-runtime quality gate before commit, push, and CI."""

from __future__ import annotations

import argparse
import os
import re
import shutil
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, Sequence

ROOT = Path(__file__).resolve().parents[1]
ALLOWLIST_PATH = ROOT / "scripts" / "ai_quality_gate_allowlist.txt"

SENSITIVE_FILENAMES = {
    ".env",
    "local.properties",
}
SENSITIVE_SUFFIXES = {
    ".jks",
    ".keystore",
    ".key",
    ".p12",
    ".pfx",
    ".pem",
}
GENERATED_SEGMENTS = {
    ".gradle",
    ".idea",
    "build",
}
PRIVATE_KEY_MARKERS = (
    "-----BEGIN " + "OPENSSH PRIVATE KEY-----",
    "-----BEGIN " + "PRIVATE KEY-----",
    "-----BEGIN " + "RSA PRIVATE KEY-----",
)
GITHUB_TOKEN_RE = re.compile(r"(?:ghp|github_pat)_[A-Za-z0-9_]{20,}")
SECRET_ASSIGNMENT_RE = re.compile(
    r"(?i)(?<![\w])(?:api[_-]?key|client[_-]?secret|password|private[_-]?key|token)(?![\w])"
    r"\s*([:=])\s*[\"']?([^\s\"']{8,})"
)
KOTLIN_SECRET_TYPE_ANNOTATION_RE = re.compile(
    r"(?i)(?<![\w])(?:api[_-]?key|client[_-]?secret|password|private[_-]?key|token)(?![\w])"
    r"\s*:\s*(?:String|Boolean|Byte|Short|Int|Long|Float|Double|Char)\??\b"
)
SAFE_DYNAMIC_SECRET_MARKERS = (
    "${",
    "$env",
    "secrets.",
)
SAFE_SYNTHETIC_VALUE_RE = re.compile(
    r"(?:^|[_-])(?:dummy|example|placeholder|redacted|test)(?:[_-]|$)"
)
ROOT_GRADLE_FILES = {
    "build.gradle.kts",
    "gradle.properties",
    "settings.gradle.kts",
}


@dataclass(frozen=True)
class ChangeKinds:
    android: bool
    desktop: bool
    extractor: bool


@dataclass(frozen=True)
class GateCommand:
    label: str
    argv: tuple[str, ...]
    timeout_seconds: int = 600


def run_capture(argv: Sequence[str]) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        argv,
        cwd=ROOT,
        check=False,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        timeout=30,
    )


def resolve_git_ref(ref: str) -> str | None:
    result = run_capture(("git", "rev-parse", "--verify", f"{ref}^{{commit}}"))
    return result.stdout.strip() if result.returncode == 0 else None


def select_base_ref(explicit: str | None) -> tuple[str, str]:
    if explicit:
        candidates = (explicit,)
    else:
        github_base = os.environ.get("GITHUB_BASE_REF", "").strip()
        candidates = tuple(
            candidate
            for candidate in (
                f"origin/{github_base}" if github_base else "",
                os.environ.get("AI_QUALITY_BASE_REF", "").strip(),
                "origin/main",
                "origin/master",
                "main",
                "master",
                "HEAD~1",
            )
            if candidate
        )

    for candidate in candidates:
        commit = resolve_git_ref(candidate)
        if commit:
            merge_base = run_capture(("git", "merge-base", "HEAD", commit))
            if merge_base.returncode == 0 and merge_base.stdout.strip():
                return candidate, merge_base.stdout.strip()

    requested = explicit or ", ".join(candidates)
    raise RuntimeError(f"cannot resolve a quality-gate base from: {requested}")


def parse_nul_paths(output: str) -> set[str]:
    return {item.replace("\\", "/") for item in output.split("\0") if item}


def git_paths(*args: str) -> set[str]:
    result = run_capture(("git", *args, "-z"))
    if result.returncode != 0:
        raise RuntimeError(result.stderr.strip() or f"git {' '.join(args)} failed")
    return parse_nul_paths(result.stdout)


def collect_changed_files(base_commit: str) -> tuple[set[str], set[str]]:
    changed = set()
    changed.update(git_paths("diff", "--name-only", f"{base_commit}...HEAD"))
    changed.update(git_paths("diff", "--name-only", "--cached"))
    changed.update(git_paths("diff", "--name-only"))
    untracked = git_paths("ls-files", "--others", "--exclude-standard")
    changed.update(untracked)
    return changed, untracked


def read_allowlist() -> set[str]:
    if not ALLOWLIST_PATH.is_file():
        return set()
    return {
        line.strip().replace("\\", "/")
        for line in ALLOWLIST_PATH.read_text(encoding="utf-8").splitlines()
        if line.strip() and not line.lstrip().startswith("#")
    }


def classify_changes(paths: Iterable[str]) -> ChangeKinds:
    normalized = {path.replace("\\", "/") for path in paths}
    root_gradle_changed = bool(normalized & ROOT_GRADLE_FILES) or any(
        path.startswith("gradle/") for path in normalized
    )
    return ChangeKinds(
        android=root_gradle_changed
        or any(path.startswith("app/") for path in normalized),
        desktop=any(path.startswith("desktop/") for path in normalized),
        extractor=root_gradle_changed
        or any(path.startswith("third_party/LevyraExtractor/") for path in normalized),
    )


def forbidden_path_findings(paths: Iterable[str], allowlist: set[str]) -> list[str]:
    findings: list[str] = []
    for raw_path in sorted(paths):
        path = raw_path.replace("\\", "/")
        if path in allowlist:
            continue
        file_path = Path(path)
        lowered_parts = {part.lower() for part in file_path.parts}
        if file_path.name.lower() in SENSITIVE_FILENAMES:
            findings.append(f"sensitive local file changed: {path}")
        elif file_path.suffix.lower() in SENSITIVE_SUFFIXES:
            findings.append(f"signing/private-key file changed: {path}")
        elif lowered_parts & GENERATED_SEGMENTS:
            findings.append(f"generated or IDE output changed: {path}")
    return findings


def scan_added_lines(lines: Iterable[str], source: str) -> list[str]:
    findings: list[str] = []
    for line_number, line in enumerate(lines, start=1):
        value = line.strip()
        if value.startswith(("+++", "---")):
            continue
        if value.startswith(("<<<<<<<", "=======", ">>>>>>>")):
            findings.append(f"{source}:{line_number}: unresolved conflict marker")
        lowered = value.lower()
        for marker in PRIVATE_KEY_MARKERS:
            if marker.lower() in lowered:
                findings.append(f"{source}:{line_number}: possible secret/private key marker")
                break
        if GITHUB_TOKEN_RE.search(value):
            findings.append(f"{source}:{line_number}: possible GitHub token")
        match = SECRET_ASSIGNMENT_RE.search(value)
        if match:
            delimiter = match.group(1)
            if delimiter == ":" and KOTLIN_SECRET_TYPE_ANNOTATION_RE.search(value):
                continue
            assigned = match.group(2).lower()
            is_dynamic = any(marker in assigned for marker in SAFE_DYNAMIC_SECRET_MARKERS)
            is_synthetic = SAFE_SYNTHETIC_VALUE_RE.search(assigned) is not None
            if not is_dynamic and not is_synthetic:
                findings.append(f"{source}:{line_number}: possible committed credential")
    return findings


def collect_patch(base_commit: str) -> str:
    patches: list[str] = []
    for argv in (
        ("git", "diff", "--unified=0", f"{base_commit}...HEAD"),
        ("git", "diff", "--unified=0", "--cached"),
        ("git", "diff", "--unified=0"),
    ):
        result = run_capture(argv)
        if result.returncode != 0:
            raise RuntimeError(result.stderr.strip() or f"{' '.join(argv)} failed")
        patches.append(result.stdout)
    return "\n".join(patches)


def scan_patch(patch: str) -> list[str]:
    added = [
        line[1:]
        for line in patch.splitlines()
        if line.startswith("+") and not line.startswith("+++")
    ]
    return scan_added_lines(added, "git-diff")


def scan_untracked(paths: Iterable[str]) -> list[str]:
    findings: list[str] = []
    for path in sorted(paths):
        absolute = ROOT / path
        try:
            content = absolute.read_bytes()
        except OSError as exc:
            findings.append(f"cannot inspect untracked file {path}: {exc}")
            continue
        if b"\0" in content:
            findings.append(f"untracked binary file requires an allowlist entry: {path}")
            continue
        try:
            text = content.decode("utf-8")
        except UnicodeDecodeError:
            findings.append(f"untracked non-UTF-8 file requires an allowlist entry: {path}")
            continue
        findings.extend(scan_added_lines(text.splitlines(), path))
        for line_number, line in enumerate(text.splitlines(), start=1):
            if line.rstrip() != line:
                findings.append(f"{path}:{line_number}: trailing whitespace")
    return findings


def binary_diff_findings(base_commit: str, allowlist: set[str]) -> list[str]:
    findings: list[str] = []
    for args in (
        ("diff", "--numstat", f"{base_commit}...HEAD"),
        ("diff", "--numstat", "--cached"),
        ("diff", "--numstat"),
    ):
        result = run_capture(("git", *args))
        if result.returncode != 0:
            raise RuntimeError(result.stderr.strip() or f"git {' '.join(args)} failed")
        for line in result.stdout.splitlines():
            parts = line.split("\t", 2)
            if len(parts) == 3 and parts[0] == "-" and parts[1] == "-":
                path = parts[2].replace("\\", "/")
                if path not in allowlist:
                    findings.append(f"binary change requires an allowlist entry: {path}")
    return sorted(set(findings))


def find_bash() -> str | None:
    discovered = shutil.which("bash")
    if discovered:
        return discovered
    if os.name == "nt":
        for candidate in (
            Path(os.environ.get("ProgramFiles", r"C:\Program Files")) / "Git/bin/bash.exe",
            Path(os.environ.get("ProgramFiles", r"C:\Program Files")) / "Git/usr/bin/bash.exe",
        ):
            if candidate.is_file():
                return str(candidate)
    return None


def find_powershell() -> str | None:
    return shutil.which("pwsh") or shutil.which("powershell")


def gradle_wrapper(directory: str = ".") -> str:
    name = "gradlew.bat" if os.name == "nt" else "gradlew"
    path = ROOT / directory / name
    return str(path)


def build_commands(
    paths: set[str], profile: str, python: str | None = None
) -> tuple[list[GateCommand], list[str]]:
    """Build the validation commands selected by changed paths and gate profile."""
    python = python or sys.executable
    kinds = classify_changes(paths)
    commands = [
        GateCommand("Validate agent configuration", (python, "scripts/validate_agent_config.py")),
        GateCommand("Validate AI efficiency", (python, "scripts/validate_ai_efficiency.py")),
        GateCommand(
            "Validate F-Droid review contract",
            (python, "scripts/validate_fdroid_review_contract.py"),
        ),
        GateCommand(
            "Run repository script tests",
            (python, "-m", "unittest", "discover", "-s", "scripts/tests", "-p", "test_*.py"),
        ),
    ]
    blocked: list[str] = []

    shell_files = sorted(path for path in paths if path.endswith(".sh"))
    if shell_files:
        bash = find_bash()
        if bash:
            commands.append(GateCommand("Check Bash syntax", (bash, "-n", *shell_files)))
            if "scripts/setup-ai.sh" in shell_files:
                commands.append(
                    GateCommand(
                        "Dry-run Unix AI setup",
                        (bash, "scripts/setup-ai.sh", "--dry-run", "--skip-hooks"),
                    )
                )
        else:
            blocked.append("Bash is required to validate changed .sh files")

    powershell_files = sorted(path for path in paths if path.endswith(".ps1"))
    if powershell_files:
        powershell = find_powershell()
        if powershell:
            quoted_paths = ",".join(
                "'" + path.replace("'", "''") + "'" for path in powershell_files
            )
            parser = (
                f"$paths=@({quoted_paths});$failed=$false; foreach($path in $paths){{"
                "$tokens=$null;$errors=$null;"
                "[System.Management.Automation.Language.Parser]::ParseFile((Resolve-Path $path),"
                "[ref]$tokens,[ref]$errors)|Out-Null; if($errors.Count){$errors|"
                "ForEach-Object{Write-Error $_};$failed=$true}}; if($failed){exit 1}"
            )
            commands.append(
                GateCommand(
                    "Check PowerShell syntax",
                    (powershell, "-NoProfile", "-Command", parser),
                )
            )
            if "scripts/setup-ai.ps1" in powershell_files:
                commands.append(
                    GateCommand(
                        "Dry-run Windows AI setup",
                        (
                            powershell,
                            "-NoProfile",
                            "-File",
                            "scripts/setup-ai.ps1",
                            "-DryRun",
                            "-SkipHooks",
                        ),
                    )
                )
        else:
            blocked.append("PowerShell is required to validate changed .ps1 files")

    if profile == "full":
        if kinds.android:
            wrapper = gradle_wrapper()
            commands.extend(
                (
                    GateCommand(
                        "Check Android runtime compatibility",
                        (python, "scripts/check_android_regex_compatibility.py"),
                    ),
                    GateCommand(
                        "Run all Android unit tests",
                        (wrapper, "--no-daemon", ":app:testDebugUnitTest"),
                        timeout_seconds=3600,
                    ),
                    GateCommand(
                        "Run Android release lint",
                        (wrapper, "--no-daemon", ":app:lintRelease", "-PlevyraFdroidBuild=true"),
                        timeout_seconds=3600,
                    ),
                    GateCommand(
                        "Compile unsigned F-Droid release",
                        (
                            wrapper,
                            "--no-daemon",
                            "--no-configuration-cache",
                            ":app:assembleRelease",
                            "-PlevyraFdroidBuild=true",
                        ),
                        timeout_seconds=3600,
                    ),
                )
            )
        if kinds.extractor:
            commands.append(
                GateCommand(
                    "Run extractor tests",
                    (
                        gradle_wrapper(),
                        "-p",
                        "third_party/LevyraExtractor",
                        "--no-daemon",
                        ":extractor:test",
                    ),
                    timeout_seconds=3600,
                )
            )
        if kinds.desktop:
            commands.append(
                GateCommand(
                    "Run Desktop checks and assembly",
                    (gradle_wrapper("desktop"), "-p", "desktop", "check", "assemble"),
                    timeout_seconds=3600,
                )
            )

    return commands, blocked


def run_command(command: GateCommand, dry_run: bool) -> bool:
    display = subprocess.list2cmdline(command.argv) if os.name == "nt" else " ".join(command.argv)
    print(f"\n==> {command.label}\n{display}")
    if dry_run:
        return True
    try:
        result = subprocess.run(
            command.argv,
            cwd=ROOT,
            check=False,
            timeout=command.timeout_seconds,
        )
    except subprocess.TimeoutExpired:
        print(
            f"[failed] {command.label}: timed out after {command.timeout_seconds}s",
            file=sys.stderr,
        )
        return False
    if result.returncode != 0:
        print(f"[failed] {command.label}: exit code {result.returncode}", file=sys.stderr)
        return False
    print(f"[passed] {command.label}")
    return True


def run_diff_checks(base_commit: str) -> list[str]:
    failures: list[str] = []
    for label, argv in (
        ("branch diff", ("git", "diff", "--check", f"{base_commit}...HEAD")),
        ("staged diff", ("git", "diff", "--cached", "--check")),
        ("working-tree diff", ("git", "diff", "--check")),
    ):
        try:
            result = subprocess.run(argv, cwd=ROOT, check=False, timeout=60)
        except subprocess.TimeoutExpired:
            failures.append(f"{label} timed out during git diff --check")
        else:
            if result.returncode != 0:
                failures.append(f"{label} failed git diff --check")
    return failures


def parse_args(argv: Sequence[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--profile", choices=("fast", "full"), default="fast")
    parser.add_argument("--base-ref", help="Git ref used to compute the complete change set")
    parser.add_argument("--dry-run", action="store_true", help="Print commands without running them")
    return parser.parse_args(argv)


def main(argv: Sequence[str] | None = None) -> int:
    args = parse_args(argv)
    try:
        base_ref, base_commit = select_base_ref(args.base_ref)
        changed, untracked = collect_changed_files(base_commit)
        allowlist = read_allowlist()
        findings = run_diff_checks(base_commit)
        findings.extend(forbidden_path_findings(changed, allowlist))
        findings.extend(binary_diff_findings(base_commit, allowlist))
        findings.extend(scan_patch(collect_patch(base_commit)))
        findings.extend(scan_untracked(untracked))
        commands, blocked = build_commands(changed, args.profile)
    except (OSError, RuntimeError, subprocess.TimeoutExpired) as exc:
        print(f"AI quality gate could not start: {exc}", file=sys.stderr)
        return 1

    print("Levyra AI Quality Gate")
    print(f"Profile: {args.profile}")
    print(f"Base: {base_ref} ({base_commit[:12]})")
    print(f"Changed files: {len(changed)}")

    if findings:
        print("\nStatic gate findings:", file=sys.stderr)
        for finding in sorted(set(findings)):
            print(f"- {finding}", file=sys.stderr)
    if blocked:
        print("\nBlocked required checks:", file=sys.stderr)
        for item in blocked:
            print(f"- {item}", file=sys.stderr)

    passed = not findings and not blocked
    if passed:
        for command in commands:
            if not run_command(command, args.dry_run):
                passed = False

    if not passed:
        print("\nAI quality gate failed.", file=sys.stderr)
        return 1

    print("\nAI quality gate passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
