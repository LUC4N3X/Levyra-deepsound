#!/usr/bin/env python3

import argparse
import pathlib
import zipfile


DEX_MARKERS = (
    b"LEVYRA_PR_DIAGNOSTICS_V1",
    b"PrDiagnosticsArtifactMarker",
    b"LEVYRA_DIAGNOSTICS_FLIGHT_RECORDER",
    b"LEVYRA_DIAGNOSTICS_ANOMALY_DETECTOR",
    b"LEVYRA_DIAGNOSTICS_PREFLIGHT",
    b"LEVYRA_DIAGNOSTICS_INTERNAL_UI",
    b"LEVYRA_DIAGNOSTICS_EXPORTER",
)
RESOURCE_MARKER = b"LEVYRA_PR_DIAGNOSTICS_RESOURCE_V1"


def scan_apk(path: pathlib.Path) -> dict[str, list[str]]:
    matches = {marker.decode(): [] for marker in DEX_MARKERS}
    matches[RESOURCE_MARKER.decode()] = []
    with zipfile.ZipFile(path) as archive:
        names = archive.namelist()
        code_candidates = [
            name
            for name in names
            if name.endswith(".dex") or name in {"resources.arsc", "AndroidManifest.xml"}
        ]
        for name in code_candidates:
            payload = archive.read(name)
            for marker in DEX_MARKERS:
                if marker in payload:
                    matches[marker.decode()].append(name)

        resource_candidates = [
            name
            for name in names
            if name == "resources.arsc" or name.startswith("res/")
        ]
        for name in resource_candidates:
            if RESOURCE_MARKER in archive.read(name):
                matches[RESOURCE_MARKER.decode()].append(name)

    return matches


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--apk", required=True, type=pathlib.Path)
    parser.add_argument("--expect", required=True, choices=("present", "absent"))
    args = parser.parse_args()
    if not args.apk.is_file():
        raise SystemExit(f"APK does not exist: {args.apk}")

    matches = scan_apk(args.apk)
    if args.expect == "present":
        missing = [marker for marker, entries in matches.items() if not entries]
        if missing:
            raise SystemExit(f"PR diagnostics APK is missing markers: {', '.join(missing)}")
    else:
        leaked = {
            marker: entries
            for marker, entries in matches.items()
            if entries
        }
        if leaked:
            raise SystemExit(f"Production APK contains PR diagnostics markers: {leaked}")

    evidence = ", ".join(
        f"{marker}={entries or 'absent'}"
        for marker, entries in matches.items()
    )
    print(f"{args.apk}: diagnostics {args.expect}; {evidence}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
