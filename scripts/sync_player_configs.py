#!/usr/bin/env python3
"""Levyra multi-source YouTube player-config synchronization.

Fetches the Zemer and Faraday registries independently, validates and normalizes
both, compares them per player, selects a trusted configuration, and publishes it
into the Android assets atomically. A failed source never corrupts the repository
and an unchanged trusted configuration never produces a repository modification.

Source outages are handled, reported and exit successfully; only an internal
pipeline failure returns a non-zero status.
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
import uuid
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Mapping, Sequence

if __package__ in (None, ""):
    sys.path.insert(0, str(Path(__file__).resolve().parent))

from player_config_pipeline import (  # noqa: E402
    DECISION_KEEP_CURRENT,
    DECISION_KEEP_LAST_KNOWN_GOOD,
    DECISION_UPDATED,
    MAX_SOURCE_BYTES,
    ROLE_PRIMARY,
    ROLE_SECONDARY,
    STATUS_OK,
    SELECTION_LAST_KNOWN_GOOD,
    FetchOutcome,
    FaradaySource,
    PlayerConfig,
    PipelineError,
    PlayerConfigSource,
    SelectionResult,
    SourceRejected,
    SourceSnapshot,
    ZemerSource,
    configurations_equal,
    content_hash,
    decode_json_object,
    parse_players_registry,
    render_registry,
    select_configurations,
)

try:
    from youtube_canary import CanaryError, _bounded_request
except ImportError as error:  # pragma: no cover - only reached on a broken checkout
    raise SystemExit(f"Unable to import the YouTube canary HTTP helpers: {error}") from error

PLAYER_CONFIGS_ASSET = "player_configs.json"
PLAYER_DATES_ASSET = "player_dates.json"
PLAYER_META_ASSET = "player_configs.meta.json"

USER_AGENT = "Levyra/2.3.20 player-config-sync"
FETCH_ATTEMPTS = 3
FETCH_RETRY_DELAY_SECONDS = 2.0


def _utcnow() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def _fetch_once(url: str) -> FetchOutcome:
    fetched_at = _utcnow()
    try:
        result = _bounded_request(
            url,
            headers={"Accept": "application/json", "User-Agent": USER_AGENT},
            max_bytes=MAX_SOURCE_BYTES,
        )
    except CanaryError as error:
        return FetchOutcome(url, None, b"", f"transport failure: {error}"[:300], fetched_at)
    if result.status < 200 or result.status >= 300:
        return FetchOutcome(url, result.status, b"", f"HTTP {result.status}", fetched_at)
    return FetchOutcome(url, result.status, result.body, None, fetched_at)


def _retryable(outcome: FetchOutcome) -> bool:
    return outcome.status is None or outcome.status == 429 or 500 <= outcome.status < 600


def http_fetch(url: str) -> FetchOutcome:
    outcome = _fetch_once(url)
    for attempt in range(2, FETCH_ATTEMPTS + 1):
        if outcome.ok or not _retryable(outcome):
            return outcome
        time.sleep(FETCH_RETRY_DELAY_SECONDS * (attempt - 1))
        outcome = _fetch_once(url)
    return outcome


@dataclass(frozen=True)
class PipelineReport:
    decision: str
    selection: str
    changed: bool
    generated_at: str
    content_hash: str
    player_count: int
    counts: Mapping[str, int]
    conflicts: tuple[str, ...]
    notes: tuple[str, ...]
    source_health: Mapping[str, Mapping[str, object]]
    written_assets: tuple[str, ...]

    def to_dict(self) -> dict[str, object]:
        return {
            "schema": 1,
            "generatedAt": self.generated_at,
            "decision": self.decision,
            "selection": self.selection,
            "changed": self.changed,
            "contentHash": self.content_hash,
            "playerCount": self.player_count,
            "counts": dict(self.counts),
            "conflicts": list(self.conflicts),
            "notes": list(self.notes),
            "sources": {key: dict(value) for key, value in self.source_health.items()},
            "writtenAssets": list(self.written_assets),
        }


def _read_text(path: Path) -> str | None:
    try:
        return path.read_text(encoding="utf-8")
    except (OSError, UnicodeDecodeError):
        return None


def _load_current_players(assets_dir: Path) -> tuple[dict[str, PlayerConfig], tuple[str, ...]]:
    text = _read_text(assets_dir / PLAYER_CONFIGS_ASSET)
    if text is None:
        return {}, ()
    try:
        players = parse_players_registry(text.encode("utf-8"))
    except SourceRejected:
        return {}, ()
    return players, tuple(players.keys())


def _load_current_dates(assets_dir: Path) -> dict[str, str]:
    path = assets_dir / PLAYER_DATES_ASSET
    try:
        body = path.read_bytes()
    except OSError:
        return {}
    try:
        root = decode_json_object(body)
    except SourceRejected:
        return {}
    return {key: value for key, value in root.items() if isinstance(key, str) and isinstance(value, str)}


def _render_and_verify(selection: SelectionResult) -> str:
    text = render_registry(selection.players, selection.order)
    reparsed = parse_players_registry(text.encode("utf-8"))
    if not configurations_equal(reparsed, selection.players):
        raise PipelineError("rendered player configuration failed round-trip verification")
    return text


def _publish_atomically(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temp = path.with_name(f".{path.name}.{uuid.uuid4().hex}.tmp")
    try:
        with open(temp, "w", encoding="utf-8", newline="\n") as handle:
            handle.write(text)
            handle.flush()
            os.fsync(handle.fileno())
        os.replace(temp, path)
    except OSError as error:
        raise PipelineError(f"unable to publish {path.name}: {error}") from error
    finally:
        if temp.exists():
            try:
                temp.unlink()
            except OSError:
                pass


def _build_meta(report: PipelineReport) -> dict[str, object]:
    return {
        "schema": 1,
        "generatedAt": report.generated_at,
        "decision": report.decision,
        "source": report.selection,
        "contentHash": report.content_hash,
        "playerCount": report.player_count,
        "counts": dict(report.counts),
        "conflicts": list(report.conflicts),
        "sources": {key: dict(value) for key, value in report.source_health.items()},
    }


def _source_line(snapshot: SourceSnapshot) -> str:
    if snapshot.status == STATUS_OK:
        return (
            f"- **{snapshot.source_id}** ({snapshot.role}): ok - {len(snapshot.players)} players "
            f"- `{snapshot.content_hash[:16]}`"
        )
    return f"- **{snapshot.source_id}** ({snapshot.role}): {snapshot.status} - {snapshot.detail}"


def render_summary(report: PipelineReport, snapshots: Sequence[SourceSnapshot]) -> str:
    lines = [
        "# Player config sync",
        "",
        f"- Decision: **{report.decision}**",
        f"- Selection: `{report.selection}`",
        f"- Players: {report.player_count}",
        f"- Content hash: `{report.content_hash}`",
        "",
    ]
    for snapshot in snapshots:
        label = snapshot.source_id.capitalize()
        lines.append(f"{label}: {snapshot.status}")
    lines.append(f"Decision: {report.decision}")
    lines += ["", "## Counts", ""]
    if report.counts:
        for key in sorted(report.counts):
            lines.append(f"- {key}: {report.counts[key]}")
    else:
        lines.append("- None")
    lines += ["", "## Sources", ""]
    lines.extend(_source_line(snapshot) for snapshot in snapshots)
    lines += ["", "## Notes", ""]
    lines.extend(f"- {note}" for note in report.notes or ("None",))
    if report.conflicts:
        lines += ["", "## Conflicting players (kept last known good)", ""]
        lines.extend(f"- `{key}`" for key in report.conflicts)
    lines += [
        "",
        "## Trust policy",
        "",
        "1. A configuration confirmed by both independent sources.",
        "2. A valid fresh configuration from the primary source (Zemer).",
        "3. A valid configuration from the secondary source (Faraday) when the primary lacks it.",
        "4. The existing last known good configuration.",
        "5. Omission when no safe value exists.",
        "",
        "A newer timestamp never overrides validation or agreement. When the sources conflict, "
        "the last known good entry is kept.",
        "",
        "## Security",
        "",
        "- Upstream JSON is treated as untrusted, size-bounded and structurally validated before use.",
        "- No upstream JavaScript is executed and no credential, visitor data, cookie, token or "
        "signed media URL is read or persisted.",
        "- Repository assets are only replaced atomically after a candidate passes validation.",
        "",
    ]
    return "\n".join(lines)


def run_pipeline(
    *,
    sources: Sequence[PlayerConfigSource],
    fetch,
    assets_dir: Path,
    generated_at: str | None = None,
) -> tuple[PipelineReport, tuple[SourceSnapshot, ...]]:
    generated_at = generated_at or _utcnow()
    assets_dir = Path(assets_dir)
    cache: dict[str, FetchOutcome] = {}

    def memo(url: str) -> FetchOutcome:
        if url not in cache:
            cache[url] = fetch(url)
        return cache[url]

    snapshots: list[SourceSnapshot] = []
    for source in sources:
        if source.config_document is None:
            snapshots.append(
                SourceSnapshot(
                    source_id=source.id,
                    role=source.role,
                    status="unavailable",
                    detail="source unavailable",
                    fetched_at=generated_at,
                )
            )
            continue
        config_outcome = memo(source.config_document.url)
        metadata_outcomes = {document.name: memo(document.url) for document in source.metadata_documents}
        snapshots.append(source.snapshot(config_outcome, metadata_outcomes, generated_at))

    primary = next((item for item in snapshots if item.role == ROLE_PRIMARY), snapshots[0])
    secondary = next((item for item in snapshots if item.role == ROLE_SECONDARY), None)
    if secondary is None:
        secondary = SourceSnapshot(
            source_id="secondary",
            role=ROLE_SECONDARY,
            status="unavailable",
            detail="source unavailable",
            fetched_at=generated_at,
        )

    last_known_good, last_known_good_order = _load_current_players(assets_dir)
    current_dates = _load_current_dates(assets_dir)

    selection = select_configurations(primary, secondary, last_known_good, last_known_good_order)

    players_changed = not configurations_equal(selection.players, last_known_good)

    dates_raw: str | None = None
    dates_changed = False
    if primary.status == STATUS_OK:
        candidate_dates = primary.metadata.get("dates")
        if isinstance(candidate_dates, dict) and candidate_dates:
            dates_raw = primary.document_text.get("player_dates")
            if dates_raw is not None and candidate_dates != current_dates:
                dates_changed = True

    report_sources = {snapshot.source_id: snapshot.health() for snapshot in snapshots}
    base = dict(
        selection=selection.selection,
        generated_at=generated_at,
        content_hash=content_hash(selection.players),
        player_count=len(selection.players),
        counts=dict(selection.counts),
        conflicts=selection.conflicts,
        notes=selection.notes,
        source_health=report_sources,
    )

    snapshot_tuple = tuple(snapshots)

    if not selection.players:
        return (
            PipelineReport(
                decision=DECISION_KEEP_LAST_KNOWN_GOOD,
                changed=False,
                written_assets=(),
                **base,
            ),
            snapshot_tuple,
        )

    if not players_changed and not dates_changed:
        decision = (
            DECISION_KEEP_LAST_KNOWN_GOOD
            if selection.selection == SELECTION_LAST_KNOWN_GOOD
            else DECISION_KEEP_CURRENT
        )
        return (
            PipelineReport(decision=decision, changed=False, written_assets=(), **base),
            snapshot_tuple,
        )

    written: list[str] = []
    if players_changed:
        _publish_atomically(assets_dir / PLAYER_CONFIGS_ASSET, _render_and_verify(selection))
        written.append(PLAYER_CONFIGS_ASSET)
    if dates_changed and dates_raw is not None:
        _publish_atomically(assets_dir / PLAYER_DATES_ASSET, dates_raw)
        written.append(PLAYER_DATES_ASSET)

    report = PipelineReport(
        decision=DECISION_UPDATED,
        changed=True,
        written_assets=tuple([*written, PLAYER_META_ASSET]),
        **base,
    )
    meta_text = json.dumps(_build_meta(report), indent=2, sort_keys=True) + "\n"
    _publish_atomically(assets_dir / PLAYER_META_ASSET, meta_text)
    return report, snapshot_tuple


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Synchronize Levyra YouTube player configurations.")
    parser.add_argument("--assets-dir", default="app/src/main/assets")
    parser.add_argument("--report", default="")
    parser.add_argument("--summary", default="")
    parser.add_argument("--github-output", default="")
    return parser


def main(argv: Sequence[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(list(argv) if argv is not None else None)

    try:
        report, snapshots = run_pipeline(
            sources=(ZemerSource(), FaradaySource()),
            fetch=http_fetch,
            assets_dir=Path(args.assets_dir),
        )
    except PipelineError as error:
        print(f"player config pipeline failed: {error}", file=sys.stderr)
        return 2

    payload = report.to_dict()
    if args.report:
        report_path = Path(args.report)
        report_path.parent.mkdir(parents=True, exist_ok=True)
        report_path.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    if args.summary:
        summary_path = Path(args.summary)
        summary_path.parent.mkdir(parents=True, exist_ok=True)
        summary_path.write_text(render_summary(report, snapshots), encoding="utf-8")
    if args.github_output:
        with open(args.github_output, "a", encoding="utf-8") as handle:
            handle.write(f"changed={'true' if report.changed else 'false'}\n")
            handle.write(f"decision={report.decision}\n")

    print(
        f"decision={report.decision} selection={report.selection} "
        f"changed={report.changed} players={report.player_count}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
