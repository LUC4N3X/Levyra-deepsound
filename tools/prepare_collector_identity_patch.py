from pathlib import Path
import re

path = Path("tools/apply_isrc_release_youtube_auth.py")
text = path.read_text()

spotify_section = r'''# ---------------------------------------------------------------------------
# Spotify collector: optional batch metadata for ISRC and album type
# ---------------------------------------------------------------------------
spotify_path = 'tools/levyra-editorial/levyra_editorial/spotify.py'
spotify = read(spotify_path)
spotify = replace_once(
    spotify,
    '        self._playlist_pages: dict[tuple[str, int], dict[str, Any]] = {}\n',
    '        self._playlist_pages: dict[tuple[str, int], dict[str, Any]] = {}\n        self._track_metadata: dict[str, Mapping[str, Any]] = {}\n',
    'spotify track metadata cache'
)
spotify = replace_once(
    spotify,
    '        return output\n\n    def close(self) -> None:\n',
    ''' + "'''" + r'''        return output

    def enrich_track_metadata(self, items: list[dict[str, Any]]) -> list[dict[str, Any]]:
        """Best-effort ISRC and release metadata without weakening Pathfinder reads."""
        if self._access_token is None:
            self.authenticate()
        ids = [
            str(item.get("track", {}).get("id") or "").strip()
            for item in items
            if isinstance(item.get("track"), Mapping)
        ]
        missing = [track_id for track_id in dict.fromkeys(ids) if track_id and track_id not in self._track_metadata]
        for offset in range(0, len(missing), 50):
            chunk = missing[offset : offset + 50]
            if not chunk:
                continue
            try:
                response = self._session.get(
                    f"{API_BASE_URL}/tracks",
                    params={"ids": ",".join(chunk)},
                    headers=self._api_headers(),
                    timeout=self._timeout,
                )
                if response.status_code == 401:
                    self.authenticate()
                    response = self._session.get(
                        f"{API_BASE_URL}/tracks",
                        params={"ids": ",".join(chunk)},
                        headers=self._api_headers(),
                        timeout=self._timeout,
                    )
                if response.status_code >= 400:
                    LOGGER.warning(
                        "Spotify track metadata enrichment skipped after HTTP %s.",
                        response.status_code,
                    )
                    continue
                payload = response.json()
            except (requests.RequestException, ValueError, AuthenticationError) as error:
                LOGGER.warning(
                    "Spotify track metadata enrichment skipped: %s.",
                    _safe_authentication_failure(error),
                )
                continue
            raw_tracks = payload.get("tracks") if isinstance(payload, Mapping) else None
            if not isinstance(raw_tracks, list):
                continue
            for raw_track in raw_tracks:
                if not isinstance(raw_track, Mapping):
                    continue
                track_id = _string(raw_track.get("id"))
                if track_id:
                    self._track_metadata[track_id] = raw_track

        for item in items:
            track = item.get("track")
            if not isinstance(track, dict):
                continue
            enriched = self._track_metadata.get(str(track.get("id") or ""))
            if not isinstance(enriched, Mapping):
                continue
            external_ids = enriched.get("external_ids")
            if isinstance(external_ids, Mapping):
                track["external_ids"] = dict(external_ids)
            for key in ("track_number", "disc_number"):
                if isinstance(enriched.get(key), int):
                    track[key] = enriched[key]
            album = track.get("album")
            enriched_album = enriched.get("album")
            if isinstance(album, dict) and isinstance(enriched_album, Mapping):
                for key in ("album_type", "total_tracks", "release_date"):
                    value = enriched_album.get(key)
                    if value is not None:
                        album[key] = value
        return items

    def _api_headers(self) -> dict[str, str]:
        if self._access_token is None:
            raise AuthenticationError("The editorial source is not authenticated.")
        headers = {
            "Authorization": f"Bearer {self._access_token}",
            "Accept": "application/json",
        }
        if self._client_id:
            headers["Client-Id"] = self._client_id
        return headers

    def close(self) -> None:
''' + "'''" + r''',
    'spotify metadata enrichment'
)
write(spotify_path, spotify)
'''

pattern = r"# ---------------------------------------------------------------------------\n# Spotify collector: batch track metadata for ISRC and album type\n# ---------------------------------------------------------------------------\n.*?write\(spotify_path, spotify\)\n"
text, count = re.subn(pattern, lambda _: spotify_section, text, count=1, flags=re.S)
if count != 1:
    raise SystemExit("Spotify enrichment patch section not found")

collector_hook = r'''collector = replace_once(
    collector,
    '        raw_items = client.iter_playlist_items(playlist_id)\n        tracks = normalize_playlist_items(raw_items)\n',
    ''' + "'''" + r'''        raw_items = client.iter_playlist_items(playlist_id)
        enricher = getattr(client, "enrich_track_metadata", None)
        if callable(enricher):
            try:
                raw_items = enricher(raw_items)
            except Exception as error:
                LOGGER.warning("Optional track metadata enrichment skipped: %s", type(error).__name__)
        tracks = normalize_playlist_items(raw_items)
''' + "'''" + r''',
    'collector optional metadata enrichment'
)
write(collector_path, collector)'''
text = text.replace("write(collector_path, collector)", collector_hook, 1)

new_test = r'''if 'test_catalog_keeps_public_isrc_and_release_type' not in test:
    test += r''' + "'''" + r'''


def test_catalog_keeps_public_isrc_and_release_type() -> None:
    item = FakeClient().iter_playlist_items("playlist12345")[0]
    item["track"]["external_ids"] = {"isrc": "ITB002000001"}
    item["track"]["album"]["album_type"] = "album"
    item["track"]["album"]["total_tracks"] = 12
    public = normalize_playlist_items([item])[0].to_dict()
    assert public["isrc"] == "ITB002000001"
    assert public["album"]["type"] == "album"
    assert public["album"]["totalTracks"] == 12
''' + "'''" + r'''
write(test_path, test)'''
text, count = re.subn(
    r"if 'test_catalog_keeps_public_isrc_and_release_type' not in test:\n.*?write\(test_path, test\)",
    lambda _: new_test,
    text,
    count=1,
    flags=re.S,
)
if count != 1:
    raise SystemExit("Collector identity test patch not found")

path.write_text(text)
