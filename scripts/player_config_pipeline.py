#!/usr/bin/env python3
"""Source-agnostic YouTube player-configuration pipeline for Levyra.

The pipeline turns independent upstream registries into one internal
representation, validates every candidate, compares the sources and selects a
trusted configuration using a conservative policy. It never trusts a payload
merely because an HTTP request returned 200, and it never destroys the last
known good configuration during a failed refresh.

Every upstream payload is untrusted input: JSON is parsed defensively, sizes and
entry counts are bounded, and identifiers are validated against the same shape
the Android decoder already enforces. No upstream JavaScript is ever executed
and no credential, visitor data, cookie, token or signed media URL is read or
persisted.
"""

from __future__ import annotations

import hashlib
import json
import re
from dataclasses import dataclass, field, replace
from typing import Any, Mapping, Sequence

SCHEMA_VERSION = 1

MAX_SOURCE_BYTES = 2 * 1024 * 1024
MIN_SOURCE_BYTES = 32
MAX_PLAYERS = 20_000
MAX_ALIASES_PER_PLAYER = 16
MAX_SIGNATURE_LENGTH = 64
MAX_SIGNATURE_TIMESTAMP = 100_000_000
MAX_METADATA_ENTRIES = 40_000

HASH_RE = re.compile(r"^[a-f0-9]{8}$")
SIGNATURE_RE = re.compile(r"^[A-Za-z0-9$_]{1,8}\(\d{1,7},\d{1,7},INPUT\)$")
N_CLASS_RE = re.compile(r"^[A-Za-z0-9$_]{1,8}$")
DATE_RE = re.compile(r"^\d{4}-\d{2}-\d{2}$")

ROLE_PRIMARY = "primary"
ROLE_SECONDARY = "secondary"

STATUS_OK = "ok"
STATUS_UNAVAILABLE = "unavailable"
STATUS_INVALID = "invalid"

VERDICT_CONFIRMED = "CONFIRMED"
VERDICT_SINGLE_PRIMARY = "SINGLE_SOURCE_PRIMARY"
VERDICT_SINGLE_SECONDARY = "SINGLE_SOURCE_SECONDARY"
VERDICT_CONFLICTING = "CONFLICTING"
VERDICT_LAST_KNOWN_GOOD = "LAST_KNOWN_GOOD"
VERDICT_OMITTED = "OMITTED"

SELECTION_BOTH_CONFIRMED = "BOTH_CONFIRMED"
SELECTION_PRIMARY = "ZEMER"
SELECTION_SECONDARY = "FARADAY"
SELECTION_MIXED = "MIXED"
SELECTION_LAST_KNOWN_GOOD = "LAST_KNOWN_GOOD"

DECISION_UPDATED = "UPDATED"
DECISION_KEEP_CURRENT = "KEEP_CURRENT"
DECISION_KEEP_LAST_KNOWN_GOOD = "KEEP_LAST_KNOWN_GOOD"

ZEMER_CONFIG_URL = (
    "https://raw.githubusercontent.com/ZemerTeam/zemer-cipher/master/"
    "library/src/main/assets/player_configs.json"
)
ZEMER_DATES_URL = "https://raw.githubusercontent.com/ZemerTeam/zemer-cipher/master/player_dates.json"
FARADAY_CONFIG_URL = (
    "https://raw.githubusercontent.com/MetrolistGroup/faraday/master/registry/player_configs.json"
)
FARADAY_REGISTRY_URL = (
    "https://raw.githubusercontent.com/MetrolistGroup/faraday/master/registry/player-registry.json"
)


class PipelineError(RuntimeError):
    """An internal pipeline or programming failure.

    Upstream outages and invalid upstream payloads are never reported through
    this exception; they are represented as unhealthy source snapshots so the
    pipeline can keep the last known good configuration.
    """


class SourceRejected(ValueError):
    """A fetched upstream payload failed validation and must be treated as unhealthy."""


@dataclass(frozen=True)
class FetchOutcome:
    url: str
    status: int | None
    body: bytes
    error: str | None
    fetched_at: str

    @property
    def ok(self) -> bool:
        return self.error is None and self.status is not None and 200 <= self.status < 300


@dataclass(frozen=True)
class PlayerConfig:
    primary_hash: str
    signature: str
    n_class: str
    signature_timestamp: int
    aliases: tuple[str, ...] = ()

    def capability(self) -> tuple[str, str, int]:
        return (self.signature, self.n_class, self.signature_timestamp)

    def with_aliases(self, aliases: Sequence[str]) -> "PlayerConfig":
        return replace(self, aliases=tuple(aliases))

    def entry(self) -> dict[str, Any]:
        return {
            "sig": self.signature,
            "nClass": self.n_class,
            "sts": self.signature_timestamp,
            "aliases": list(self.aliases),
        }


@dataclass(frozen=True)
class SourceDocument:
    name: str
    url: str
    required: bool = True


@dataclass(frozen=True)
class SourceSnapshot:
    source_id: str
    role: str
    status: str
    detail: str
    fetched_at: str
    players: Mapping[str, PlayerConfig] = field(default_factory=dict)
    player_order: tuple[str, ...] = ()
    content_hash: str = ""
    document_hashes: Mapping[str, str] = field(default_factory=dict)
    metadata: Mapping[str, Any] = field(default_factory=dict)
    document_text: Mapping[str, str] = field(default_factory=dict, compare=False, repr=False)

    @property
    def healthy(self) -> bool:
        return self.status == STATUS_OK and bool(self.players)

    def health(self) -> dict[str, Any]:
        return {
            "source": self.source_id,
            "role": self.role,
            "status": self.status,
            "detail": self.detail[:240],
            "fetchedAt": self.fetched_at,
            "contentHash": self.content_hash,
            "playerCount": len(self.players) if self.status == STATUS_OK else 0,
            "registryUpdatedAt": self.metadata.get("registryUpdatedAt"),
        }


def sha256_hex(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def content_hash(players: Mapping[str, PlayerConfig]) -> str:
    digest = hashlib.sha256()
    for key in sorted(players):
        config = players[key]
        digest.update(key.encode("utf-8"))
        digest.update(b"|")
        digest.update(config.signature.encode("utf-8"))
        digest.update(b"|")
        digest.update(config.n_class.encode("utf-8"))
        digest.update(b"|")
        digest.update(str(config.signature_timestamp).encode("ascii"))
        digest.update(b"|")
        digest.update(",".join(sorted(config.aliases)).encode("utf-8"))
        digest.update(b"\n")
    return digest.hexdigest()


def decode_json_object(body: bytes, *, max_bytes: int = MAX_SOURCE_BYTES) -> dict[str, Any]:
    """Defensively decode a bounded JSON object from an untrusted payload."""
    if not isinstance(body, (bytes, bytearray)):
        raise SourceRejected("payload is not bytes")
    if len(body) > max_bytes:
        raise SourceRejected(f"payload exceeds {max_bytes} bytes")
    if len(body) < MIN_SOURCE_BYTES:
        raise SourceRejected("payload is implausibly small")
    if body.lstrip()[:1] == b"<":
        raise SourceRejected("payload looks like an HTML/error page, not JSON")
    try:
        text = body.decode("utf-8")
    except UnicodeDecodeError as error:
        raise SourceRejected(f"payload is not valid UTF-8: {error}") from error
    try:
        value = json.loads(text)
    except json.JSONDecodeError as error:
        raise SourceRejected(f"payload is not valid JSON: {error}") from error
    if not isinstance(value, dict):
        raise SourceRejected("payload root is not a JSON object")
    return value


def parse_player_entry(primary_hash: str, raw_entry: Any) -> PlayerConfig:
    if not isinstance(raw_entry, dict):
        raise SourceRejected(f"entry {primary_hash} is not an object")
    signature = raw_entry.get("sig")
    n_class = raw_entry.get("nClass")
    timestamp = raw_entry.get("sts")
    aliases = raw_entry.get("aliases", [])
    if (
        not isinstance(signature, str)
        or len(signature) > MAX_SIGNATURE_LENGTH
        or not SIGNATURE_RE.fullmatch(signature)
    ):
        raise SourceRejected(f"entry {primary_hash} has an invalid signature expression")
    if not isinstance(n_class, str) or not N_CLASS_RE.fullmatch(n_class):
        raise SourceRejected(f"entry {primary_hash} has an invalid nClass")
    if (
        not isinstance(timestamp, int)
        or isinstance(timestamp, bool)
        or not 1 <= timestamp <= MAX_SIGNATURE_TIMESTAMP
    ):
        raise SourceRejected(f"entry {primary_hash} has an invalid signature timestamp")
    if not isinstance(aliases, list):
        raise SourceRejected(f"entry {primary_hash} aliases is not a list")
    if len(aliases) > MAX_ALIASES_PER_PLAYER:
        raise SourceRejected(f"entry {primary_hash} has too many aliases")
    clean_aliases: list[str] = []
    for alias in aliases:
        if not isinstance(alias, str) or not HASH_RE.fullmatch(alias):
            raise SourceRejected(f"entry {primary_hash} has an invalid alias")
        if alias == primary_hash:
            raise SourceRejected(f"entry {primary_hash} aliases itself")
        if alias in clean_aliases:
            raise SourceRejected(f"entry {primary_hash} repeats alias {alias}")
        clean_aliases.append(alias)
    return PlayerConfig(primary_hash, signature, n_class, timestamp, tuple(clean_aliases))


def parse_players_registry(body: bytes) -> dict[str, PlayerConfig]:
    """Parse the shared ``schemaVersion``/``players`` registry into an internal map."""
    root = decode_json_object(body)
    schema = root.get("schemaVersion")
    if not isinstance(schema, int) or isinstance(schema, bool) or schema != SCHEMA_VERSION:
        raise SourceRejected(f"unsupported schemaVersion {schema!r}")
    players = root.get("players")
    if not isinstance(players, dict):
        raise SourceRejected("players is missing or not an object")
    if not players:
        raise SourceRejected("players is empty")
    if len(players) > MAX_PLAYERS:
        raise SourceRejected(f"players exceeds {MAX_PLAYERS} entries")

    parsed: dict[str, PlayerConfig] = {}
    claimed: dict[str, str] = {}
    for raw_hash, raw_entry in players.items():
        if not isinstance(raw_hash, str) or not HASH_RE.fullmatch(raw_hash):
            raise SourceRejected(f"invalid player hash {raw_hash!r}")
        config = parse_player_entry(raw_hash, raw_entry)
        for key in (raw_hash, *config.aliases):
            owner = claimed.get(key)
            if owner is not None:
                raise SourceRejected(f"duplicate hash/alias {key!r} shared by {owner} and {raw_hash}")
            claimed[key] = raw_hash
        parsed[raw_hash] = config
    return parsed


def render_registry(players: Mapping[str, PlayerConfig], order: Sequence[str] | None = None) -> str:
    """Render the canonical repository asset format, preserving a stable order."""
    keys: list[str] = []
    seen: set[str] = set()

    def push(key: str) -> None:
        if key in players and key not in seen:
            seen.add(key)
            keys.append(key)

    for key in order or ():
        push(key)
    for key in players:
        push(key)

    lines = ["{", f'  "schemaVersion": {SCHEMA_VERSION},', '  "players": {']
    for index, key in enumerate(keys):
        config = players[key]
        aliases = "[" + ", ".join(json.dumps(alias) for alias in config.aliases) + "]"
        comma = "," if index < len(keys) - 1 else ""
        lines.append(
            "    %s: { \"sig\": %s, \"nClass\": %s, \"sts\": %s, \"aliases\": %s }%s"
            % (
                json.dumps(key),
                json.dumps(config.signature),
                json.dumps(config.n_class),
                json.dumps(config.signature_timestamp),
                aliases,
                comma,
            )
        )
    lines.append("  }")
    lines.append("}")
    return "\n".join(lines) + "\n"


def configurations_equal(left: Mapping[str, PlayerConfig], right: Mapping[str, PlayerConfig]) -> bool:
    if left.keys() != right.keys():
        return False
    return all(left[key] == right[key] for key in left)


class PlayerConfigSource:
    """Base class for an independent upstream player-config source.

    Adding a third source only requires a subclass that names its documents and
    parses its payload into the internal :class:`PlayerConfig` representation.
    """

    id: str = ""
    role: str = ""
    config_document: SourceDocument
    metadata_documents: tuple[SourceDocument, ...] = ()

    def parse_config(self, body: bytes) -> Mapping[str, PlayerConfig]:
        raise NotImplementedError

    def parse_metadata(self, documents: Mapping[str, bytes]) -> Mapping[str, Any]:
        return {}

    def snapshot(
        self,
        config: FetchOutcome,
        metadata: Mapping[str, FetchOutcome],
        fetched_at: str,
    ) -> SourceSnapshot:
        document_hashes: dict[str, str] = {}
        document_text: dict[str, str] = {}
        if config.body:
            document_hashes[self.config_document.name] = sha256_hex(config.body)
        for name, outcome in metadata.items():
            if outcome.body:
                document_hashes[name] = sha256_hex(outcome.body)
                try:
                    document_text[name] = outcome.body.decode("utf-8")
                except UnicodeDecodeError:
                    pass

        if not config.ok:
            detail = config.error or f"HTTP {config.status}"
            return SourceSnapshot(
                source_id=self.id,
                role=self.role,
                status=STATUS_UNAVAILABLE,
                detail=detail,
                fetched_at=fetched_at,
                document_hashes=document_hashes,
            )

        try:
            players = self.parse_config(config.body)
        except SourceRejected as error:
            return SourceSnapshot(
                source_id=self.id,
                role=self.role,
                status=STATUS_INVALID,
                detail=str(error),
                fetched_at=fetched_at,
                document_hashes=document_hashes,
            )

        metadata_values: dict[str, Any] = {}
        warnings: list[str] = []
        metadata_bodies = {name: outcome.body for name, outcome in metadata.items() if outcome.ok}
        if metadata_bodies:
            try:
                metadata_values.update(self.parse_metadata(metadata_bodies))
            except SourceRejected as error:
                warnings.append(f"metadata ignored: {error}")
        if warnings:
            metadata_values["warnings"] = warnings

        return SourceSnapshot(
            source_id=self.id,
            role=self.role,
            status=STATUS_OK,
            detail="",
            fetched_at=fetched_at,
            players=players,
            player_order=tuple(players.keys()),
            content_hash=document_hashes.get(self.config_document.name, ""),
            document_hashes=document_hashes,
            metadata=metadata_values,
            document_text=document_text,
        )


class ZemerSource(PlayerConfigSource):
    id = "zemer"
    role = ROLE_PRIMARY
    config_document = SourceDocument("player_configs", ZEMER_CONFIG_URL)
    metadata_documents = (SourceDocument("player_dates", ZEMER_DATES_URL, required=False),)

    def parse_config(self, body: bytes) -> Mapping[str, PlayerConfig]:
        return parse_players_registry(body)

    def parse_metadata(self, documents: Mapping[str, bytes]) -> Mapping[str, Any]:
        body = documents.get("player_dates")
        if body is None:
            return {}
        root = decode_json_object(body)
        if len(root) > MAX_METADATA_ENTRIES:
            raise SourceRejected("player_dates exceeds the entry limit")
        dates: dict[str, str] = {}
        for key, value in root.items():
            if not isinstance(key, str) or not HASH_RE.fullmatch(key):
                raise SourceRejected(f"player_dates has an invalid hash {key!r}")
            if not isinstance(value, str) or not DATE_RE.fullmatch(value):
                raise SourceRejected(f"player_dates has an invalid date for {key}")
            dates[key] = value
        return {"dates": dates}


class FaradaySource(PlayerConfigSource):
    id = "faraday"
    role = ROLE_SECONDARY
    config_document = SourceDocument("player_configs", FARADAY_CONFIG_URL)
    metadata_documents = (SourceDocument("player_registry", FARADAY_REGISTRY_URL, required=False),)

    def parse_config(self, body: bytes) -> Mapping[str, PlayerConfig]:
        return parse_players_registry(body)

    def parse_metadata(self, documents: Mapping[str, bytes]) -> Mapping[str, Any]:
        body = documents.get("player_registry")
        if body is None:
            return {}
        root = decode_json_object(body)
        registry_updated = str(root.get("updatedAt") or "")[:40]
        known: dict[str, dict[str, str]] = {}
        entries = root.get("players")
        if isinstance(entries, list):
            for item in entries:
                if not isinstance(item, dict):
                    continue
                player_hash = item.get("playerHash")
                if not isinstance(player_hash, str) or not HASH_RE.fullmatch(player_hash):
                    continue
                known[player_hash] = {
                    "status": str(item.get("status") or "")[:32],
                    "sha256": str(item.get("sha256") or "")[:64],
                }
        current = root.get("current")
        current_hash = ""
        if isinstance(current, dict):
            candidate = current.get("playerHash")
            if isinstance(candidate, str) and HASH_RE.fullmatch(candidate):
                current_hash = candidate
        return {
            "registryUpdatedAt": registry_updated,
            "knownPlayers": known,
            "currentPlayerHash": current_hash,
        }


@dataclass(frozen=True)
class SelectionResult:
    players: Mapping[str, PlayerConfig]
    order: tuple[str, ...]
    verdicts: Mapping[str, str]
    counts: Mapping[str, int]
    selection: str
    conflicts: tuple[str, ...]
    notes: tuple[str, ...]


def _candidate_order(
    primary_players: Mapping[str, PlayerConfig],
    secondary_players: Mapping[str, PlayerConfig],
    last_known_good: Mapping[str, PlayerConfig],
) -> list[str]:
    order: list[str] = []
    seen: set[str] = set()

    def push(key: str) -> None:
        if key not in seen:
            seen.add(key)
            order.append(key)

    for key in primary_players:
        push(key)
    for key in last_known_good:
        push(key)
    for key in secondary_players:
        push(key)
    for key in sorted(set(primary_players) | set(secondary_players) | set(last_known_good)):
        push(key)
    return order


def _resolve_collisions(
    picks: Mapping[str, PlayerConfig],
    verdicts: Mapping[str, str],
    order: Sequence[str],
) -> tuple[dict[str, PlayerConfig], list[str], list[str]]:
    priority = {
        VERDICT_CONFIRMED: 0,
        VERDICT_SINGLE_PRIMARY: 1,
        VERDICT_SINGLE_SECONDARY: 2,
        VERDICT_LAST_KNOWN_GOOD: 3,
    }
    index = {key: position for position, key in enumerate(order)}
    ordered = sorted(
        picks.items(),
        key=lambda item: (priority.get(verdicts.get(item[0], ""), 9), index.get(item[0], 0)),
    )
    final: dict[str, PlayerConfig] = {}
    claimed: set[str] = set()
    omitted: list[str] = []
    warnings: list[str] = []
    for key, config in ordered:
        if key in claimed:
            omitted.append(key)
            warnings.append(f"{key}: dropped because another entry already owns that hash")
            continue
        safe_aliases: list[str] = []
        for alias in config.aliases:
            if alias in claimed:
                warnings.append(f"{key}: dropped colliding alias {alias}")
            else:
                safe_aliases.append(alias)
        if len(safe_aliases) != len(config.aliases):
            config = config.with_aliases(safe_aliases)
        final[key] = config
        claimed.add(key)
        claimed.update(safe_aliases)
    return final, omitted, warnings


def select_configurations(
    primary: SourceSnapshot,
    secondary: SourceSnapshot,
    last_known_good: Mapping[str, PlayerConfig],
    last_known_good_order: Sequence[str],
) -> SelectionResult:
    """Compare both sources and select a trusted configuration.

    Policy, highest preference first: a config confirmed by both sources, a valid
    primary config, a valid secondary config when the primary lacks it, the last
    known good entry, and finally omission. A conflicting pair never silently
    replaces the last known good entry.
    """
    primary_players = primary.players if primary.healthy else {}
    secondary_players = secondary.players if secondary.healthy else {}

    if not primary_players and not secondary_players:
        return SelectionResult(
            players=dict(last_known_good),
            order=tuple(last_known_good_order),
            verdicts={key: VERDICT_LAST_KNOWN_GOOD for key in last_known_good},
            counts={VERDICT_LAST_KNOWN_GOOD: len(last_known_good)},
            selection=SELECTION_LAST_KNOWN_GOOD,
            conflicts=(),
            notes=("No healthy upstream source is available; the last known good configuration is kept.",),
        )

    order = _candidate_order(primary_players, secondary_players, last_known_good)
    picks: dict[str, PlayerConfig] = {}
    verdicts: dict[str, str] = {}
    conflicts: list[str] = []
    notes: list[str] = []

    for key in order:
        primary_entry = primary_players.get(key)
        secondary_entry = secondary_players.get(key)
        existing = last_known_good.get(key)
        if primary_entry is not None and secondary_entry is not None:
            if primary_entry.capability() == secondary_entry.capability():
                verdicts[key] = VERDICT_CONFIRMED
                picks[key] = primary_entry
            else:
                verdicts[key] = VERDICT_CONFLICTING
                conflicts.append(key)
                notes.append(f"{key}: sources disagree; " + ("last known good entry kept" if existing else "entry omitted"))
                if existing is not None:
                    picks[key] = existing
        elif primary_entry is not None:
            verdicts[key] = VERDICT_SINGLE_PRIMARY
            picks[key] = primary_entry
        elif secondary_entry is not None:
            verdicts[key] = VERDICT_SINGLE_SECONDARY
            picks[key] = secondary_entry
        elif existing is not None:
            verdicts[key] = VERDICT_LAST_KNOWN_GOOD
            picks[key] = existing

    final, omitted, warnings = _resolve_collisions(picks, verdicts, order)
    for key in omitted:
        verdicts[key] = VERDICT_OMITTED
    notes.extend(warnings)

    final_order = tuple(key for key in order if key in final)
    counts: dict[str, int] = {}
    for verdict in verdicts.values():
        counts[verdict] = counts.get(verdict, 0) + 1

    if primary.healthy and secondary.healthy:
        selection = SELECTION_MIXED if conflicts else SELECTION_BOTH_CONFIRMED
    elif primary.healthy:
        selection = SELECTION_PRIMARY
    elif secondary.healthy:
        selection = SELECTION_SECONDARY
    else:
        selection = SELECTION_LAST_KNOWN_GOOD

    return SelectionResult(
        players=final,
        order=final_order,
        verdicts=verdicts,
        counts=counts,
        selection=selection,
        conflicts=tuple(conflicts),
        notes=tuple(notes),
    )


def load_registry_text(text: str) -> dict[str, PlayerConfig]:
    try:
        encoded = text.encode("utf-8")
    except UnicodeEncodeError as error:
        raise SourceRejected(f"registry is not encodable: {error}") from error
    return parse_players_registry(encoded)
