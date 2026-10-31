from __future__ import annotations

import importlib.util
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MODULE_PATH = ROOT / "scripts" / "validate_fdroid_review_contract.py"
SPEC = importlib.util.spec_from_file_location("validate_fdroid_review_contract", MODULE_PATH)
assert SPEC is not None and SPEC.loader is not None
contract = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(contract)


class FdroidReviewContractTest(unittest.TestCase):
    def source(self, relative: str) -> str:
        return (ROOT / relative).read_text(encoding="utf-8")

    def violations_after(self, relative: str, mutated: str) -> list[str]:
        return contract.collect_violations(ROOT, {relative: mutated})

    def test_current_repository_satisfies_the_contract(self) -> None:
        self.assertEqual([], contract.collect_violations(ROOT))

    def test_downloads_fallback_is_rejected(self) -> None:
        relative = (
            "app/src/main/java/com/luc4n3x/levyra/player/offline/"
            "OfflineAudioExporter.kt"
        )
        violations = self.violations_after(
            relative,
            self.source(relative) + "\n// MediaStore.Downloads\n",
        )
        self.assertTrue(any("must not silently fall back" in item for item in violations))

    def test_remote_announcements_cannot_be_enabled_for_fdroid(self) -> None:
        relative = "app/build.gradle.kts"
        mutated = self.source(relative).replace(
            'buildConfigField("boolean", "REMOTE_ANNOUNCEMENTS_ENABLED", (!isFdroidBuild).toString())',
            'buildConfigField("boolean", "REMOTE_ANNOUNCEMENTS_ENABLED", "true")',
        )
        violations = self.violations_after(relative, mutated)
        self.assertTrue(any("disable remote announcements" in item for item in violations))

    def test_new_fdroid_runtime_dependency_requires_review(self) -> None:
        relative = "app/build.gradle.kts"
        mutated = self.source(relative).replace(
            "    implementation(libs.timber)\n",
            "    implementation(libs.timber)\n    implementation(libs.leakcanary.android)\n",
        )
        violations = self.violations_after(relative, mutated)
        self.assertTrue(any("unreviewed F-Droid runtime dependency" in item for item in violations))

    def test_reviewed_dependency_coordinate_cannot_be_repointed(self) -> None:
        relative = "gradle/libs.versions.toml"
        mutated = self.source(relative).replace(
            'okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }',
            'okhttp = { group = "example.closed", name = "replacement", version.ref = "okhttp" }',
        )
        violations = self.violations_after(relative, mutated)
        self.assertTrue(any("libs.okhttp must remain" in item for item in violations))

    def test_non_fdroid_cronet_dependencies_stay_excluded_from_runtime_review(self) -> None:
        relative = "app/build.gradle.kts"
        aliases, malformed = contract._fdroid_runtime_aliases(self.source(relative))
        alias_names = {alias for _, alias in aliases}
        self.assertEqual([], malformed)
        self.assertNotIn("androidx.media3.datasource.cronet", alias_names)
        self.assertNotIn("chromium.cronet.embedded", alias_names)

    def test_hardcoded_runtime_message_is_rejected(self) -> None:
        relative = (
            "app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt"
        )
        violations = self.violations_after(
            relative,
            self.source(relative) + '\n// offlineExportMessage = "Salvato in Music"\n',
        )
        self.assertTrue(any("must use LevyraStrings" in item for item in violations))

    def test_missing_network_disclosure_is_rejected(self) -> None:
        relative = "fastlane/metadata/android/en-US/full_description.txt"
        mutated = self.source(relative).replace("SponsorBlock", "segment service")
        violations = self.violations_after(relative, mutated)
        self.assertTrue(any("network behavior" in item for item in violations))

    def test_spotify_artwork_disclosure_is_required(self) -> None:
        relative = "fastlane/metadata/android/en-US/full_description.txt"
        mutated = self.source(relative).replace("spotifycdn.com", "image host")
        violations = self.violations_after(relative, mutated)
        self.assertTrue(any("network behavior" in item for item in violations))

    def test_editorial_artwork_host_contract_cannot_drift_silently(self) -> None:
        relative = "app/src/main/java/com/luc4n3x/levyra/data/EditorialChartsRepository.kt"
        mutated = self.source(relative).replace(
            'host == "image-cdn-ak.spotifycdn.com"',
            'host == "images.example.invalid"',
        )
        violations = self.violations_after(relative, mutated)
        self.assertTrue(any("editorial network host contract changed" in item for item in violations))

    def test_removing_the_pr_gate_is_rejected(self) -> None:
        relative = ".github/workflows/pr-check.yml"
        mutated = self.source(relative).replace(
            "python3 scripts/validate_fdroid_review_contract.py",
            "echo skipped",
        )
        violations = self.violations_after(relative, mutated)
        self.assertTrue(any("blocking CI/release gate" in item for item in violations))


if __name__ == "__main__":
    unittest.main()
