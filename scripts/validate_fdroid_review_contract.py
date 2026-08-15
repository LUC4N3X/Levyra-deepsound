#!/usr/bin/env python3
"""Fail early when Levyra drifts from its reviewed F-Droid behavior."""

from __future__ import annotations

import argparse
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def collect_violations(
    root: Path = ROOT,
    overrides: dict[str, str] | None = None,
) -> list[str]:
    overrides = overrides or {}
    violations: list[str] = []

    def read(relative: str) -> str:
        if relative in overrides:
            return overrides[relative]
        path = root / relative
        if not path.is_file():
            violations.append(f"missing required file: {relative}")
            return ""
        return path.read_text(encoding="utf-8")

    def require(relative: str, text: str, needle: str, reason: str) -> None:
        if needle not in text:
            violations.append(f"{relative}: {reason} (missing {needle!r})")

    def require_count(
        relative: str,
        text: str,
        needle: str,
        minimum: int,
        reason: str,
    ) -> None:
        if text.count(needle) < minimum:
            violations.append(
                f"{relative}: {reason} (expected at least {minimum} occurrences of {needle!r})"
            )

    def forbid(relative: str, text: str, needle: str, reason: str) -> None:
        if needle in text:
            violations.append(f"{relative}: {reason} (found {needle!r})")

    build_path = "app/build.gradle.kts"
    build = read(build_path)
    require(
        build_path,
        build,
        'buildConfigField("boolean", "UPSTREAM_UPDATES_ENABLED", (!isFdroidBuild).toString())',
        "the F-Droid build must disable the upstream updater",
    )
    require(
        build_path,
        build,
        'buildConfigField("boolean", "REMOTE_ANNOUNCEMENTS_ENABLED", (!isFdroidBuild).toString())',
        "the F-Droid build must disable remote announcements",
    )

    prompt_path = (
        "app/src/main/java/com/luc4n3x/levyra/ui/support/"
        "OpenSourceSupportPrompt.kt"
    )
    prompt = read(prompt_path)
    require_count(
        prompt_path,
        prompt,
        "if (!BuildConfig.REMOTE_ANNOUNCEMENTS_ENABLED) return",
        2,
        "both the prompt gate and settings card must be absent from F-Droid builds",
    )

    announcement_path = (
        "app/src/main/java/com/luc4n3x/levyra/ui/support/"
        "RemoteAnnouncementRepository.kt"
    )
    announcements = read(announcement_path)
    require_count(
        announcement_path,
        announcements,
        "BuildConfig.REMOTE_ANNOUNCEMENTS_ENABLED",
        3,
        "repository entry points must remain gated for F-Droid",
    )

    preferences_path = "app/src/main/java/com/luc4n3x/levyra/data/LevyraPreferences.kt"
    preferences = read(preferences_path)
    require(
        preferences_path,
        preferences,
        "internal const val DEFAULT_SPONSORBLOCK_ENABLED = true",
        "SponsorBlock is an important playback feature and remains enabled by default",
    )

    exporter_path = (
        "app/src/main/java/com/luc4n3x/levyra/player/offline/"
        "OfflineAudioExporter.kt"
    )
    exporter = read(exporter_path)
    for forbidden, reason in (
        ("MediaStore.Downloads", "offline audio must not silently fall back to Downloads"),
        ("Environment.DIRECTORY_DOWNLOADS", "offline audio must stay under Music/Levyra"),
        ("saveScopedDownloadFile", "the Downloads fallback must not return"),
        ("downloadsDestinationLabel", "the Downloads destination must not return"),
    ):
        forbid(exporter_path, exporter, forbidden, reason)
    for required, reason in (
        ("isMp4AudioExportUrl(track.streamUrl)", "provided streams must be checked as MP4/M4A"),
        ("if (!downloaded.container.supportsEmbeddedMetadata)", "non-M4A downloads must fail"),
        ("LevyraM4aTagWriter.write", "M4A metadata must be embedded"),
        ("MediaStore.Audio.Media.EXTERNAL_CONTENT_URI", "audio must be registered in MediaStore.Audio"),
    ):
        require(exporter_path, exporter, required, reason)

    resolver_path = "app/src/main/java/com/luc4n3x/levyra/data/PlaybackResolver.kt"
    resolver = read(resolver_path)
    require(
        resolver_path,
        resolver,
        "preferMp4Audio && !isMp4OfflineAudioCandidate",
        "offline resolution must reject WebM/Opus instead of merely preferring MP4",
    )
    forbid(
        resolver_path,
        resolver,
        'format.contains("mpeg")',
        "generic MPEG matching would incorrectly accept MP3 as MPEG-4",
    )

    viewmodel_path = (
        "app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt"
    )
    viewmodel = read(viewmodel_path)
    for forbidden in (
        'offlineExportMessage = "Salvato in',
        'title = "Caricamento playlist"',
        'searchError = if (data.isEmpty) "Nessun risultato',
        'searchError = error.message ?:',
    ):
        forbid(
            viewmodel_path,
            viewmodel,
            forbidden,
            "user-visible runtime text and errors must use LevyraStrings",
        )
    for required in (
        "strings.formatOfflineExportSaved(",
        "strings.loadingSharedPlaylist",
        "strings.formatNoSearchResults(clean)",
        ".localizeUserError(message)",
    ):
        require(
            viewmodel_path,
            viewmodel,
            required,
            "the reviewed localization path must remain in use",
        )

    descriptions = {
        "fastlane/metadata/android/en-US/full_description.txt": (
            "YouTube",
            "Apple Music/iTunes",
            "Deezer",
            "Tidal",
            "Qobuz",
            "Wikidata",
            "LRCLIB",
            "Lyrics.ovh",
            "LyricsPlus",
            "SponsorBlock",
            "enabled by default",
            "Return YouTube Dislike",
            "WebView",
            "remote announcement feed",
            "raw.githubusercontent.com",
            "github.com",
            "ZemerTeam/zemer-cipher",
        ),
        "fastlane/metadata/android/it-IT/full_description.txt": (
            "YouTube",
            "Apple Music/iTunes",
            "Deezer",
            "Tidal",
            "Qobuz",
            "Wikidata",
            "LRCLIB",
            "Lyrics.ovh",
            "LyricsPlus",
            "SponsorBlock",
            "attivo per impostazione predefinita",
            "Return YouTube Dislike",
            "WebView",
            "feed degli annunci remoti",
            "raw.githubusercontent.com",
            "github.com",
            "ZemerTeam/zemer-cipher",
        ),
    }
    for relative, disclosures in descriptions.items():
        description = read(relative)
        for disclosure in disclosures:
            require(
                relative,
                description,
                disclosure,
                "F-Droid network behavior must remain explicitly disclosed",
            )

    required_tests = (
        "app/src/test/java/com/luc4n3x/levyra/data/LevyraPreferencesDefaultsTest.kt",
        "app/src/test/java/com/luc4n3x/levyra/data/PlaybackResolverOfflineExportTest.kt",
        "app/src/test/java/com/luc4n3x/levyra/player/offline/OfflineAudioExporterTest.kt",
        "app/src/test/java/com/luc4n3x/levyra/player/offline/tagging/LevyraM4aTagWriterTest.kt",
        "app/src/test/java/com/luc4n3x/levyra/ui/i18n/LevyraStringsTest.kt",
        "app/src/test/java/com/luc4n3x/levyra/ui/support/RemoteAnnouncementRepositoryTest.kt",
    )
    for required_test in required_tests:
        read(required_test)

    validator_command = "python3 scripts/validate_fdroid_review_contract.py"
    workflow_requirements = {
        ".github/workflows/pr-check.yml": (
            validator_command,
            "LevyraPreferencesDefaultsTest",
            "PlaybackResolverOfflineExportTest",
            "OfflineAudioExporterTest",
            "LevyraM4aTagWriterTest",
            "LevyraStringsTest",
            "RemoteAnnouncementRepositoryTest",
        ),
        ".github/workflows/fdroid-metadata-preflight.yml": (validator_command,),
        ".github/workflows/release-apk.yml": (validator_command,),
        ".github/workflows/release-guard.yml": (validator_command,),
    }
    for relative, requirements in workflow_requirements.items():
        workflow = read(relative)
        for requirement in requirements:
            require(
                relative,
                workflow,
                requirement,
                "the F-Droid review contract must remain a blocking CI/release gate",
            )

    return violations


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=ROOT)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    violations = collect_violations(args.root.resolve())
    if violations:
        print("F-Droid review contract failed:", file=sys.stderr)
        for violation in violations:
            print(f"- {violation}", file=sys.stderr)
        return 1
    print("F-Droid review contract passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
