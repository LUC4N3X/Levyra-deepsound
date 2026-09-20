import json
import os
import sys
import tempfile
import unittest
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parents[1]
if str(SCRIPTS) not in sys.path:
    sys.path.insert(0, str(SCRIPTS))

import player_config_pipeline as pipeline  # noqa: E402
import sync_player_configs as sync  # noqa: E402

NOW = "2026-09-20T00:00:00Z"
ZEMER_URL = pipeline.ZEMER_CONFIG_URL
ZEMER_DATES_URL = pipeline.ZEMER_DATES_URL
FARADAY_URL = pipeline.FARADAY_CONFIG_URL
FARADAY_REGISTRY_URL = pipeline.FARADAY_REGISTRY_URL


def entry(sts, n_class="Yx", aliases=(), sig="Ab(1,2,INPUT)"):
    return {"sig": sig, "nClass": n_class, "sts": sts, "aliases": list(aliases)}


def registry(players):
    return json.dumps({"schemaVersion": 1, "players": players}).encode("utf-8")


def outcome(url, body=b"", status=200, error=None):
    return pipeline.FetchOutcome(url, status, body, error, NOW)


class FakeNetwork:
    def __init__(self, **routes):
        self.routes = routes

    def __call__(self, url):
        if url not in self.routes:
            return outcome(url, error="offline")
        value = self.routes[url]
        if isinstance(value, pipeline.FetchOutcome):
            return value
        if isinstance(value, tuple):
            status, body = value
            return outcome(url, body=body, status=status)
        return outcome(url, body=value)


class PipelineTestBase(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.assets = Path(self.tmp.name) / "assets"
        self.assets.mkdir(parents=True, exist_ok=True)

    def tearDown(self):
        self.tmp.cleanup()

    def seed(self, players, dates=None):
        (self.assets / sync.PLAYER_CONFIGS_ASSET).write_text(
            pipeline.render_registry(players, tuple(players.keys())), encoding="utf-8"
        )
        if dates is not None:
            (self.assets / sync.PLAYER_DATES_ASSET).write_text(
                json.dumps(dates, indent=2) + "\n", encoding="utf-8"
            )

    def run_pipeline(self, network, sources=None):
        return sync.run_pipeline(
            sources=sources or (pipeline.ZemerSource(), pipeline.FaradaySource()),
            fetch=network,
            assets_dir=self.assets,
            generated_at=NOW,
        )

    def asset_player_bytes(self):
        return (self.assets / sync.PLAYER_CONFIGS_ASSET).read_bytes()

    def temp_files(self):
        return sorted(
            p.name
            for p in self.assets.iterdir()
            if p.name.endswith(".tmp") or p.name.endswith(".bak")
        )


class SelectionTest(PipelineTestBase):
    def test_both_sources_valid_and_matching(self):
        (self.assets / sync.PLAYER_CONFIGS_ASSET).write_text(
            pipeline.render_registry({}, ()), encoding="utf-8"
        )
        network = FakeNetwork(**{
            ZEMER_URL: registry({"aaaaaaaa": entry(100)}),
            FARADAY_URL: registry({"aaaaaaaa": entry(100)}),
        })
        report, snapshots = self.run_pipeline(network)
        self.assertEqual(pipeline.SELECTION_DUAL_SOURCE_HEALTHY, report.selection)
        self.assertEqual(1, report.counts.get(pipeline.VERDICT_CONFIRMED))
        self.assertEqual(0, len(report.conflicts))
        self.assertEqual(100, pipeline.load_registry_text(
            (self.assets / sync.PLAYER_CONFIGS_ASSET).read_text(encoding="utf-8")
        )["aaaaaaaa"].signature_timestamp)

    def test_primary_valid_secondary_unavailable(self):
        network = FakeNetwork(**{ZEMER_URL: registry({"aaaaaaaa": entry(100)})})
        report, _ = self.run_pipeline(network)
        self.assertEqual(pipeline.SELECTION_PRIMARY, report.selection)
        self.assertEqual("unavailable", report.source_health["faraday"]["status"])
        self.assertTrue(report.changed)

    def test_primary_unavailable_secondary_valid(self):
        network = FakeNetwork(**{FARADAY_URL: registry({"aaaaaaaa": entry(100)})})
        report, _ = self.run_pipeline(network)
        self.assertEqual(pipeline.SELECTION_SECONDARY, report.selection)
        self.assertEqual("unavailable", report.source_health["zemer"]["status"])
        players = pipeline.load_registry_text(
            (self.assets / sync.PLAYER_CONFIGS_ASSET).read_text(encoding="utf-8")
        )
        self.assertEqual(100, players["aaaaaaaa"].signature_timestamp)

    def test_primary_invalid_secondary_valid(self):
        network = FakeNetwork(**{
            ZEMER_URL: b"<html>rate limited</html>",
            FARADAY_URL: registry({"aaaaaaaa": entry(100)}),
        })
        report, _ = self.run_pipeline(network)
        self.assertEqual("invalid", report.source_health["zemer"]["status"])
        self.assertEqual(pipeline.SELECTION_SECONDARY, report.selection)
        self.assertTrue(report.changed)

    def test_both_invalid_keeps_last_known_good_and_assets(self):
        seeded = {"aaaaaaaa": pipeline.PlayerConfig("aaaaaaaa", "Ab(1,2,INPUT)", "Yx", 100)}
        self.seed(seeded)
        before = self.asset_player_bytes()
        network = FakeNetwork(**{
            ZEMER_URL: b"not json",
            FARADAY_URL: b"<html>down</html>",
        })
        report, _ = self.run_pipeline(network)
        self.assertEqual(pipeline.DECISION_KEEP_LAST_KNOWN_GOOD, report.decision)
        self.assertFalse(report.changed)
        self.assertEqual(before, self.asset_player_bytes())
        self.assertEqual(pipeline.SELECTION_LAST_KNOWN_GOOD, report.selection)

    def test_both_valid_but_conflicting_keeps_last_known_good(self):
        seeded = {"aaaaaaaa": pipeline.PlayerConfig("aaaaaaaa", "Ab(1,2,INPUT)", "Yx", 100)}
        self.seed(seeded)
        network = FakeNetwork(**{
            ZEMER_URL: registry({"aaaaaaaa": entry(100, n_class="Yx")}),
            FARADAY_URL: registry({"aaaaaaaa": entry(100, n_class="Zz")}),
        })
        report, _ = self.run_pipeline(network)
        self.assertEqual(pipeline.SELECTION_MIXED, report.selection)
        self.assertEqual(("aaaaaaaa",), report.conflicts)
        self.assertFalse(report.changed)
        players = pipeline.load_registry_text(
            (self.assets / sync.PLAYER_CONFIGS_ASSET).read_text(encoding="utf-8")
        )
        self.assertEqual("Yx", players["aaaaaaaa"].n_class)

    def test_conflict_without_last_known_good_omits_entry(self):
        network = FakeNetwork(**{
            ZEMER_URL: registry({"aaaaaaaa": entry(100, n_class="Yx")}),
            FARADAY_URL: registry({"aaaaaaaa": entry(100, n_class="Zz")}),
        })
        report, _ = self.run_pipeline(network)
        self.assertEqual(pipeline.SELECTION_MIXED, report.selection)
        self.assertEqual(pipeline.DECISION_KEEP_LAST_KNOWN_GOOD, report.decision)
        self.assertFalse((self.assets / sync.PLAYER_CONFIGS_ASSET).exists())

    def test_newer_invalid_candidate_keeps_last_known_good(self):
        seeded = {"aaaaaaaa": pipeline.PlayerConfig("aaaaaaaa", "Ab(1,2,INPUT)", "Yx", 100)}
        self.seed(seeded)
        before = self.asset_player_bytes()
        bad_entry = {"sig": "not-a-signature", "nClass": "Yx", "sts": 99999, "aliases": []}
        network = FakeNetwork(**{
            ZEMER_URL: registry({"dddddddd": bad_entry}),
        })
        report, _ = self.run_pipeline(network)
        self.assertEqual("invalid", report.source_health["zemer"]["status"])
        self.assertEqual(pipeline.DECISION_KEEP_LAST_KNOWN_GOOD, report.decision)
        self.assertEqual(before, self.asset_player_bytes())

    def test_primary_healthy_does_not_publish_secondary_only_player(self):
        self.seed({"aaaaaaaa": pipeline.PlayerConfig("aaaaaaaa", "Ab(1,2,INPUT)", "Yx", 100)})
        network = FakeNetwork(**{
            ZEMER_URL: registry({"aaaaaaaa": entry(100)}),
            FARADAY_URL: registry({"bbbbbbbb": entry(200)}),
        })
        report, _ = self.run_pipeline(network)
        self.assertEqual(pipeline.SELECTION_DUAL_SOURCE_HEALTHY, report.selection)
        self.assertEqual(1, report.counts.get(pipeline.VERDICT_CANDIDATE))
        players = pipeline.load_registry_text(
            (self.assets / sync.PLAYER_CONFIGS_ASSET).read_text(encoding="utf-8")
        )
        self.assertNotIn("bbbbbbbb", players)
        self.assertIn("aaaaaaaa", players)

    def test_primary_healthy_keeps_last_known_good_secondary_only_player(self):
        seeded = {
            "aaaaaaaa": pipeline.PlayerConfig("aaaaaaaa", "Ab(1,2,INPUT)", "Yx", 100),
            "bbbbbbbb": pipeline.PlayerConfig("bbbbbbbb", "Cd(3,4,INPUT)", "Zz", 200),
        }
        self.seed(seeded)
        network = FakeNetwork(**{
            ZEMER_URL: registry({"aaaaaaaa": entry(100)}),
            FARADAY_URL: registry({"bbbbbbbb": entry(200, n_class="Zz", sig="Cd(3,4,INPUT)")}),
        })
        report, _ = self.run_pipeline(network)
        self.assertEqual(1, report.counts.get(pipeline.VERDICT_LAST_KNOWN_GOOD))
        players = pipeline.load_registry_text(
            (self.assets / sync.PLAYER_CONFIGS_ASSET).read_text(encoding="utf-8")
        )
        self.assertIn("bbbbbbbb", players)

    def test_cross_source_alias_identity_is_confirmed(self):
        network = FakeNetwork(**{
            ZEMER_URL: registry({"aaaaaaaa": entry(100, aliases=("bbbbbbbb",))}),
            FARADAY_URL: registry({"bbbbbbbb": entry(100, aliases=("aaaaaaaa",))}),
        })
        report, _ = self.run_pipeline(network)
        self.assertEqual(pipeline.SELECTION_DUAL_SOURCE_HEALTHY, report.selection)
        self.assertEqual(1, report.counts.get(pipeline.VERDICT_CONFIRMED))
        self.assertEqual(0, report.counts.get(pipeline.VERDICT_SINGLE_PRIMARY, 0))
        self.assertEqual(0, report.counts.get(pipeline.VERDICT_SINGLE_SECONDARY, 0))
        players = pipeline.load_registry_text(
            (self.assets / sync.PLAYER_CONFIGS_ASSET).read_text(encoding="utf-8")
        )
        self.assertEqual(["aaaaaaaa"], list(players.keys()))
        self.assertEqual(("bbbbbbbb",), players["aaaaaaaa"].aliases)

    def test_cross_source_alias_conflict_keeps_last_known_good(self):
        seeded = {
            "aaaaaaaa": pipeline.PlayerConfig("aaaaaaaa", "Ab(1,2,INPUT)", "Yx", 100, ("bbbbbbbb",))
        }
        self.seed(seeded)
        network = FakeNetwork(**{
            ZEMER_URL: registry({"aaaaaaaa": entry(100, n_class="Yx", aliases=("bbbbbbbb",))}),
            FARADAY_URL: registry({"bbbbbbbb": entry(100, n_class="Zz", aliases=("aaaaaaaa",))}),
        })
        report, _ = self.run_pipeline(network)
        self.assertEqual(pipeline.SELECTION_MIXED, report.selection)
        self.assertEqual(1, len(report.conflicts))
        players = pipeline.load_registry_text(
            (self.assets / sync.PLAYER_CONFIGS_ASSET).read_text(encoding="utf-8")
        )
        self.assertEqual("Yx", players["aaaaaaaa"].n_class)

    def test_secondary_only_entry_cannot_evict_last_known_good_identity(self):
        seeded = {
            "aaaaaaaa": pipeline.PlayerConfig("aaaaaaaa", "Ab(1,2,INPUT)", "Yx", 100, ("bbbbbbbb",))
        }
        self.seed(seeded)
        network = FakeNetwork(**{
            ZEMER_URL: registry({"cccccccc": entry(300)}),
            FARADAY_URL: registry({"bbbbbbbb": entry(200, n_class="Zz")}),
        })
        report, _ = self.run_pipeline(network)
        players = pipeline.load_registry_text(
            (self.assets / sync.PLAYER_CONFIGS_ASSET).read_text(encoding="utf-8")
        )
        self.assertEqual("Yx", players["aaaaaaaa"].n_class)
        self.assertEqual(("bbbbbbbb",), players["aaaaaaaa"].aliases)
        self.assertNotIn("bbbbbbbb", players)
        self.assertIn("cccccccc", players)

    def test_twoLkgEntriesLinkedByNewUpstreamAliasAreBothPreserved(self):
        seeded = {
            "aaaaaaaa": pipeline.PlayerConfig("aaaaaaaa", "Ab(1,2,INPUT)", "Yx", 100, ()),
            "bbbbbbbb": pipeline.PlayerConfig("bbbbbbbb", "Cd(3,4,INPUT)", "Zz", 200, ()),
        }
        self.seed(seeded)
        network = FakeNetwork(**{
            ZEMER_URL: registry({"aaaaaaaa": entry(100, aliases=("bbbbbbbb",))}),
        })
        report, _ = self.run_pipeline(network)
        self.assertFalse(report.changed)
        self.assertEqual(2, report.counts.get(pipeline.VERDICT_LAST_KNOWN_GOOD))
        players = pipeline.load_registry_text(
            (self.assets / sync.PLAYER_CONFIGS_ASSET).read_text(encoding="utf-8")
        )
        self.assertIn("aaaaaaaa", players)
        self.assertIn("bbbbbbbb", players)
        self.assertEqual((), players["aaaaaaaa"].aliases)
        self.assertEqual((), players["bbbbbbbb"].aliases)
        self.assertEqual(200, players["bbbbbbbb"].signature_timestamp)

    def test_ambiguousClusterDoesNotMutateExistingAliases(self):
        seeded = {
            "aaaaaaaa": pipeline.PlayerConfig("aaaaaaaa", "Ab(1,2,INPUT)", "Yx", 100, ("eeeeeeee",)),
            "bbbbbbbb": pipeline.PlayerConfig("bbbbbbbb", "Cd(3,4,INPUT)", "Zz", 200, ()),
        }
        self.seed(seeded)
        network = FakeNetwork(**{
            ZEMER_URL: registry({"aaaaaaaa": entry(100, aliases=("bbbbbbbb",))}),
        })
        report, _ = self.run_pipeline(network)
        self.assertFalse(report.changed)
        players = pipeline.load_registry_text(
            (self.assets / sync.PLAYER_CONFIGS_ASSET).read_text(encoding="utf-8")
        )
        self.assertEqual(("eeeeeeee",), players["aaaaaaaa"].aliases)
        self.assertEqual((), players["bbbbbbbb"].aliases)

    def test_ambiguousClusterWithoutLkgPublishesNothing(self):
        network = FakeNetwork(**{
            ZEMER_URL: registry({
                "aaaaaaaa": entry(100, aliases=("cccccccc",)),
                "bbbbbbbb": entry(200, aliases=("dddddddd",)),
            }),
            FARADAY_URL: registry({
                "cccccccc": entry(100, aliases=("bbbbbbbb",)),
            }),
        })
        report, _ = self.run_pipeline(network)
        self.assertEqual(pipeline.SELECTION_MIXED, report.selection)
        self.assertEqual(pipeline.DECISION_KEEP_LAST_KNOWN_GOOD, report.decision)
        self.assertFalse((self.assets / sync.PLAYER_CONFIGS_ASSET).exists())

    def test_canonicalAliasOverflowPreservesLastKnownGood(self):
        seeded = {"aaaaaaaa": pipeline.PlayerConfig("aaaaaaaa", "Ab(1,2,INPUT)", "Yx", 100, ())}
        self.seed(seeded)
        network = FakeNetwork(**self._overflow_network())
        report, _ = self.run_pipeline(network)
        self.assertEqual(1, report.counts.get(pipeline.VERDICT_LAST_KNOWN_GOOD))
        self.assertIn("aaaaaaaa", report.conflicts)
        players = pipeline.load_registry_text(
            (self.assets / sync.PLAYER_CONFIGS_ASSET).read_text(encoding="utf-8")
        )
        self.assertIn("aaaaaaaa", players)
        self.assertEqual((), players["aaaaaaaa"].aliases)

    def test_canonicalAliasOverflowWithoutLkgOmitsEntry(self):
        network = FakeNetwork(**self._overflow_network())
        report, _ = self.run_pipeline(network)
        self.assertEqual(pipeline.DECISION_KEEP_LAST_KNOWN_GOOD, report.decision)
        self.assertFalse((self.assets / sync.PLAYER_CONFIGS_ASSET).exists())

    def test_canonicalAliasOverflowWithDifferentLkgPrimaryHashPreservesLkg(self):
        seeded = {
            "bbbbbbbb": pipeline.PlayerConfig("bbbbbbbb", "Ab(1,2,INPUT)", "Yx", 100, ("cccccccc",)),
        }
        self.seed(seeded)
        zemer_aliases = tuple(f"{index:08x}" for index in range(1, 16)) + ("bbbbbbbb",)
        faraday_aliases = ("fffffff0", "fffffff1")
        network = FakeNetwork(**{
            ZEMER_URL: registry({"aaaaaaaa": entry(100, aliases=zemer_aliases)}),
            FARADAY_URL: registry({"aaaaaaaa": entry(100, aliases=faraday_aliases)}),
        })
        report, _ = self.run_pipeline(network)
        self.assertFalse(report.changed)
        self.assertEqual(pipeline.DECISION_KEEP_CURRENT, report.decision)
        self.assertEqual(1, report.counts.get(pipeline.VERDICT_LAST_KNOWN_GOOD))
        self.assertIn("bbbbbbbb", report.conflicts)
        players = pipeline.load_registry_text(
            (self.assets / sync.PLAYER_CONFIGS_ASSET).read_text(encoding="utf-8")
        )
        self.assertEqual(["bbbbbbbb"], list(players.keys()))
        self.assertNotIn("aaaaaaaa", players)
        self.assertEqual(("cccccccc",), players["bbbbbbbb"].aliases)
        self.assertEqual(100, players["bbbbbbbb"].signature_timestamp)

    @staticmethod
    def _overflow_network():
        zemer_aliases = tuple(f"{index:08x}" for index in range(1, 17))
        faraday_aliases = tuple(f"{index:08x}" for index in range(17, 19))
        return {
            ZEMER_URL: registry({"aaaaaaaa": entry(100, aliases=zemer_aliases)}),
            FARADAY_URL: registry({"aaaaaaaa": entry(100, aliases=faraday_aliases)}),
        }


class UnchangedAndAtomicTest(PipelineTestBase):
    def test_unchanged_trusted_config_is_not_committed(self):
        network = FakeNetwork(**{
            ZEMER_URL: registry({"aaaaaaaa": entry(100)}),
            FARADAY_URL: registry({"aaaaaaaa": entry(100)}),
        })
        first, _ = self.run_pipeline(network)
        self.assertTrue(first.changed)
        after_first = self.asset_player_bytes()
        meta_after_first = (self.assets / sync.PLAYER_META_ASSET).read_bytes()

        second, _ = self.run_pipeline(network)
        self.assertFalse(second.changed)
        self.assertEqual(pipeline.DECISION_KEEP_CURRENT, second.decision)
        self.assertEqual(after_first, self.asset_player_bytes())
        self.assertEqual(meta_after_first, (self.assets / sync.PLAYER_META_ASSET).read_bytes())
        self.assertEqual([], self.temp_files())

    def test_atomic_publish_failure_preserves_original(self):
        target = self.assets / "atomic.txt"
        target.write_text("original", encoding="utf-8")
        original_replace = os.replace

        def failing_replace(source, destination):
            raise OSError("simulated replace failure")

        os.replace = failing_replace
        try:
            with self.assertRaises(sync.PipelineError):
                sync._publish_atomically(target, "replacement")
        finally:
            os.replace = original_replace
        self.assertEqual("original", target.read_text(encoding="utf-8"))
        self.assertEqual([], self.temp_files())

    def test_invalid_refresh_never_touches_dates_or_players(self):
        players = {"aaaaaaaa": pipeline.PlayerConfig("aaaaaaaa", "Ab(1,2,INPUT)", "Yx", 100)}
        self.seed(players, dates={"aaaaaaaa": "2026-01-01"})
        before_players = self.asset_player_bytes()
        before_dates = (self.assets / sync.PLAYER_DATES_ASSET).read_bytes()
        network = FakeNetwork(**{ZEMER_URL: b"{\"schemaVersion\":1,\"players\":{"})
        report, _ = self.run_pipeline(network)
        self.assertFalse(report.changed)
        self.assertEqual(before_players, self.asset_player_bytes())
        self.assertEqual(before_dates, (self.assets / sync.PLAYER_DATES_ASSET).read_bytes())


class AssetTransactionTest(PipelineTestBase):
    def setUp(self):
        super().setUp()
        players = {"aaaaaaaa": pipeline.PlayerConfig("aaaaaaaa", "Ab(1,2,INPUT)", "Yx", 100)}
        self.seed(players, dates={"aaaaaaaa": "2026-01-01"})
        self.before_players = (self.assets / sync.PLAYER_CONFIGS_ASSET).read_bytes()
        self.before_dates = (self.assets / sync.PLAYER_DATES_ASSET).read_bytes()
        new_players = {"bbbbbbbb": pipeline.PlayerConfig("bbbbbbbb", "Cd(3,4,INPUT)", "Zz", 200)}
        self.updates = {
            sync.PLAYER_CONFIGS_ASSET: pipeline.render_registry(new_players, ("bbbbbbbb",)),
            sync.PLAYER_DATES_ASSET: json.dumps({"aaaaaaaa": "2026-02-02"}) + "\n",
            sync.PLAYER_META_ASSET: json.dumps({"schema": 1, "source": "TEST"}) + "\n",
        }

    def test_commit_failure_restores_all_assets(self):
        for fail_at in (0, 1, 2):
            with self.subTest(fail_at=fail_at):
                with self.assertRaises(sync.PipelineError):
                    sync._publish_generation(self.assets, self.updates, fail_after_commits=fail_at)
                self.assertEqual(self.before_players, (self.assets / sync.PLAYER_CONFIGS_ASSET).read_bytes())
                self.assertEqual(self.before_dates, (self.assets / sync.PLAYER_DATES_ASSET).read_bytes())
                self.assertFalse((self.assets / sync.PLAYER_META_ASSET).exists())
                self.assertEqual([], self.temp_files())

    def test_generation_commit_succeeds_atomically(self):
        sync._publish_generation(self.assets, self.updates)
        self.assertEqual(
            self.updates[sync.PLAYER_CONFIGS_ASSET],
            (self.assets / sync.PLAYER_CONFIGS_ASSET).read_text(encoding="utf-8"),
        )
        self.assertEqual(
            self.updates[sync.PLAYER_DATES_ASSET],
            (self.assets / sync.PLAYER_DATES_ASSET).read_text(encoding="utf-8"),
        )
        self.assertEqual(
            self.updates[sync.PLAYER_META_ASSET],
            (self.assets / sync.PLAYER_META_ASSET).read_text(encoding="utf-8"),
        )
        self.assertEqual([], self.temp_files())

    def test_stagingFailureLeavesAssetsUntouchedAndNoTempFiles(self):
        before_players = (self.assets / sync.PLAYER_CONFIGS_ASSET).read_bytes()
        before_dates = (self.assets / sync.PLAYER_DATES_ASSET).read_bytes()
        original_write = sync._write_file_lf
        calls = {"count": 0}

        def failing_write(path, text):
            calls["count"] += 1
            if calls["count"] >= 2:
                raise OSError("simulated staging write failure")
            return original_write(path, text)

        sync._write_file_lf = failing_write
        try:
            with self.assertRaises(sync.PipelineError):
                sync._publish_generation(self.assets, self.updates)
        finally:
            sync._write_file_lf = original_write

        self.assertEqual(before_players, (self.assets / sync.PLAYER_CONFIGS_ASSET).read_bytes())
        self.assertEqual(before_dates, (self.assets / sync.PLAYER_DATES_ASSET).read_bytes())
        self.assertFalse((self.assets / sync.PLAYER_META_ASSET).exists())
        self.assertEqual([], self.temp_files())


class ValidationTest(unittest.TestCase):
    def test_corrupted_and_truncated_json_is_rejected(self):
        with self.assertRaises(pipeline.SourceRejected):
            pipeline.parse_players_registry(b'{"schemaVersion":1,"players":{"aaaaaaaa":{"sig":"Ab(1,2,INPUT)"')
        with self.assertRaises(pipeline.SourceRejected):
            pipeline.parse_players_registry(b"")

    def test_html_error_page_is_rejected(self):
        with self.assertRaises(pipeline.SourceRejected):
            pipeline.parse_players_registry(b"<!DOCTYPE html><html>access denied</html>")

    def test_oversized_payload_is_rejected(self):
        with self.assertRaises(pipeline.SourceRejected):
            pipeline.decode_json_object(b"x" * (pipeline.MAX_SOURCE_BYTES + 1))

    def test_root_array_is_rejected(self):
        with self.assertRaises(pipeline.SourceRejected):
            pipeline.decode_json_object(b'[{"schemaVersion": 1}]')

    def test_wrong_schema_is_rejected(self):
        body = json.dumps({"schemaVersion": 2, "players": {"aaaaaaaa": entry(100)}}).encode()
        with self.assertRaises(pipeline.SourceRejected):
            pipeline.parse_players_registry(body)

    def test_empty_players_is_rejected(self):
        with self.assertRaises(pipeline.SourceRejected):
            pipeline.parse_players_registry(registry({}))

    def test_invalid_identifiers_and_values_are_rejected(self):
        cases = [
            {"NOT-A-HASH": entry(100)},
            {"aaaaaaaa": entry(0)},
            {"aaaaaaaa": entry(100, n_class="way-too-long")},
            {"aaaaaaaa": {"sig": "bad", "nClass": "Yx", "sts": 100, "aliases": []}},
            {"aaaaaaaa": {"sig": "Ab(1,2,INPUT)", "nClass": "Yx", "sts": "100", "aliases": []}},
            {"aaaaaaaa": {"sig": "Ab(1,2,INPUT)", "nClass": "Yx", "sts": 100, "aliases": ["zz"]}},
            {"aaaaaaaa": {"sig": "Ab(1,2,INPUT)", "nClass": "Yx", "sts": 100, "aliases": ["aaaaaaaa"]}},
        ]
        for players in cases:
            with self.subTest(players=players):
                with self.assertRaises(pipeline.SourceRejected):
                    pipeline.parse_players_registry(registry(players))

    def test_duplicate_hash_alias_collision_is_rejected(self):
        players = {
            "aaaaaaaa": entry(100, aliases=("bbbbbbbb",)),
            "bbbbbbbb": entry(200),
        }
        with self.assertRaises(pipeline.SourceRejected):
            pipeline.parse_players_registry(registry(players))

    def test_metadata_validation_rejects_bad_dates(self):
        source = pipeline.ZemerSource()
        with self.assertRaises(pipeline.SourceRejected):
            source.parse_metadata({"player_dates": json.dumps({"aaaaaaaa": "not-a-date"}).encode()})
        with self.assertRaises(pipeline.SourceRejected):
            source.parse_metadata({"player_dates": json.dumps({"zz": "2026-01-01"}).encode()})

    def test_faraday_metadata_accepts_valid_registry(self):
        source = pipeline.FaradaySource()
        body = json.dumps({
            "updatedAt": "2026-09-19T05:07:21Z",
            "players": [{"playerHash": "aaaaaaaa", "status": "validated"}],
            "current": {"playerHash": "aaaaaaaa"},
        }).encode()
        parsed = source.parse_metadata({"player_registry": body})
        self.assertEqual("aaaaaaaa", parsed["currentPlayerHash"])
        self.assertIn("aaaaaaaa", parsed["knownPlayers"])

    def test_faraday_metadata_rejects_malformed_registry(self):
        source = pipeline.FaradaySource()
        cases = {
            "players missing": json.dumps({"updatedAt": "x"}).encode(),
            "players object": json.dumps({"players": {}}).encode(),
            "players strings": json.dumps({"players": ["aaaaaaaa"]}).encode(),
            "players nulls": json.dumps({"players": [None]}).encode(),
            "missing playerHash": json.dumps({"players": [{}]}).encode(),
            "invalid playerHash": json.dumps({"players": [{"playerHash": "zz"}]}).encode(),
        }
        for label, body in cases.items():
            with self.subTest(label=label):
                with self.assertRaises(pipeline.SourceRejected):
                    source.parse_metadata({"player_registry": body})

    def test_malformed_faraday_metadata_keeps_config_healthy(self):
        source = pipeline.FaradaySource()
        config = outcome(FARADAY_URL, registry({"aaaaaaaa": entry(100)}))
        malformed = outcome(FARADAY_REGISTRY_URL, json.dumps({"players": "not-a-list"}).encode())
        snapshot = source.snapshot(config, {"player_registry": malformed}, NOW)
        self.assertEqual(pipeline.STATUS_OK, snapshot.status)
        self.assertEqual(1, len(snapshot.players))
        warnings = snapshot.metadata.get("warnings", [])
        self.assertTrue(any("metadata ignored" in warning for warning in warnings))


class SerializationTest(unittest.TestCase):
    def test_render_registry_round_trips(self):
        players = {
            "aaaaaaaa": pipeline.PlayerConfig("aaaaaaaa", "Ab(1,2,INPUT)", "Yx", 100, ("bbbbbbbb",)),
            "cccccccc": pipeline.PlayerConfig("cccccccc", "Cd(3,4,INPUT)", "Zz", 200),
        }
        text = pipeline.render_registry(players, ("cccccccc", "aaaaaaaa"))
        parsed = pipeline.parse_players_registry(text.encode("utf-8"))
        self.assertTrue(pipeline.configurations_equal(players, parsed))
        self.assertTrue(text.endswith("}\n"))

    def test_health_snapshot_contains_no_url_or_body(self):
        source = pipeline.FaradaySource()
        snapshot = source.snapshot(outcome(FARADAY_URL, registry({"aaaaaaaa": entry(100)})), {}, NOW)
        health = json.dumps(snapshot.health())
        self.assertNotIn("http", health)
        self.assertNotIn("sig", health)


if __name__ == "__main__":
    unittest.main()
