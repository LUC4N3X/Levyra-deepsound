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

    def test_summarize_player_response_records_sabr_metadata_without_urls(self):
        player = {
            "playabilityStatus": {"status": "OK"},
            "playerConfig": {
                "mediaCommonConfig": {
                    "mediaUstreamerRequestConfig": {
                        "videoPlaybackUseUmp": True,
                        "videoPlaybackUstreamerConfig": "Q3M9Q29uZmln",
                    }
                }
            },
            "streamingData": {
                "serverAbrStreamingUrl": (
                    "https://rr5---sn-a.googlevideo.com/videoplayback?mn=sn-a,sn-b&sig=secret"
                ),
                "adaptiveFormats": [
                    {
                        "itag": 140,
                        "mimeType": 'audio/mp4; codecs="mp4a.40.2"',
                        "url": "https://rr5---sn-a.googlevideo.com/videoplayback?mn=sn-a,sn-b&n=abc",
                    },
                    {"itag": 137, "mimeType": 'video/mp4; codecs="avc1"', "height": 1080,
                     "url": "https://rr5---sn-a.googlevideo.com/videoplayback?mn=sn-a,sn-b&n=abc"},
                ],
            },
        }

        summary = canary._summarize_player_response(player)
        summary.pop("_probe_url")
        serialized = json.dumps(summary)

        self.assertTrue(summary["has_server_abr_streaming_url"])
        self.assertTrue(summary["server_abr_host_is_googlevideo"])
        self.assertEqual(2, summary["server_abr_media_networks"])
        self.assertEqual(2, summary["direct_media_networks"])
        self.assertTrue(summary["has_ustreamer_config"])
        self.assertTrue(summary["video_playback_use_ump"])
        self.assertNotIn("secret", serialized)
        self.assertNotIn("googlevideo.com/videoplayback", serialized)

    def test_delivery_classification_separates_sabr_security_and_client_failures(self):
        healthy = {
            "playability_status": "OK",
            "has_streaming_data": True,
            "direct_urls": 4,
            "adaptive_audio_formats": 2,
            "adaptive_video_formats": 2,
        }
        self.assertEqual(canary.DELIVERY_DIRECT_HEALTHY, canary._classify_delivery(healthy))
        self.assertEqual(
            canary.DELIVERY_DIRECT_DEGRADED,
            canary._classify_delivery({**healthy, "adaptive_video_formats": 0}),
        )
        self.assertEqual(
            canary.DELIVERY_SABR_ONLY,
            canary._classify_delivery(
                {
                    "playability_status": "OK",
                    "has_streaming_data": True,
                    "direct_urls": 0,
                    "cipher_urls": 0,
                    "has_server_abr_streaming_url": True,
                }
            ),
        )
        self.assertEqual(
            canary.DELIVERY_DIRECT_UNAVAILABLE,
            canary._classify_delivery(
                {"playability_status": "OK", "has_streaming_data": True, "direct_urls": 0}
            ),
        )
        self.assertEqual(
            canary.DELIVERY_SECURITY_FAILURE,
            canary._classify_delivery(
                {"playability_status": "LOGIN_REQUIRED", "playability_reason": "Please sign in"}
            ),
        )
        self.assertEqual(
            canary.DELIVERY_SECURITY_FAILURE,
            canary._classify_delivery(
                {
                    "playability_status": "UNPLAYABLE",
                    "playability_reason": "Sign in to confirm you are not a bot",
                }
            ),
        )
        self.assertEqual(
            canary.DELIVERY_CLIENT_FAILURE,
            canary._classify_delivery({"playability_status": "ERROR", "playability_reason": "nope"}),
        )
        self.assertEqual(
            canary.DELIVERY_CLIENT_FAILURE,
            canary._classify_delivery({"playability_status": "OK", "has_streaming_data": False}),
        )

    def test_client_matrix_matches_levyra_policy_and_excludes_android_vr(self):
        names = [entry["name"] for entry in canary.LEVYRA_CLIENT_MATRIX]

        self.assertEqual(sorted(names), sorted(set(names)))
        self.assertNotIn("ANDROID_VR", names)
        for expected in ("VISIONOS", "ANDROID_MUSIC", "ANDROID", "IOS", "WEB_REMIX", "WEB", "WEB_EMBEDDED_PLAYER"):
            self.assertIn(expected, names)

    def test_sabr_enforcement_is_material_only_when_direct_disappears_everywhere(self):
        enforced = canary._summarize_delivery(
            [
                {"client": "IOS", "delivery": canary.DELIVERY_SABR_ONLY,
                 "player": {"has_server_abr_streaming_url": True}},
                {"client": "WEB", "delivery": canary.DELIVERY_CLIENT_FAILURE, "player": {}},
            ]
        )
        mixed = canary._summarize_delivery(
            [
                {"client": "IOS", "delivery": canary.DELIVERY_DIRECT_HEALTHY,
                 "player": {"has_server_abr_streaming_url": True}},
                {"client": "WEB", "delivery": canary.DELIVERY_SABR_ONLY,
                 "player": {"has_server_abr_streaming_url": True}},
            ]
        )

        self.assertTrue(enforced["sabr_enforced"])
        self.assertFalse(mixed["sabr_enforced"])

        baseline = {
            "schema": canary.SCHEMA_VERSION,
            "observation": {
                "schema": canary.SCHEMA_VERSION,
                "sentinels": [
                    {
                        "name": "s",
                        "required": True,
                        "ok": True,
                        "observation": {"player": {"playability_status": "OK"}, "delivery_summary": mixed},
                    }
                ],
            },
        }
        observation = {
            "schema": canary.SCHEMA_VERSION,
            "sentinels": [
                {
                    "name": "s",
                    "required": True,
                    "ok": True,
                    "observation": {"player": {"playability_status": "OK"}, "delivery_summary": enforced},
                }
            ],
        }

        decision = canary._classify(
            baseline, observation, {"thresholds": {"required_sentinel_regressions_for_repair": 1}}
        )
        self.assertEqual("repair", decision["decision"])
        self.assertTrue(any("SABR only" in item for item in decision["material_changes"]))

    def test_a_matrix_where_every_probe_failed_is_not_evidence(self):
        all_failed = canary._summarize_delivery(
            [
                {"client": "IOS", "delivery": canary.DELIVERY_TRANSPORT_FAILURE, "player": {}},
                {"client": "WEB", "delivery": canary.DELIVERY_CLIENT_FAILURE, "player": {}},
            ]
        )
        healthy_before = canary._summarize_delivery(
            [
                {"client": "IOS", "delivery": canary.DELIVERY_DIRECT_HEALTHY,
                 "player": {"has_server_abr_streaming_url": True}},
                {"client": "WEB", "delivery": canary.DELIVERY_CLIENT_FAILURE, "player": {}},
            ]
        )

        self.assertFalse(canary._delivery_evidence_is_conclusive(all_failed))
        self.assertFalse(canary._delivery_evidence_is_conclusive({}))
        self.assertFalse(canary._delivery_evidence_is_conclusive(canary._summarize_delivery([])))
        self.assertTrue(canary._delivery_evidence_is_conclusive(healthy_before))

        baseline = {
            "schema": canary.SCHEMA_VERSION,
            "observation": {
                "schema": canary.SCHEMA_VERSION,
                "sentinels": [
                    {
                        "name": "s",
                        "required": True,
                        "ok": True,
                        "observation": {
                            "player": {"playability_status": "OK"},
                            "delivery_summary": healthy_before,
                        },
                    }
                ],
            },
        }
        observation = {
            "schema": canary.SCHEMA_VERSION,
            "sentinels": [
                {
                    "name": "s",
                    "required": True,
                    "ok": True,
                    "observation": {
                        "player": {"playability_status": "OK"},
                        "delivery_summary": all_failed,
                    },
                }
            ],
        }

        decision = canary._classify(
            baseline, observation, {"thresholds": {"required_sentinel_regressions_for_repair": 1}}
        )
        self.assertEqual("none", decision["decision"])

    def test_canary_error_carries_the_http_status_for_classification(self):
        self.assertIsNone(canary.CanaryError("invalid player JSON: boom").status)
        self.assertEqual(403, canary.CanaryError("player endpoint HTTP 403", status=403).status)

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

    def test_legacy_baseline_is_blocked_before_format_comparison(self):
        legacy = self._observation(js="a", ok=True, formats=8, adaptive_video=5, muxed_video=1)
        legacy["schema"] = 1
        legacy["sentinels"][0]["observation"]["player"].pop("adaptive_video_formats", None)
        legacy["sentinels"][0]["observation"]["player"].pop("muxed_video_formats", None)
        after = self._observation(js="b", ok=True, formats=2, adaptive_video=0, muxed_video=1)

        decision = canary._classify(
            {"schema": 1, "observation": legacy},
            after,
            {"thresholds": {"required_sentinel_regressions_for_repair": 1}},
        )

        self.assertEqual("blocked", decision["decision"])
        self.assertTrue(any("baseline schema" in item.lower() for item in decision["informational_changes"]))

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
        self.assertIn("openai/codex-action@86365089eb2b84e0a8fb0717b304f8bdcb13b20e", workflow)
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
            self.assertEqual(canary.SCHEMA_VERSION, baseline["schema"])
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
            "schema": canary.SCHEMA_VERSION,
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


class CapabilityProbePatch:
    def __init__(self, clients, html=""):
        self.clients = clients
        self.html = html

    def __enter__(self):
        self.inputs = canary._capability_matrix_inputs
        self.matrix = canary._probe_client_matrix
        self.client = canary._probe_client
        html = self.html
        clients = self.clients
        canary._capability_matrix_inputs = lambda video_id, *, hl, gl: {
            "innertube_query_value": "key",
            "web_client_version": "1.0",
            "visitor_data": "visitor",
            "_watch_html": html,
        }
        canary._probe_client_matrix = lambda **kwargs: clients
        canary._probe_client = lambda entry, **kwargs: {
            "client": entry["name"],
            "delivery": canary.DELIVERY_SECURITY_FAILURE,
        }
        return self

    def __exit__(self, *args):
        canary._capability_matrix_inputs = self.inputs
        canary._probe_client_matrix = self.matrix
        canary._probe_client = self.client
        return False


class YoutubeCanaryCapabilityCheckTest(unittest.TestCase):
    def _client(self, name, delivery, sabr=False):
        entry = {"client": name, "delivery": delivery, "latency_ms": 10}
        if delivery != canary.DELIVERY_TRANSPORT_FAILURE:
            entry["player"] = {"has_server_abr_streaming_url": sabr}
        return entry

    def _observation(self, checks):
        return {
            "schema": canary.SCHEMA_VERSION,
            "sentinels": [],
            "capability_checks": checks,
            "upstreams": [],
        }

    def test_default_checks_cover_made_for_kids_and_potoken(self):
        names = {str(item["name"]) for item in canary.DEFAULT_CAPABILITY_CHECKS}
        self.assertEqual({canary.CAPABILITY_MADE_FOR_KIDS, canary.CAPABILITY_POTOKEN}, names)
        for item in canary.DEFAULT_CAPABILITY_CHECKS:
            self.assertTrue(canary.VIDEO_ID_RE.fullmatch(str(item["video_id"])))

    def test_shipped_config_declares_both_capability_checks(self):
        config_path = (
            Path(__file__).resolve().parents[2]
            / "third_party"
            / "LevyraExtractor"
            / "canary"
            / "config.json"
        )
        config = json.loads(config_path.read_text(encoding="utf-8"))
        checks = canary._configured_capability_checks(config)
        self.assertEqual(
            {canary.CAPABILITY_MADE_FOR_KIDS, canary.CAPABILITY_POTOKEN},
            {str(item["name"]) for item in checks},
        )

    def test_client_matrix_po_token_flags_match_the_shipped_playback_profiles(self):
        flags = {
            str(entry["name"]): bool(entry.get("requires_po_token", False))
            for entry in canary.LEVYRA_CLIENT_MATRIX
        }
        self.assertEqual(
            {
                "VISIONOS": False,
                "ANDROID_MUSIC": False,
                "ANDROID": False,
                "IOS": False,
                "WEB_REMIX": True,
                "WEB": True,
                "WEB_EMBEDDED_PLAYER": False,
            },
            flags,
        )

    def test_made_for_kids_failure_is_material_even_when_sentinels_stay_healthy(self):
        baseline = {
            "schema": canary.SCHEMA_VERSION,
            "observation": self._observation(
                [{"name": canary.CAPABILITY_MADE_FOR_KIDS, "status": canary.CAPABILITY_STATUS_PASS}]
            ),
        }
        observation = self._observation(
            [
                {
                    "name": canary.CAPABILITY_MADE_FOR_KIDS,
                    "status": canary.CAPABILITY_STATUS_FAIL,
                    "detail": "no probed client delivers the made-for-kids fixture",
                }
            ]
        )

        decision = canary._classify(baseline, observation, {})

        self.assertEqual("repair", decision["decision"])
        self.assertTrue(
            any(
                "MADE_FOR_KIDS capability check FAIL" in item
                for item in decision["material_changes"]
            ),
            decision["material_changes"],
        )

    def test_potoken_failure_is_material_even_when_sentinels_stay_healthy(self):
        baseline = {
            "schema": canary.SCHEMA_VERSION,
            "observation": self._observation(
                [{"name": canary.CAPABILITY_POTOKEN, "status": canary.CAPABILITY_STATUS_PASS}]
            ),
        }
        observation = self._observation(
            [
                {
                    "name": canary.CAPABILITY_POTOKEN,
                    "status": canary.CAPABILITY_STATUS_FAIL,
                    "detail": "no PoToken-free client delivers",
                }
            ]
        )

        decision = canary._classify(baseline, observation, {})

        self.assertEqual("repair", decision["decision"])
        self.assertTrue(
            any("POTOKEN capability check FAIL" in item for item in decision["material_changes"]),
            decision["material_changes"],
        )

    def test_blocked_capability_check_never_triggers_a_repair(self):
        baseline = {
            "schema": canary.SCHEMA_VERSION,
            "observation": self._observation(
                [{"name": canary.CAPABILITY_POTOKEN, "status": canary.CAPABILITY_STATUS_PASS}]
            ),
        }
        observation = self._observation(
            [
                {
                    "name": canary.CAPABILITY_POTOKEN,
                    "status": canary.CAPABILITY_STATUS_BLOCKED,
                    "detail": "every client probe failed on our side",
                }
            ]
        )

        decision = canary._classify(baseline, observation, {})

        self.assertEqual("none", decision["decision"])
        self.assertTrue(
            any(
                "POTOKEN capability check BLOCKED" in item
                for item in decision["informational_changes"]
            ),
            decision["informational_changes"],
        )

    def test_recovered_capability_check_does_not_open_a_repair(self):
        baseline = {
            "schema": canary.SCHEMA_VERSION,
            "observation": self._observation(
                [{"name": canary.CAPABILITY_MADE_FOR_KIDS, "status": canary.CAPABILITY_STATUS_FAIL}]
            ),
        }
        observation = self._observation(
            [{"name": canary.CAPABILITY_MADE_FOR_KIDS, "status": canary.CAPABILITY_STATUS_PASS}]
        )

        decision = canary._classify(baseline, observation, {})

        self.assertEqual("none", decision["decision"])

    def test_report_renders_distinguishable_capability_lines(self):
        observation = self._observation(
            [
                {
                    "name": canary.CAPABILITY_MADE_FOR_KIDS,
                    "status": canary.CAPABILITY_STATUS_PASS,
                    "detail": "3/7 clients deliver the made-for-kids fixture",
                },
                {
                    "name": canary.CAPABILITY_POTOKEN,
                    "status": canary.CAPABILITY_STATUS_FAIL,
                    "detail": "no PoToken-free client delivers",
                },
            ]
        )

        report = canary._render_report(
            observation,
            {
                "decision": "repair",
                "severity": "major",
                "material_changes": [],
                "informational_changes": [],
            },
        )

        self.assertIn("MADE_FOR_KIDS PASS", report)
        self.assertIn("POTOKEN FAIL", report)

    def test_made_for_kids_passes_when_any_client_delivers(self):
        clients = [
            self._client("VISIONOS", canary.DELIVERY_DIRECT_HEALTHY),
            self._client("WEB", canary.DELIVERY_SECURITY_FAILURE),
        ]
        with CapabilityProbePatch(clients, html='"isMadeForKids":true'):
            result = canary._probe_capability_check(
                {
                    "name": canary.CAPABILITY_MADE_FOR_KIDS,
                    "kind": canary.CAPABILITY_KIND_MADE_FOR_KIDS,
                    "video_id": "XqZsoesa55w",
                },
                hl="en",
                gl="US",
            )

        self.assertEqual(canary.CAPABILITY_STATUS_PASS, result["status"])
        self.assertEqual(["VISIONOS"], result["playable_clients"])
        self.assertEqual({"isMadeForKids": True}, result["fixture_markers"])

    def test_made_for_kids_fails_when_no_client_delivers(self):
        clients = [
            self._client("VISIONOS", canary.DELIVERY_DIRECT_UNAVAILABLE),
            self._client("WEB", canary.DELIVERY_SECURITY_FAILURE),
        ]
        with CapabilityProbePatch(clients):
            result = canary._probe_capability_check(
                {
                    "name": canary.CAPABILITY_MADE_FOR_KIDS,
                    "kind": canary.CAPABILITY_KIND_MADE_FOR_KIDS,
                    "video_id": "XqZsoesa55w",
                },
                hl="en",
                gl="US",
            )

        self.assertEqual(canary.CAPABILITY_STATUS_FAIL, result["status"])

    def test_potoken_passes_while_only_potoken_bound_clients_are_blocked(self):
        clients = [
            self._client("VISIONOS", canary.DELIVERY_DIRECT_HEALTHY),
            self._client("WEB", canary.DELIVERY_SECURITY_FAILURE),
            self._client("WEB_REMIX", canary.DELIVERY_SECURITY_FAILURE),
        ]
        with CapabilityProbePatch(clients):
            result = canary._probe_capability_check(
                {
                    "name": canary.CAPABILITY_POTOKEN,
                    "kind": canary.CAPABILITY_KIND_POTOKEN,
                    "video_id": "BaW_jenozKc",
                },
                hl="en",
                gl="US",
            )

        self.assertEqual(canary.CAPABILITY_STATUS_PASS, result["status"])
        self.assertTrue(result["po_token_enforced"])
        self.assertEqual(["VISIONOS"], result["po_token_free_playable"])

    def test_potoken_fails_when_the_potoken_free_path_disappears(self):
        clients = [
            self._client("VISIONOS", canary.DELIVERY_SECURITY_FAILURE),
            self._client("IOS", canary.DELIVERY_SECURITY_FAILURE),
            self._client("WEB", canary.DELIVERY_DIRECT_HEALTHY),
        ]
        with CapabilityProbePatch(clients):
            result = canary._probe_capability_check(
                {
                    "name": canary.CAPABILITY_POTOKEN,
                    "kind": canary.CAPABILITY_KIND_POTOKEN,
                    "video_id": "BaW_jenozKc",
                },
                hl="en",
                gl="US",
            )

        self.assertEqual(canary.CAPABILITY_STATUS_FAIL, result["status"])
        self.assertEqual([], result["po_token_free_playable"])

    def test_capability_check_is_blocked_when_the_watch_page_is_unreachable(self):
        def fail_inputs(video_id, *, hl, gl):
            raise canary.CanaryError("watch page HTTP 429", status=429)

        original = canary._capability_matrix_inputs
        canary._capability_matrix_inputs = fail_inputs
        try:
            result = canary._probe_capability_check(
                {
                    "name": canary.CAPABILITY_POTOKEN,
                    "kind": canary.CAPABILITY_KIND_POTOKEN,
                    "video_id": "BaW_jenozKc",
                },
                hl="en",
                gl="US",
            )
        finally:
            canary._capability_matrix_inputs = original

        self.assertEqual(canary.CAPABILITY_STATUS_BLOCKED, result["status"])
        self.assertIn("429", result["detail"])

    def test_capability_check_is_blocked_when_every_probe_failed_on_our_side(self):
        clients = [
            self._client("VISIONOS", canary.DELIVERY_TRANSPORT_FAILURE),
            self._client("WEB", canary.DELIVERY_TRANSPORT_FAILURE),
        ]
        with CapabilityProbePatch(clients):
            result = canary._probe_capability_check(
                {
                    "name": canary.CAPABILITY_MADE_FOR_KIDS,
                    "kind": canary.CAPABILITY_KIND_MADE_FOR_KIDS,
                    "video_id": "XqZsoesa55w",
                },
                hl="en",
                gl="US",
            )

        self.assertEqual(canary.CAPABILITY_STATUS_BLOCKED, result["status"])

    def test_invalid_configured_video_id_is_blocked_not_failed(self):
        result = canary._probe_capability_check(
            {
                "name": canary.CAPABILITY_POTOKEN,
                "kind": canary.CAPABILITY_KIND_POTOKEN,
                "video_id": "nope",
            },
            hl="en",
            gl="US",
        )

        self.assertEqual(canary.CAPABILITY_STATUS_BLOCKED, result["status"])


if __name__ == "__main__":
    unittest.main()
