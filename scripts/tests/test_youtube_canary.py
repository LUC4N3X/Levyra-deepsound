import importlib.util
import json
import tempfile
import unittest
import sys
from pathlib import Path

SCRIPT = Path(__file__).resolve().parents[1] / "youtube_canary.py"
SPEC = importlib.util.spec_from_file_location("youtube_canary", SCRIPT)
canary = importlib.util.module_from_spec(SPEC)
assert SPEC and SPEC.loader
sys.modules["youtube_canary"] = canary
SPEC.loader.exec_module(canary)


class YoutubeCanaryTest(unittest.TestCase):
    def test_extract_balanced_json_handles_nested_strings(self):
        text = 'before ytcfg.set({"a":{"b":"} still string"},"c":1}); after'
        start = text.index("ytcfg.set(") + len("ytcfg.set(")
        self.assertEqual({"a": {"b": "} still string"}, "c": 1}, canary._extract_balanced_json(text, start))

    def test_extract_ytcfg_merges_and_falls_back(self):
        html = (
            'ytcfg.set({"INNERTUBE_CLIENT_VERSION":"1.2.3"});'
            'window.x={"INNERTUBE_API_KEY":"abc","VISITOR_DATA":"visitor",'
            '"PLAYER_JS_URL":"\\/s\\/player\\/hash\\/base.js"};'
        )
        config = canary._extract_ytcfg(html)
        self.assertEqual("1.2.3", config["INNERTUBE_CLIENT_VERSION"])
        self.assertEqual("abc", config["INNERTUBE_API_KEY"])
        self.assertEqual("visitor", config["VISITOR_DATA"])
        self.assertEqual("/s/player/hash/base.js", config["PLAYER_JS_URL"])

    def test_summarize_player_response_never_returns_media_url(self):
        player = {
            "playabilityStatus": {"status": "OK"},
            "streamingData": {
                "formats": [
                    {
                        "url": "https://r1---sn.googlevideo.com/videoplayback?n=secret&sig=hidden",
                        "mimeType": "video/mp4",
                    }
                ],
                "adaptiveFormats": [
                    {
                        "signatureCipher": "url=https%3A%2F%2Fr2---sn.googlevideo.com%2Fvideoplayback%3Fn%3Dx&s=abc",
                        "mimeType": "audio/webm",
                    }
                ],
            },
        }
        summary = canary._summarize_player_response(player)
        self.assertEqual(2, summary["total_formats"])
        self.assertEqual(1, summary["muxed_video_formats"])
        self.assertEqual(0, summary["adaptive_video_formats"])
        self.assertEqual(1, summary["adaptive_audio_formats"])
        self.assertEqual(1, summary["direct_urls"])
        self.assertEqual(1, summary["cipher_urls"])
        self.assertEqual(2, summary["n_parameter_urls"])
        serialized = json.dumps({k: v for k, v in summary.items() if k != "_probe_url"})
        self.assertNotIn("secret", serialized)
        self.assertNotIn("videoplayback", serialized)

    def test_media_host_policy_rejects_non_googlevideo(self):
        self.assertEqual("x.googlevideo.com", canary._safe_host("https://x.googlevideo.com/a", media=True))
        with self.assertRaises(canary.CanaryError):
            canary._safe_host("https://example.com/a", media=True)
        with self.assertRaises(canary.CanaryError):
            canary._safe_host("http://x.googlevideo.com/a", media=True)

    def test_classify_bootstrap_without_baseline(self):
        decision = canary._classify(
            {"schema": 1, "observation": None},
            self._observation(js="a", ok=True, formats=2),
            {"thresholds": {}},
        )
        self.assertEqual("bootstrap", decision["decision"])

    def test_bootstrap_is_blocked_when_required_probe_fails(self):
        decision = canary._classify(
            {"schema": 1, "observation": None},
            self._observation(js="a", ok=False, formats=0, status="ERROR", streaming=False),
            {"thresholds": {}},
        )
        self.assertEqual("blocked", decision["decision"])

    def test_player_hash_change_alone_does_not_trigger_repair(self):
        before = self._observation(js="a", ok=True, formats=2)
        after = self._observation(js="b", ok=True, formats=2)
        decision = canary._classify(
            {"observation": before},
            after,
            {"thresholds": {"range_regressions_for_repair": 2}},
        )
        self.assertEqual("none", decision["decision"])
        self.assertTrue(any("player JS hash changed" in item for item in decision["informational_changes"]))

    def test_adaptive_video_ladder_disappearing_is_material(self):
        before = self._observation(js="a", ok=True, formats=8, adaptive_video=5, muxed_video=1)
        after = self._observation(js="b", ok=True, formats=2, adaptive_video=0, muxed_video=1)
        decision = canary._classify(
            {"observation": before},
            after,
            {"thresholds": {"required_sentinel_regressions_for_repair": 1}},
        )
        self.assertEqual("repair", decision["decision"])
        self.assertTrue(
            any("adaptive video ladder disappeared" in item for item in decision["material_changes"])
        )

    def test_required_sentinels_expect_adaptive_video(self):
        root = Path(__file__).resolve().parents[2]
        config = json.loads(
            (root / "third_party/LevyraExtractor/canary/config.json").read_text(encoding="utf-8")
        )
        required = [item for item in config["sentinels"] if item.get("required", True)]
        self.assertTrue(required)
        self.assertTrue(all(item.get("expect_adaptive_video") is True for item in required))

    def test_streaming_data_disappearing_triggers_repair(self):
        before = self._observation(js="a", ok=True, formats=2)
        after = self._observation(js="b", ok=False, formats=0, status="ERROR", streaming=False)
        decision = canary._classify(
            {"observation": before},
            after,
            {"thresholds": {"range_regressions_for_repair": 2, "required_sentinel_regressions_for_repair": 1}},
        )
        self.assertEqual("repair", decision["decision"])
        self.assertTrue(any("streamingData disappeared" in item for item in decision["material_changes"]))

    def test_single_range_failure_is_informational_below_threshold(self):
        before = self._observation(js="a", ok=True, formats=2, range_ok=True)
        after = self._observation(js="a", ok=True, formats=2, range_ok=False)
        decision = canary._classify(
            {"observation": before},
            after,
            {"thresholds": {"range_regressions_for_repair": 2}},
        )
        self.assertEqual("none", decision["decision"])
        self.assertTrue(any("below repair threshold" in item for item in decision["informational_changes"]))

    def test_existing_baseline_network_block_is_not_repair(self):
        before = self._observation(js="a", ok=True, formats=2)
        after = self._observation(js="a", ok=False, formats=0, status="ERROR", streaming=False)
        after["sentinels"][0]["observation"] = {"ok": False, "error": "watch page HTTP 429"}
        decision = canary._classify(
            {"observation": before},
            after,
            {"thresholds": {"required_sentinel_regressions_for_repair": 1}},
        )
        self.assertEqual("blocked", decision["decision"])

    def test_single_semantic_sentinel_regression_is_downgraded(self):
        before = self._observation(js="a", ok=True, formats=2)
        after = self._observation(js="b", ok=False, formats=0, status="ERROR", streaming=False)
        decision = canary._classify(
            {"observation": before},
            after,
            {"thresholds": {"required_sentinel_regressions_for_repair": 2}},
        )
        self.assertEqual("none", decision["decision"])
        self.assertTrue(any("below repair threshold" in item for item in decision["informational_changes"]))

    def test_repository_canary_workflow_keeps_repair_draft_and_scoped(self):
        root = Path(__file__).resolve().parents[2]
        workflow = (root / ".github/workflows/nightly-extractor-check.yml").read_text(encoding="utf-8")
        self.assertIn("openai/codex-action@52fe01ec70a42f454c9d2ebd47598f9fd6893d56", workflow)
        self.assertIn("LEVYRA_CANARY_OPENROUTER_API_KEY", workflow)
        self.assertNotIn("LEVYRA_CANARY_OPENAI_API_KEY", workflow)
        self.assertIn("responses-api-endpoint: https://openrouter.ai/api/v1/responses", workflow)
        self.assertIn("model: openrouter/free", workflow)
        self.assertIn("draft: always-true", workflow)
        self.assertIn("permission-profile: \":workspace\"", workflow)
        self.assertIn("safety-strategy: drop-sudo", workflow)
        self.assertNotIn("pull_request_target", workflow)
        self.assertNotIn("enable-auto-merge", workflow)

    def test_repository_canary_requires_multiple_semantic_sentinels(self):
        root = Path(__file__).resolve().parents[2]
        config = json.loads(
            (root / "third_party/LevyraExtractor/canary/config.json").read_text(encoding="utf-8")
        )
        required = [item for item in config["sentinels"] if item.get("required", True)]
        self.assertGreaterEqual(len(required), 2)
        self.assertGreaterEqual(
            config["thresholds"]["required_sentinel_regressions_for_repair"], 2
        )

    def test_accept_writes_sanitized_baseline(self):
        with tempfile.TemporaryDirectory() as temp:
            temp = Path(temp)
            observation_path = temp / "observation.json"
            baseline_path = temp / "baseline.json"
            observation_path.write_text(json.dumps(self._observation(js="x", ok=True, formats=2)), encoding="utf-8")
            args = type("Args", (), {
                "observation": str(observation_path),
                "baseline": str(baseline_path),
                "reason": "test",
            })()
            self.assertEqual(0, canary.command_accept(args))
            baseline = json.loads(baseline_path.read_text(encoding="utf-8"))
            self.assertEqual(1, baseline["schema"])
            self.assertEqual("test", baseline["accepted_reason"])
            self.assertIn("observation", baseline)

    @staticmethod
    def _observation(
        js, ok, formats, status="OK", streaming=True, range_ok=None,
        adaptive_video=1, muxed_video=1,
    ):
        media = {"attempted": range_ok is not None}
        if range_ok is not None:
            media.update({"initial_ok": range_ok, "continuation_ok": True})
        return {
            "schema": 1,
            "sentinels": [
                {
                    "name": "stable",
                    "video_id": "BaW_jenozKc",
                    "required": True,
                    "ok": ok,
                    "observation": {
                        "ok": ok,
                        "player_js": {"sha256": js},
                        "web_client_version": "1",
                        "player": {
                            "playability_status": status,
                            "has_streaming_data": streaming,
                            "total_formats": formats,
                            "adaptive_video_formats": adaptive_video if streaming else 0,
                            "muxed_video_formats": muxed_video if streaming else 0,
                            "streaming_keys": ["formats"] if streaming else [],
                        },
                        "media_probe": media,
                    },
                }
            ],
            "upstreams": [],
        }


if __name__ == "__main__":
    unittest.main()
