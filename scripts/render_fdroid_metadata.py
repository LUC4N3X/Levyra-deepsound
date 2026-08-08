#!/usr/bin/env python3
"""Render and verify Levyra's fdroiddata metadata before a GitLab push."""

from __future__ import annotations

import argparse
import difflib
import re
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_PROPERTIES = ROOT / "gradle.properties"
DEFAULT_TEMPLATE = ROOT / "fdroid" / "metadata" / "com.luc4n3x.levyra.yml.in"
APP_ID = "com.luc4n3x.levyra"
VERSION_PATTERN = re.compile(r"^[0-9]+(?:\.[0-9]+){0,3}(?:[-+][0-9A-Za-z.-]+)?$")
COMMIT_PATTERN = re.compile(r"^[0-9a-f]{40}$")


class MetadataError(ValueError):
    """Raised when release inputs cannot produce valid F-Droid metadata."""


def read_properties(path: Path) -> dict[str, str]:
    properties: dict[str, str] = {}
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        properties[key.strip()] = value.strip()
    return properties


def expected_version_code(version: str) -> int:
    base_version = re.split(r"[-+]", version, maxsplit=1)[0]
    components = [int(component) for component in base_version.split(".")]
    if len(components) > 4:
        raise MetadataError(f"Unsupported Android version: {version}")
    components.extend([0] * (4 - len(components)))
    major, minor, patch, build = components
    if any(component < 0 or component > 99 for component in components):
        raise MetadataError(f"Android version component out of range: {version}")
    return major * 1_000_000 + minor * 10_000 + patch * 100 + build


def release_values(properties_path: Path) -> tuple[str, int]:
    properties = read_properties(properties_path)
    version = properties.get("levyraVersionName", "")
    version_code_text = properties.get("levyraVersionCode", "")
    if not VERSION_PATTERN.fullmatch(version):
        raise MetadataError(f"Invalid levyraVersionName: {version!r}")
    if not version_code_text.isdigit() or int(version_code_text) <= 0:
        raise MetadataError(f"Invalid levyraVersionCode: {version_code_text!r}")
    version_code = int(version_code_text)
    expected = expected_version_code(version)
    if version_code != expected:
        raise MetadataError(
            f"levyraVersionCode {version_code} does not match {version} (expected {expected})"
        )
    return version, version_code


def resolve_commit(commit: str | None) -> str:
    resolved = commit
    if resolved is None:
        resolved = subprocess.check_output(
            ["git", "rev-parse", "HEAD"], cwd=ROOT, text=True
        ).strip()
    resolved = resolved.lower()
    if not COMMIT_PATTERN.fullmatch(resolved):
        raise MetadataError(f"Commit must be a full 40-character SHA-1: {resolved!r}")
    return resolved


def validate_rendered_metadata(text: str, version: str, version_code: int, commit: str) -> None:
    if "@" in text:
        raise MetadataError("Rendered metadata still contains a template placeholder")
    required_lines = (
        "    en-US: Streams and metadata are obtained from YouTube, YouTube Music, and \n",
        "Binaries: \n",
        f"  - versionName: {version}\n",
        f"    versionCode: {version_code}\n",
        f"    commit: {commit}\n",
        f"CurrentVersion: {version}\n",
        f"CurrentVersionCode: {version_code}\n",
        '    prebuild: sdkmanager "platforms;android-37.0" "build-tools;36.0.0"\n',
        "      - levyraFdroidBuild=true\n",
        "AllowedAPKSigningKeys: \n",
        "UpdateCheckData: \n",
    )
    missing = [line.rstrip("\n") for line in required_lines if line not in text]
    if missing:
        raise MetadataError("Missing required F-Droid metadata: " + ", ".join(missing))
    if text.count("Builds:\n") != 1 or text.count("  - versionName:") != 1:
        raise MetadataError("The submission must contain exactly one Levyra build")
    if "\r" in text:
        raise MetadataError("F-Droid metadata must use LF line endings")


def render_metadata(properties_path: Path, template_path: Path, commit: str | None) -> str:
    version, version_code = release_values(properties_path)
    resolved_commit = resolve_commit(commit)
    template = template_path.read_text(encoding="utf-8")
    replacements = {
        "@FDROID_YAML_SPACE@": " ",
        "@VERSION@": version,
        "@VERSION_CODE@": str(version_code),
        "@COMMIT@": resolved_commit,
    }
    for placeholder, value in replacements.items():
        if placeholder not in template:
            raise MetadataError(f"Template is missing {placeholder}")
        template = template.replace(placeholder, value)
    if not template.endswith("\n"):
        template += "\n"
    validate_rendered_metadata(template, version, version_code, resolved_commit)
    return template


def read_exact_text(path: Path) -> str:
    with path.open("r", encoding="utf-8", newline="") as source:
        return source.read()


def write_exact_text(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as destination:
        destination.write(text)


def check_metadata(expected: str, candidate_path: Path) -> bool:
    actual = read_exact_text(candidate_path)
    # Git for Windows can check out LF-normalized YAML as CRLF. GitLab receives
    # the normalized blob, so compare normalized line endings while preserving
    # all other whitespace (especially F-Droid's required `Binaries: ` space).
    actual = actual.replace("\r\n", "\n")
    if actual == expected:
        return True
    diff = difflib.unified_diff(
        actual.splitlines(keepends=True),
        expected.splitlines(keepends=True),
        fromfile=str(candidate_path),
        tofile="generated-fdroid-metadata",
    )
    sys.stderr.writelines(diff)
    return False


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--properties", type=Path, default=DEFAULT_PROPERTIES)
    parser.add_argument("--template", type=Path, default=DEFAULT_TEMPLATE)
    parser.add_argument("--commit", help="Full source commit; defaults to the current HEAD")
    destination = parser.add_mutually_exclusive_group()
    destination.add_argument("--output", type=Path, help="Write the canonical metadata file")
    destination.add_argument("--check", type=Path, help="Compare an existing fdroiddata file")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    try:
        rendered = render_metadata(args.properties, args.template, args.commit)
        if args.check:
            if not check_metadata(rendered, args.check):
                print("F-Droid metadata differs from the canonical Levyra output.", file=sys.stderr)
                return 1
            print(f"F-Droid metadata is canonical: {args.check}")
        elif args.output:
            write_exact_text(args.output, rendered)
            print(f"Rendered F-Droid metadata: {args.output}")
        else:
            sys.stdout.write(rendered)
        return 0
    except (MetadataError, OSError, subprocess.CalledProcessError) as error:
        print(f"F-Droid metadata preflight failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
