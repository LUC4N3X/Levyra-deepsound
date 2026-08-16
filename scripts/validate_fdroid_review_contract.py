#!/usr/bin/env python3
"""Fail early when Levyra drifts from its reviewed F-Droid behavior."""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]

# Runtime dependencies that are intentionally packaged in the F-Droid release.
# The coordinate is pinned here as a review contract as well as the Gradle alias:
# adding a dependency or repointing an existing alias must be reviewed explicitly.
FDROID_RUNTIME_DEPENDENCIES = {
    "androidx.core.ktx": "androidx.core:core-ktx",
    "androidx.activity.compose": "androidx.activity:activity-compose",
    "androidx.lifecycle.runtime.ktx": "androidx.lifecycle:lifecycle-runtime-ktx",
    "androidx.lifecycle.runtime.compose": "androidx.lifecycle:lifecycle-runtime-compose",
    "androidx.lifecycle.viewmodel.compose": "androidx.lifecycle:lifecycle-viewmodel-compose",
    "androidx.compose.bom": "androidx.compose:compose-bom",
    "androidx.compose.ui": "androidx.compose.ui:ui",
    "androidx.compose.ui.graphics": "androidx.compose.ui:ui-graphics",
    "androidx.compose.ui.tooling.preview": "androidx.compose.ui:ui-tooling-preview",
    "androidx.compose.material3": "androidx.compose.material3:material3",
    "androidx.compose.material.icons.extended": "androidx.compose.material:material-icons-extended",
    "androidx.compose.ui.text.googlefonts": "androidx.compose.ui:ui-text-google-fonts",
    "androidx.media3.exoplayer": "androidx.media3:media3-exoplayer",
    "androidx.media3.exoplayer.hls": "androidx.media3:media3-exoplayer-hls",
    "androidx.media3.exoplayer.dash": "androidx.media3:media3-exoplayer-dash",
    "androidx.media3.session": "androidx.media3:media3-session",
    "androidx.media3.ui": "androidx.media3:media3-ui",
    "androidx.car.app": "androidx.car.app:app",
    "androidx.car.app.projected": "androidx.car.app:app-projected",
    "androidx.media.compat": "androidx.media:media",
    "androidx.media3.datasource.okhttp": "androidx.media3:media3-datasource-okhttp",
    "androidx.media3.datasource": "androidx.media3:media3-datasource",
    "androidx.media3.database": "androidx.media3:media3-database",
    "androidx.media3.transformer": "androidx.media3:media3-transformer",
    "kotlinx.coroutines.android": "org.jetbrains.kotlinx:kotlinx-coroutines-android",
    "coil.compose": "io.coil-kt.coil3:coil-compose",
    "coil.network.okhttp": "io.coil-kt.coil3:coil-network-okhttp",
    "okhttp": "com.squareup.okhttp3:okhttp",
    "okhttp.brotli": "com.squareup.okhttp3:okhttp-brotli",
    "newpipe.extractor": "com.github.LUC4N3X:LevyraExtractor",
    "androidx.room.runtime": "androidx.room:room-runtime",
    "androidx.room.ktx": "androidx.room:room-ktx",
    "androidx.datastore.preferences": "androidx.datastore:datastore-preferences",
    "androidx.work.runtime.ktx": "androidx.work:work-runtime-ktx",
    "androidx.profileinstaller": "androidx.profileinstaller:profileinstaller",
    "kotlinx.serialization.json": "org.jetbrains.kotlinx:kotlinx-serialization-json",
    "timber": "com.jakewharton.timber:timber",
    "shimmer": "com.valentinilk.shimmer:compose-shimmer",
    "chucker.no.op": "com.github.chuckerteam.chucker:library-no-op",
    "desugar.jdk.libs.nio": "com.android.tools:desugar_jdk_libs_nio",
}

RUNTIME_DEPENDENCY_CALL = re.compile(
    r"^\s*(?:implementation|api|releaseImplementation|runtimeOnly|coreLibraryDesugaring)\s*\("
)
LIBS_ACCESSOR = re.compile(r"\blibs\.([A-Za-z0-9_.]+)")
CATALOG_LIBRARY = re.compile(r'^([A-Za-z0-9_.-]+)\s*=\s*\{(.*)\}\s*$')
CATALOG_GROUP = re.compile(r'\bgroup\s*=\s*"([^"]+)"')
CATALOG_NAME = re.compile(r'\bname\s*=\s*"([^"]+)"')
CATALOG_MODULE = re.compile(r'\bmodule\s*=\s*"([^"]+)"')


def _fdroid_runtime_aliases(build: str) -> tuple[list[tuple[int, str]], list[str]]:
    """Return packaged F-Droid aliases while ignoring the explicit non-F-Droid block."""
    aliases: list[tuple[int, str]] = []
    malformed: list[str] = []
    excluded_depth = 0

    for line_number, line in enumerate(build.splitlines(), start=1):
        if excluded_depth:
            excluded_depth += line.count("{") - line.count("}")
            continue

        if re.search(r"\bif\s*\(\s*!isFdroidBuild\s*\)\s*\{", line):
            excluded_depth = max(1, line.count("{") - line.count("}"))
            continue

        if not RUNTIME_DEPENDENCY_CALL.search(line):
            continue

        match = LIBS_ACCESSOR.search(line)
        if match:
            aliases.append((line_number, match.group(1)))
        else:
            malformed.append(
                f"line {line_number}: F-Droid runtime dependency must use a reviewed libs.* alias: {line.strip()}"
            )

    return aliases, malformed


def _catalog_coordinates(catalog: str) -> dict[str, str]:
    coordinates: dict[str, str] = {}
    in_libraries = False
    for raw_line in catalog.splitlines():
        line = raw_line.strip()
        if line == "[libraries]":
            in_libraries = True
            continue
        if line.startswith("[") and line.endswith("]"):
            in_libraries = False
            continue
        if not in_libraries or not line or line.startswith("#"):
            continue
        match = CATALOG_LIBRARY.match(line)
        if not match:
            continue
        alias, body = match.groups()
        module = CATALOG_MODULE.search(body)
        if module:
            coordinate = module.group(1)
        else:
            group = CATALOG_GROUP.search(body)
            name = CATALOG_NAME.search(body)
            if group is None or name is None:
                continue
            coordinate = f"{group.group(1)}:{name.group(1)}"
        coordinates[alias.replace("-", ".")] = coordinate
    return coordinates


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

    catalog_path = "gradle/libs.versions.toml"
    catalog = read(catalog_path)
    catalog_coordinates = _catalog_coordinates(catalog)
    runtime_aliases, malformed_dependencies = _fdroid_runtime_aliases(build)
    violations.extend(f"{build_path}: {message}" for message in malformed_dependencies)
    for line_number, alias in runtime_aliases:
        expected_coordinate = FDROID_RUNTIME_DEPENDENCIES.get(alias)
        if expected_coordinate is None:
            violations.append(
                f"{build_path}: line {line_number}: unreviewed F-Droid runtime dependency "
                f"libs.{alias}; review its license/network behavior and add it to "
                "FDROID_RUNTIME_DEPENDENCIES only when intentionally approved"
            )
            continue
        actual_coordinate = catalog_coordinates.get(alias)
        if actual_coordinate != expected_coordinate:
            violations.append(
                f"{catalog_path}: libs.{alias} must remain {expected_coordinate!r} for the "
                f"reviewed F-Droid build (found {actual_coordinate!r})"
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

    editorial_path = "app/src/main/java/com/luc4n3x/levyra/data/EditorialChartsRepository.kt"
    editorial = read(editorial_path)
    for marker in (
        '"https://raw.githubusercontent.com/LUC4N3X/Levyra-deepsound/editorial-data/catalog/editorial.json"',
        'host == "i.scdn.co"',
        'host.endsWith(".scdn.co")',
        'host == "image-cdn-ak.spotifycdn.com"',
    ):
        require(
            editorial_path,
            editorial,
            marker,
            "the reviewed editorial network host contract changed; update F-Droid disclosure and review the trust boundary",
        )

    descriptions = {
        "fastlane/metadata/android/en-US/full_description.txt": (
            "YouTube",
            "Apple Music/iTunes",
            "Deezer",
            "Tidal",
            "Qobuz",
            "Wikidata",
            "Spotify",
            "scdn.co",
            "spotifycdn.com",
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
            "Spotify",
            "scdn.co",
            "spotifycdn.com",
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
