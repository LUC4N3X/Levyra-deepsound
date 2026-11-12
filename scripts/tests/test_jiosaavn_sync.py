from __future__ import annotations

import json
import random
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock

REPO_ROOT = Path(__file__).resolve().parents[2]
SCRIPTS_DIR = REPO_ROOT / "scripts"
if str(SCRIPTS_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPTS_DIR))

import jiosaavn_sync as js  # noqa: E402

SAMPLE_ENCRYPTED_PAYLOAD = (
    "ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDy8IXxuTNJ1oLbvDGDneZj5h25kdaKPCof"
    "228ruhJnw7PIr7uKBnaPmxw7tS9a8Gtq"
)
SAMPLE_DECODED_96 = "https://aac.saavncdn.com/396/eca27e31e93211051fa0de18160ea825_96.mp4"
SAMPLE_DECODED_320 = "https://aac.saavncdn.com/396/eca27e31e93211051fa0de18160ea825_320.mp4"


def _healthy_tiers(*, tiers: tuple[int, ...] = (96, 160, 320)) -> dict[str, dict]:
    return {
        str(tier): {
            "status": "valid",
            "valid": True,
            "http_status": 206,
            "verified_tier": tier,
            "estimated_kbps": tier + 3,
            "has_mp4a": True,
        }
        for tier in tiers
    }


def _track_entry(
    name: str,
    *,
    ok: bool = True,
    access_blocked: bool = False,
    expect_explicit_and_clean: bool = False,
    explicit_and_clean_ok: bool = True,
    tiers: tuple[int, ...] = (96, 160, 320),
    error: str = "",
) -> dict:
    obs = {
        "ok": ok,
        "access_blocked": access_blocked,
        "explicit_and_clean_ok": explicit_and_clean_ok,
        "search": {
            "envelope_ok": ok,
            "top_keys": ["results", "start", "total"] if ok else [],
            "song_count": 10 if ok else 0,
        },
        "schema": {
            "song_keys": ["explicit_content", "id", "more_info", "title"] if ok else [],
            "more_info_keys": ["duration", "encrypted_media_url", "vlink"] if ok else [],
        },
        "details": {"ok": ok, "envelope_kind": "songs_array"} if ok else {"ok": False},
        "lyrics": {"skipped": True, "ok": True},
        "streams": {
            "decrypt_ok": ok,
            "auth_api_ok": ok,
            "auth_url_valid": ok,
            "verified_tiers": list(tiers) if ok else [],
            "tiers": _healthy_tiers(tiers=tiers) if ok else {},
            "playable_any_tier": ok and bool(tiers),
            "cdn_access_blocked": False,
        },
    }
    if error:
        obs["error"] = error
    return {
        "name": name,
        "ok": ok,
        "required": True,
        "expect_explicit_and_clean": expect_explicit_and_clean,
        "observation": obs,
    }


def _entity_entry(
    name: str,
    *,
    ok: bool = True,
    access_blocked: bool = False,
    error: str = "",
) -> dict:
    obs: dict = {"ok": ok, "access_blocked": access_blocked}
    if error:
        obs["error"] = error
    return {"name": name, "ok": ok, "required": True, "observation": obs}


def _healthy_observation() -> dict:
    return {
        "schema": js.SCHEMA_VERSION,
        "observed_at_epoch": 1700000000,
        "api_contract": {
            "host": "www.jiosaavn.com",
            "api_version": "4",
            "client_context": "web6dot0",
            "open_media_host": "aac.saavncdn.com",
        },
        "tracks": [
            _track_entry(
                "starboy-explicit-clean",
                expect_explicit_and_clean=True,
                explicit_and_clean_ok=True,
            ),
            _track_entry(
                "blinding-lights",
                expect_explicit_and_clean=True,
                explicit_and_clean_ok=True,
            ),
            _track_entry(
                "tum-hi-ho-hindi-lyrics",
                expect_explicit_and_clean=False,
                explicit_and_clean_ok=True,
            ),
        ],
        "artists": [
            _entity_entry("the-weeknd"),
            _entity_entry("arijit-singh"),
        ],
        "albums": [
            _entity_entry("after-hours"),
            _entity_entry("aashiqui-2"),
        ],
        "upstreams": [],
    }


def _baseline_from(observation: dict) -> dict:
    return {
        "schema": js.SCHEMA_VERSION,
        "accepted_at_epoch": 1700000000,
        "accepted_reason": "test baseline",
        "observation": js._sanitize_for_baseline(observation),
    }


class TestCryptoAndSafety(unittest.TestCase):
    def test_des_decryption_matches_levyra_kotlin_vector(self) -> None:
        decoded = js.decode_media_token(SAMPLE_ENCRYPTED_PAYLOAD)
        self.assertEqual(decoded, SAMPLE_DECODED_96)
        loc = js.parse_media_location(decoded or "")
        self.assertIsNotNone(loc)
        assert loc is not None
        self.assertEqual(loc.open_url(320), SAMPLE_DECODED_320)
        self.assertEqual(loc.open_url(96), SAMPLE_DECODED_96)

    def test_des_decryption_rejects_invalid_and_non_url_payloads(self) -> None:
        self.assertIsNone(js.decode_media_token(""))
        self.assertIsNone(js.decode_media_token("not-base64!!!"))
        self.assertIsNone(js.decode_media_token("A" * 600))
        self.assertIsNone(js.decode_media_token("SGVsbG8gd29ybGQ="))

    def test_host_allowlists_reject_untrusted_destinations(self) -> None:
        self.assertEqual(js._safe_host("https://www.jiosaavn.com/api.php"), "www.jiosaavn.com")
        self.assertEqual(js._safe_host("https://aac.saavncdn.com/396/track_320.mp4", media=True), "aac.saavncdn.com")
        for bad_url, media in (
            ("http://www.jiosaavn.com/api.php", False),
            ("https://user:pass@www.jiosaavn.com/api.php", False),
            ("https://www.jiosaavn.com:8443/api.php", False),
            ("https://evil.example/api.php", False),
            ("https://saavncdn.com.evil.example/a.mp4", True),
            ("https://127.0.0.1/a.mp4", True),
        ):
            with self.assertRaises(js.SyncError):
                js._safe_host(bad_url, media=media)

    def test_parse_media_location_strips_query_and_extracts_tier(self) -> None:
        loc = js.parse_media_location(
            "https://web.saavncdn.com/396/eca27e31e93211051fa0de18160ea825_160.mp4"
            "?Expires=1794500000&Signature=dummySigValue&Key-Pair-Id=K1234",
            override_open_host="aac.saavncdn.com",
            authorized=True,
        )
        self.assertIsNotNone(loc)
        assert loc is not None
        self.assertEqual(loc.open_host, "aac.saavncdn.com")
        self.assertEqual(loc.authorized_tier, 160)
        self.assertTrue(loc.has_expires)
        self.assertEqual(
            loc.open_url(320),
            "https://aac.saavncdn.com/396/eca27e31e93211051fa0de18160ea825_320.mp4",
        )
        self.assertIsNone(js.parse_media_location("https://evil.example/1/abc_320.mp4"))
        self.assertIsNone(js.parse_media_location("https://aac.saavncdn.com/../secret_320.mp4"))

    def test_sanitize_text_redacts_ips_paths_and_signed_params(self) -> None:
        raw = (
            "Failed fetching https://aac.saavncdn.com/123/abc_320.mp4"
            "?Expires=12345&Signature=secretVal&Key-Pair-Id=KP1 via 49.36.12.88"
        )
        cleaned = js.sanitize_text(raw)
        self.assertNotIn("49.36.12.88", cleaned)
        self.assertNotIn("secretVal", cleaned)
        self.assertNotIn("KP1", cleaned)
        self.assertNotIn("abc_320.mp4", cleaned)
        self.assertIn("<redacted-ip>", cleaned)

    def test_india_profile_rotates_blocks_and_isolates_media_headers(self) -> None:
        rng = random.Random(42)
        first = js.build_india_profile(rng=rng)
        second = js.build_india_profile(excluded_indices={first.block_index}, rng=rng)
        self.assertNotEqual(first.block_index, second.block_index)
        api_headers = first.api_headers()
        self.assertEqual(api_headers["Accept-Language"], "en-IN,en;q=0.9")
        self.assertIn("X-Forwarded-For", api_headers)
        media_headers = first.media_headers()
        self.assertNotIn("X-Forwarded-For", media_headers)
        self.assertNotIn("X-Real-IP", media_headers)
        self.assertEqual(media_headers["Range"], f"bytes=0-{js.MEDIA_PROBE_RANGE_END}")


class TestStreamQualityAndSchemas(unittest.TestCase):
    def _mp4_prefix(self) -> bytes:
        return b"\x00\x00\x00\x1cftypmp42\x00\x00\x00\x00mp42isom" + b"moovtrakmdia" + b"mp4a" + b"\x00" * 64

    def test_validate_media_probe_accepts_96_160_320_tiers(self) -> None:
        duration_sec = 200
        for tier in (96, 160, 320):
            total_bytes = int(tier * 1000 / 8 * duration_sec)
            res = js.HttpResult(
                status=206,
                headers={
                    "content-type": "audio/mp4",
                    "content-range": f"bytes 0-8191/{total_bytes}",
                },
                body=self._mp4_prefix(),
            )
            checked = js.validate_media_probe(res, tier, duration_sec)
            self.assertTrue(checked["valid"], checked)
            self.assertEqual(checked["verified_tier"], tier)
            self.assertTrue(checked["has_mp4a"])

    def test_validate_media_probe_rejects_downgraded_or_html_responses(self) -> None:
        total_96 = int(96 * 1000 / 8 * 200)
        downgraded = js.HttpResult(
            status=206,
            headers={
                "content-type": "audio/mp4",
                "content-range": f"bytes 0-8191/{total_96}",
            },
            body=self._mp4_prefix(),
        )
        res = js.validate_media_probe(downgraded, 320, 200)
        self.assertFalse(res["valid"])
        self.assertEqual(res["status"], "bitrate_mismatch")

        html = js.HttpResult(
            status=200,
            headers={"content-type": "text/html; charset=utf-8"},
            body=b"<html>Access Denied</html>",
        )
        res_html = js.validate_media_probe(html, 160, 200)
        self.assertFalse(res_html["valid"])
        self.assertEqual(res_html["status"], "not_audio")

        missing = js.HttpResult(status=404, headers={}, body=b"")
        res_404 = js.validate_media_probe(missing, 320, 200)
        self.assertFalse(res_404["valid"])
        self.assertEqual(res_404["status"], "http_404")

    def test_search_schema_checks_explicit_and_clean_coexistence(self) -> None:
        songs = [
            {
                "id": "s1",
                "title": "Starboy",
                "explicit_content": "1",
                "more_info": {
                    "duration": "230",
                    "encrypted_media_url": SAMPLE_ENCRYPTED_PAYLOAD,
                    "has_lyrics": "true",
                    "lyrics_id": "s1",
                    "artistMap": {"primary_artists": [{"name": "The Weeknd"}]},
                },
            },
            {
                "id": "s2",
                "title": "Starboy",
                "explicit_content": "0",
                "more_info": {
                    "duration": "230",
                    "encrypted_media_url": SAMPLE_ENCRYPTED_PAYLOAD,
                    "artistMap": {"primary_artists": [{"name": "The Weeknd"}]},
                },
            },
        ]
        summary = js._inspect_song_schema(songs)
        self.assertTrue(summary["has_explicit"])
        self.assertTrue(summary["has_clean"])
        self.assertEqual(summary["playable_count"], 2)
        self.assertIsNotNone(summary.get("_first_playable"))
        self.assertEqual(summary.get("_lyrics_id"), "s1")

    def test_missing_title_or_primary_artist_marks_search_and_details_unplayable(self) -> None:
        missing_title_summary = js._inspect_song_schema(
            [
                {
                    "id": "s1",
                    "title": "   ",
                    "more_info": {
                        "duration": "230",
                        "encrypted_media_url": SAMPLE_ENCRYPTED_PAYLOAD,
                        "artistMap": {"primary_artists": [{"name": "The Weeknd"}]},
                    },
                }
            ]
        )
        self.assertEqual(missing_title_summary["playable_count"], 0)
        self.assertIsNone(missing_title_summary.get("_first_playable"))

        missing_artist_summary = js._inspect_song_schema(
            [
                {
                    "id": "s2",
                    "title": "Starboy",
                    "more_info": {
                        "duration": "230",
                        "encrypted_media_url": SAMPLE_ENCRYPTED_PAYLOAD,
                        "artistMap": {"primary_artists": []},
                    },
                }
            ]
        )
        self.assertEqual(missing_artist_summary["playable_count"], 0)
        self.assertIsNone(missing_artist_summary.get("_first_playable"))

        api_cfg = {"host": "www.jiosaavn.com"}
        details_payload = {
            "songs": [
                {
                    "id": "s2",
                    "title": "Starboy",
                    "more_info": {
                        "duration": "230",
                        "encrypted_media_url": SAMPLE_ENCRYPTED_PAYLOAD,
                        "artistMap": {"primary_artists": []},
                    },
                }
            ]
        }
        with mock.patch.object(
            js,
            "_bounded_request",
            return_value=js.HttpResult(
                status=200,
                headers={"content-type": "application/json"},
                body=json.dumps(details_payload).encode("utf-8"),
            ),
        ):
            details = js._probe_song_details(api_cfg, {"song_details": "song.getDetails"}, "s2")
        self.assertFalse(details["ok"])
        self.assertTrue(details["has_title"])
        self.assertFalse(details["has_primary_artists"])

    def test_mocked_track_artist_album_probes_handle_partial_tiers(self) -> None:
        mp4_bytes = self._mp4_prefix()
        api_cfg = {
            "host": "www.jiosaavn.com",
            "api_path": "/api.php",
            "api_version": "4",
            "client_context": "web6dot0",
            "open_media_host": "aac.saavncdn.com",
            "endpoints": {
                "search_songs": "search.getResults",
                "search_artists": "search.getArtistResults",
                "search_albums": "search.getAlbumResults",
                "song_details": "song.getDetails",
                "artist_details": "artist.getArtistPageDetails",
                "album_details": "content.getAlbumDetails",
                "authorize_media": "song.generateAuthToken",
                "lyrics": "lyrics.getLyrics",
            },
        }

        def fake_bounded(url: str, *, headers=None, max_bytes=0, timeout=0, media=False):
            if media:
                if "_320.mp4" in url:
                    return js.HttpResult(status=404, headers={}, body=b"")
                tier = 160 if "_160.mp4" in url else 96
                total = int(tier * 1000 / 8 * 230)
                return js.HttpResult(
                    status=206,
                    headers={
                        "content-type": "audio/mp4",
                        "content-range": f"bytes 0-8191/{total}",
                    },
                    body=mp4_bytes,
                )
            if "search.getResults" in url:
                payload = {
                    "start": 1,
                    "total": 2,
                    "results": [
                        {
                            "id": "s1",
                            "title": "Starboy",
                            "explicit_content": "1",
                            "more_info": {
                                "duration": "230",
                                "encrypted_media_url": SAMPLE_ENCRYPTED_PAYLOAD,
                                "has_lyrics": "true",
                                "lyrics_id": "s1",
                                "artistMap": {
                                    "primary_artists": [{"name": "The Weeknd"}],
                                    "featured_artists": [],
                                    "artists": [],
                                },
                            },
                        },
                        {
                            "id": "s2",
                            "title": "Starboy",
                            "explicit_content": "0",
                            "more_info": {
                                "duration": "230",
                                "encrypted_media_url": SAMPLE_ENCRYPTED_PAYLOAD,
                                "artistMap": {"primary_artists": [{"name": "The Weeknd"}]},
                            },
                        },
                    ],
                }
            elif "song.getDetails" in url:
                payload = {
                    "songs": [
                        {
                            "id": "s1",
                            "title": "Starboy",
                            "more_info": {
                                "duration": "230",
                                "encrypted_media_url": SAMPLE_ENCRYPTED_PAYLOAD,
                                "artistMap": {"primary_artists": [{"name": "The Weeknd"}]},
                            },
                        }
                    ]
                }
            elif "song.generateAuthToken" in url:
                payload = {
                    "status": "success",
                    "auth_url": (
                        "https://web.saavncdn.com/396/eca27e31e93211051fa0de18160ea825_160.mp4"
                        "?Expires=1794500000&Signature=dummySig&Key-Pair-Id=KP1"
                    ),
                }
            elif "lyrics.getLyrics" in url:
                payload = {"lyrics": "Line one<br/>Line two"}
            elif "search.getArtistResults" in url:
                payload = {"results": [{"id": "art1", "name": "The Weeknd"}]}
            elif "artist.getArtistPageDetails" in url:
                payload = {
                    "artistId": "art1",
                    "name": "The Weeknd",
                    "topSongs": [{"id": "s1"}],
                    "topAlbums": [{"id": "alb1"}],
                }
            elif "search.getAlbumResults" in url:
                payload = {"results": [{"id": "alb1", "title": "After Hours"}]}
            else:
                payload = {
                    "id": "alb1",
                    "title": "After Hours",
                    "more_info": {},
                    "list": [{"id": "s1", "more_info": {"duration": "230"}}],
                }
            return js.HttpResult(
                status=200,
                headers={"content-type": "application/json"},
                body=json.dumps(payload).encode("utf-8"),
            )

        with mock.patch.object(js, "_bounded_request", side_effect=fake_bounded):
            track_res = js._probe_track_sentinel(
                {
                    "name": "starboy",
                    "query": "The Weeknd Starboy",
                    "expect_explicit_and_clean": True,
                    "expect_lyrics": True,
                },
                api_cfg,
                [96, 160, 320],
                attempts=1,
                retry_delay_seconds=0.0,
            )
            self.assertTrue(track_res["ok"])
            self.assertTrue(track_res["observation"]["explicit_and_clean_ok"])
            self.assertTrue(track_res["observation"]["lyrics"]["ok"])
            self.assertEqual(track_res["observation"]["streams"]["verified_tiers"], [96, 160])

            artist_res = js._probe_artist_sentinel(
                {"name": "the-weeknd", "query": "The Weeknd"},
                api_cfg,
                attempts=1,
                retry_delay_seconds=0.0,
            )
            self.assertTrue(artist_res["ok"])

            album_res = js._probe_album_sentinel(
                {"name": "after-hours", "query": "After Hours"},
                api_cfg,
                attempts=1,
                retry_delay_seconds=0.0,
            )
            self.assertTrue(album_res["ok"])

    def test_downgraded_bitrate_records_measured_tier_and_falls_back_to_auth_host(self) -> None:
        mp4_bytes = self._mp4_prefix()
        api_cfg = {
            "host": "www.jiosaavn.com",
            "api_path": "/api.php",
            "api_version": "4",
            "client_context": "web6dot0",
            "open_media_host": "c.saavncdn.com",
            "endpoints": {"authorize_media": "song.generateAuthToken"},
        }
        duration_sec = 200
        seen_urls: list[str] = []

        def fake_bounded(url: str, *, headers=None, max_bytes=0, timeout=0, media=False):
            if "song.generateAuthToken" in url:
                return js.HttpResult(
                    status=200,
                    headers={"content-type": "application/json"},
                    body=json.dumps(
                        {
                            "status": "success",
                            "auth_url": (
                                "https://web.saavncdn.com/396/eca27e31e93211051fa0de18160ea825_320.mp4"
                                "?Expires=1794500000&Signature=dummySig&Key-Pair-Id=KP1"
                            ),
                        }
                    ).encode("utf-8"),
                )
            seen_urls.append(url)
            if url.startswith("https://aac.saavncdn.com/"):
                return js.HttpResult(status=403, headers={}, body=b"")
            tier_bytes = int((210 if "_320.mp4" in url else 96) * 1000 / 8 * duration_sec)
            return js.HttpResult(
                status=206,
                headers={
                    "content-type": "audio/mp4",
                    "content-range": f"bytes 0-8191/{tier_bytes}",
                },
                body=mp4_bytes,
            )

        with mock.patch.object(js, "_bounded_request", side_effect=fake_bounded):
            streams = js._probe_streams(
                api_cfg,
                api_cfg["endpoints"],
                SAMPLE_ENCRYPTED_PAYLOAD,
                duration_sec,
                [96, 320],
                open_media_host_override="c.saavncdn.com",
            )
        self.assertEqual(streams["decrypted_cdn_host"], "aac.saavncdn.com")
        self.assertTrue(any(u.startswith("https://aac.saavncdn.com/") for u in seen_urls))
        self.assertTrue(any(u.startswith("https://c.saavncdn.com/") for u in seen_urls))
        self.assertEqual(streams["verified_tiers"], [96, 160])

    def test_signed_auth_url_is_probed_when_open_cdn_fails_without_leaking_url(self) -> None:
        mp4_bytes = self._mp4_prefix()
        api_cfg = {
            "host": "www.jiosaavn.com",
            "open_media_host": "c.saavncdn.com",
            "endpoints": {"authorize_media": "song.generateAuthToken"},
        }
        duration_sec = 200
        seen_urls: list[str] = []
        signed_320 = (
            "https://web.saavncdn.com/396/eca27e31e93211051fa0de18160ea825_320.mp4"
            "?Expires=1794500000&Signature=secretSigToken&Key-Pair-Id=KPTEST"
        )

        def fake_bounded(url: str, *, headers=None, max_bytes=0, timeout=0, media=False):
            if "song.generateAuthToken" in url:
                return js.HttpResult(
                    status=200,
                    headers={"content-type": "application/json"},
                    body=json.dumps({"status": "success", "auth_url": signed_320}).encode("utf-8"),
                )
            seen_urls.append(url)
            if url == signed_320:
                total_bytes = int(320 * 1000 / 8 * duration_sec)
                return js.HttpResult(
                    status=206,
                    headers={"content-type": "audio/mp4", "content-range": f"bytes 0-8191/{total_bytes}"},
                    body=mp4_bytes,
                )
            return js.HttpResult(status=404, headers={}, body=b"")

        with mock.patch.object(js, "_bounded_request", side_effect=fake_bounded):
            streams = js._probe_streams(
                api_cfg,
                api_cfg["endpoints"],
                SAMPLE_ENCRYPTED_PAYLOAD,
                duration_sec,
                [320],
            )
        self.assertIn(signed_320, seen_urls)
        self.assertTrue(streams["playable_any_tier"])
        self.assertEqual(streams["verified_tiers"], [320])
        serialized = json.dumps(streams)
        self.assertNotIn("secretSigToken", serialized)
        self.assertNotIn("Expires=", serialized)
        self.assertNotIn("web.saavncdn.com", serialized)

    def test_malformed_duration_returns_failed_observation_instead_of_crashing(self) -> None:
        api_cfg = {"host": "www.jiosaavn.com", "endpoints": {"search_songs": "search.getResults"}}
        bad_payload = {
            "start": 1,
            "total": 1,
            "results": [
                {
                    "id": "s1",
                    "title": "Starboy",
                    "more_info": {
                        "duration": "200.0",
                        "encrypted_media_url": SAMPLE_ENCRYPTED_PAYLOAD,
                        "artistMap": {"primary_artists": [{"name": "The Weeknd"}]},
                    },
                }
            ],
        }
        with mock.patch.object(
            js,
            "_bounded_request",
            return_value=js.HttpResult(
                status=200,
                headers={"content-type": "application/json"},
                body=json.dumps(bad_payload).encode("utf-8"),
            ),
        ):
            result = js._probe_track_sentinel(
                {"name": "starboy", "query": "Starboy"},
                api_cfg,
                [96, 160, 320],
                attempts=1,
                retry_delay_seconds=0.0,
            )
        self.assertFalse(result["ok"])
        self.assertEqual(result["observation"]["error"], "invalid_duration_schema")

    def test_alternative_discovery_skips_all_blocked_failures(self) -> None:
        blocked_track = _track_entry("t1", ok=False, access_blocked=True)
        with mock.patch.object(js, "_probe_track_sentinel") as mocked_probe:
            alts = js._probe_protocol_alternatives(
                {
                    "api": {
                        "host": "www.jiosaavn.com",
                        "fallback_hosts": ["api.jiosaavn.com"],
                        "client_context": "web6dot0",
                        "fallback_contexts": ["android"],
                    },
                    "track_sentinels": [{"name": "t1", "query": "test"}],
                },
                [blocked_track],
                [_entity_entry("a1", ok=False, access_blocked=True)],
                [_entity_entry("al1", ok=False, access_blocked=True)],
            )
        self.assertEqual(alts, [])
        mocked_probe.assert_not_called()

    def test_mixed_outage_excludes_blocked_tracks_from_global_320_loss(self) -> None:
        config = json.loads((REPO_ROOT / "jiosaavn-sync" / "config.json").read_text(encoding="utf-8"))
        base_obs = _healthy_observation()
        mixed_obs = _healthy_observation()
        mixed_obs["tracks"][0] = _track_entry(
            "starboy-explicit-clean",
            expect_explicit_and_clean=True,
            explicit_and_clean_ok=True,
            tiers=(96, 160),
        )
        for idx in (1, 2):
            mixed_obs["tracks"][idx] = _track_entry(
                mixed_obs["tracks"][idx]["name"],
                ok=False,
                access_blocked=True,
                expect_explicit_and_clean=(idx == 1),
                error="search_failed:http_403",
            )
        decision = js._classify(_baseline_from(base_obs), mixed_obs, config)
        self.assertEqual(decision["decision"], "none")
        self.assertEqual(decision["material_changes"], [])

    def test_alternative_discovery_runs_for_quality_loss_and_requires_restored_320(self) -> None:
        base_obs = _healthy_observation()
        baseline = _baseline_from(base_obs)
        degraded_tracks = [
            _track_entry("starboy-explicit-clean", expect_explicit_and_clean=True, tiers=(96, 160)),
            _track_entry("blinding-lights", expect_explicit_and_clean=True, tiers=(96, 160)),
        ]
        cfg = {
            "api": {
                "host": "www.jiosaavn.com",
                "fallback_hosts": ["api.jiosaavn.com"],
                "client_context": "web6dot0",
                "fallback_contexts": ["android"],
            },
            "thresholds": {"required_track_regressions_for_repair": 2, "required_entity_regressions_for_repair": 2},
            "track_sentinels": [
                {"name": "starboy-explicit-clean", "query": "Starboy", "expect_explicit_and_clean": True},
                {"name": "blinding-lights", "query": "Blinding Lights", "expect_explicit_and_clean": True},
            ],
            "artist_sentinels": [],
            "album_sentinels": [],
        }
        provisional = js._classify(
            baseline,
            {"schema": js.SCHEMA_VERSION, "tracks": degraded_tracks, "artists": [], "albums": []},
            cfg,
        )
        self.assertEqual(provisional["decision"], "repair")

        def fake_track_probe(sentinel, *args, **kwargs):
            tiers = (96, 160, 320) if (
                kwargs.get("host_override") == "api.jiosaavn.com"
                and kwargs.get("context_override") == "android"
            ) else (96, 160)
            return _track_entry(
                sentinel["name"],
                expect_explicit_and_clean=True,
                tiers=tiers,
            )

        with mock.patch.object(js, "_probe_track_sentinel", side_effect=fake_track_probe):
            alts = js._probe_protocol_alternatives(
                cfg,
                degraded_tracks,
                [],
                [],
                baseline=baseline,
                provisional_decision=provisional,
            )
        passing = [alt for alt in alts if alt.get("all_passed")]
        self.assertEqual(len(passing), 1)
        self.assertEqual(passing[0]["api_host"], "api.jiosaavn.com")
        self.assertEqual(passing[0]["client_context"], "android")


class TestClassifierAndDeterministicFix(unittest.TestCase):
    def setUp(self) -> None:
        self.config = json.loads((REPO_ROOT / "jiosaavn-sync" / "config.json").read_text(encoding="utf-8"))

    def test_bootstrap_when_baseline_missing(self) -> None:
        decision = js._classify({"schema": 1, "observation": None}, _healthy_observation(), self.config)
        self.assertEqual(decision["decision"], "bootstrap")

        unhealthy = _healthy_observation()
        for t in unhealthy["tracks"]:
            t["ok"] = False
            t["observation"]["access_blocked"] = True
        decision_blocked = js._classify({"schema": 1, "observation": None}, unhealthy, self.config)
        self.assertEqual(decision_blocked["decision"], "blocked")

    def test_access_blocked_does_not_trigger_repair(self) -> None:
        base_obs = _healthy_observation()
        cur_obs = _healthy_observation()
        for track in cur_obs["tracks"]:
            track["ok"] = False
            track["observation"]["ok"] = False
            track["observation"]["access_blocked"] = True
            track["observation"]["error"] = "search_failed:http_403"
        decision = js._classify(_baseline_from(base_obs), cur_obs, self.config)
        self.assertEqual(decision["decision"], "none")
        self.assertEqual(decision["material_changes"], [])
        self.assertTrue(any("access-blocked" in line for line in decision["informational_changes"]))

    def test_single_track_regression_below_threshold_does_not_trigger_repair(self) -> None:
        base_obs = _healthy_observation()
        cur_obs = _healthy_observation()
        cur_obs["tracks"][1]["ok"] = False
        cur_obs["tracks"][1]["observation"]["ok"] = False
        cur_obs["tracks"][1]["observation"]["error"] = "no_playable_song_in_search"
        decision = js._classify(_baseline_from(base_obs), cur_obs, self.config)
        self.assertEqual(decision["decision"], "none")
        self.assertTrue(
            any("below repair threshold" in line for line in decision["informational_changes"])
        )

    def test_two_regressing_tracks_triggers_repair(self) -> None:
        base_obs = _healthy_observation()
        two_broken = _healthy_observation()
        for idx in (0, 1):
            two_broken["tracks"][idx]["ok"] = False
            two_broken["tracks"][idx]["observation"]["ok"] = False
            two_broken["tracks"][idx]["observation"]["error"] = "decrypt_failed"
        res_two = js._classify(_baseline_from(base_obs), two_broken, self.config)
        self.assertEqual(res_two["decision"], "repair")

    def test_global_320_tier_loss_triggers_repair(self) -> None:
        base_obs = _healthy_observation()
        no_320 = _healthy_observation()
        for idx in range(3):
            no_320["tracks"][idx] = _track_entry(
                no_320["tracks"][idx]["name"],
                tiers=(96, 160),
                explicit_and_clean_ok=(idx < 2),
            )
        res_320 = js._classify(_baseline_from(base_obs), no_320, self.config)
        self.assertEqual(res_320["decision"], "repair")
        self.assertTrue(any("320 kbps" in item for item in res_320["material_changes"]))

    def test_explicit_and_clean_coexistence_loss_triggers_repair(self) -> None:
        base_obs = _healthy_observation()
        lost_explicit = _healthy_observation()
        for idx in (0, 1):
            lost_explicit["tracks"][idx]["observation"]["explicit_and_clean_ok"] = False
        res_exp = js._classify(_baseline_from(base_obs), lost_explicit, self.config)
        self.assertEqual(res_exp["decision"], "repair")

    def test_deterministic_fixer_only_applies_proven_allowlisted_alternatives(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            kt_rel = "app/src/main/java/com/luc4n3x/levyra/data/hqaudio/jiosaavn/JioSaavnEndpoints.kt"
            kt_test_rel = "app/src/test/java/com/luc4n3x/levyra/data/hqaudio/jiosaavn/JioSaavnEndpointsTest.kt"
            cfg_rel = "jiosaavn-sync/config.json"
            for rel in (kt_rel, kt_test_rel, cfg_rel):
                (root / rel).parent.mkdir(parents=True, exist_ok=True)
            (root / kt_rel).write_text(
                'object JioSaavnEndpoints {\n'
                '    const val API_HOST = "www.jiosaavn.com"\n'
                '    const val OPEN_MEDIA_HOST = "aac.saavncdn.com"\n'
                '    private const val CLIENT_CONTEXT = "web6dot0"\n'
                '}\n',
                encoding="utf-8",
            )
            (root / kt_test_rel).write_text(
                'assertEquals("web6dot0", url.queryParameter("ctx"))\n'
                'assertEquals("https://aac.saavncdn.com/820/5ddb9a79a5218f85ca9bef170f3a461d_160.mp4", x)\n',
                encoding="utf-8",
            )
            (root / cfg_rel).write_text(
                json.dumps({"api": {"host": "www.jiosaavn.com", "client_context": "web6dot0"}}),
                encoding="utf-8",
            )

            untrusted_obs = {
                "protocol_alternatives": [
                    {
                        "api_host": "evil.example",
                        "client_context": "android",
                        "open_media_host": "aac.saavncdn.com",
                        "all_passed": True,
                    }
                ]
            }
            rejected = js.apply_deterministic_protocol_fix(untrusted_obs, root)
            self.assertFalse(rejected["applied"])

            valid_obs = {
                "protocol_alternatives": [
                    {
                        "api_host": "api.jiosaavn.com",
                        "client_context": "android",
                        "open_media_host": "c.saavncdn.com",
                        "all_passed": True,
                    }
                ]
            }
            applied = js.apply_deterministic_protocol_fix(valid_obs, root)
            self.assertTrue(applied["applied"])
            kt_after = (root / kt_rel).read_text(encoding="utf-8")
            self.assertIn('const val API_HOST = "api.jiosaavn.com"', kt_after)
            self.assertIn('const val OPEN_MEDIA_HOST = "c.saavncdn.com"', kt_after)
            self.assertIn('private const val CLIENT_CONTEXT = "android"', kt_after)
            kt_test_after = (root / kt_test_rel).read_text(encoding="utf-8")
            self.assertIn('assertEquals("android", url.queryParameter("ctx"))', kt_test_after)
            self.assertIn(
                '"https://c.saavncdn.com/820/5ddb9a79a5218f85ca9bef170f3a461d_160.mp4"',
                kt_test_after,
            )


class TestRepositoryContracts(unittest.TestCase):
    def test_workflow_enforces_verified_draft_pr_and_least_privilege(self) -> None:
        wf = (REPO_ROOT / ".github" / "workflows" / "jiosaavn-sync.yml").read_text(encoding="utf-8")
        self.assertIn("schedule:", wf)
        self.assertIn("cron:", wf)
        self.assertNotIn("pull_request_target", wf)
        self.assertNotIn("enable-auto-merge", wf)
        self.assertIn("draft: always-true", wf)
        self.assertIn("openai/codex-action@bdf19a4a223ec2549a3e2274a0cf61556bc07675", wf)
        self.assertIn("LEVYRA_CANARY_OPENROUTER_API_KEY", wf)
        self.assertIn("openrouter/free", wf)
        self.assertIn("steps.agent.outputs.available == 'true'", wf)
        self.assertIn("ai_quality_gate.py --profile full", wf)
        self.assertNotIn("--profile fast", wf)
        self.assertIn('git", "status", "--porcelain=v1", "-z"', wf)
        self.assertIn("jiosaavn_allowed_lines", wf)
        for heading in (
            "## Summary",
            "## Changes",
            "## Type of change",
            "## Scope",
            "## Testing",
            "## Screenshots or recordings",
            "## Risk and compatibility",
            "## Reviewer notes",
            "## Release note",
            "## Related issues",
            "## Checklist",
        ):
            self.assertGreaterEqual(wf.count(heading), 2, heading)
        self.assertIn("post_decision.get(\"decision\") == \"none\"", wf)
        for line in wf.splitlines():
            stripped = line.lstrip()
            if stripped.startswith("if:"):
                self.assertNotIn("secrets.", stripped)
        self.assertIn("production_changed == 'true'", wf)
        self.assertIn("scope_check.outputs.scope_ok == 'true'", wf)
        self.assertIn("validate_fix.outcome == 'success'", wf)

    def test_baseline_is_present_valid_and_free_of_secrets(self) -> None:
        baseline_path = REPO_ROOT / "jiosaavn-sync" / "baseline.json"
        self.assertTrue(baseline_path.is_file())
        raw = baseline_path.read_text(encoding="utf-8")
        self.assertNotIn("https://", raw)
        self.assertNotIn("http://", raw)
        self.assertNotIn("Signature=", raw)
        self.assertNotIn("Key-Pair-Id=", raw)
        self.assertNotIn("Expires=", raw)
        self.assertNotIn('"auth_url":', raw)
        self.assertNotIn(SAMPLE_ENCRYPTED_PAYLOAD, raw)
        self.assertIsNone(js.IPV4_RE.search(raw))
        data = json.loads(raw)
        obs = data.get("observation") or {}
        self.assertGreaterEqual(len(obs.get("tracks") or []), 3)
        self.assertGreaterEqual(len(obs.get("artists") or []), 2)
        self.assertGreaterEqual(len(obs.get("albums") or []), 2)


if __name__ == "__main__":
    unittest.main()
