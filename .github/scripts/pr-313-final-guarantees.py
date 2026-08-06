from __future__ import annotations

import json
from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match, found {count}")
    return text.replace(old, new, 1)


# When the user has no history, keep all four localized fallback queries.
# With a warm profile, reserve two slots for preferences and two for locale.
path = Path("app/src/main/java/com/luc4n3x/levyra/data/YoutubeShortsRepository.kt")
text = path.read_text(encoding="utf-8")
text = replace_once(
    text,
    '''    val localized = localizedShortQueries(languageCode)
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinctBy { query -> query.lowercase(Locale.ROOT) }
        .take(2)
''',
    '''    val localizedLimit = if (personalized.isEmpty()) MAX_SHORT_QUERIES else 2
    val localized = localizedShortQueries(languageCode)
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinctBy { query -> query.lowercase(Locale.ROOT) }
        .take(localizedLimit)
''',
    "adaptive localized NewPipe query limit",
)
path.write_text(text, encoding="utf-8")


# Every configured language/market must publish a release collection. Localized
# discovery already has a global fallback, so these collections should not be
# silently dropped from the public catalog.
config_path = Path("tools/levyra-editorial/config.json")
config = json.loads(config_path.read_text(encoding="utf-8"))
release_count = 0
for collection in config.get("collections", []):
    if str(collection.get("id", "")).startswith("new-releases-"):
        collection["optional"] = False
        release_count += 1
if release_count != 26:
    raise RuntimeError(f"Expected 26 localized release collections, found {release_count}")
config_path.write_text(json.dumps(config, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

print("Guaranteed localized release collections and balanced Samples fallbacks")
