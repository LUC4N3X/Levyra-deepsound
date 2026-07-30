from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def replace_once(path: str, old: str, new: str) -> None:
    target = ROOT / path
    text = target.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{path}: expected one anchor, found {count}: {old!r}")
    target.write_text(text.replace(old, new, 1), encoding="utf-8")


def replace_all_exact(path: str, old: str, new: str, expected: int) -> None:
    target = ROOT / path
    text = target.read_text(encoding="utf-8")
    count = text.count(old)
    if count != expected:
        raise RuntimeError(f"{path}: expected {expected} anchors, found {count}: {old!r}")
    target.write_text(text.replace(old, new), encoding="utf-8")


replace_once(
    "third_party/LevyraNexus/src/main/kotlin/com/luc4n3x/levyra/nexus/network/LevyraRouteEngine.kt",
    "                LevyraRouteFailure.UNKNOWN -> if (consecutive >= 3) SHORT_BLOCK_MS else 0L\n                else -> 0L\n",
    "                LevyraRouteFailure.UNKNOWN -> if (consecutive >= 3) SHORT_BLOCK_MS else 0L\n",
)

replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/AlbumDescriptionRepository.kt",
    "                response.body?.string()?.take(MAX_RESPONSE_CHARS)\n",
    "                response.body.string().take(MAX_RESPONSE_CHARS)\n",
)

replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/ChartsRepository.kt",
    "            override fun onFailure(call: Call, error: IOException) {\n",
    "            override fun onFailure(call: Call, e: IOException) {\n",
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/ChartsRepository.kt",
    "                        if (!current.isSuccessful) null else current.body?.string()?.takeIf { it.isNotBlank() }\n",
    "                        if (!current.isSuccessful) null else current.body.string().takeIf { it.isNotBlank() }\n",
)

replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/EditorialChartsRepository.kt",
    "                override fun onFailure(call: Call, error: IOException) {\n",
    "                override fun onFailure(call: Call, e: IOException) {\n",
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/EditorialChartsRepository.kt",
    "                            val body = current.body ?: return@use null\n",
    "                            val body = current.body\n",
)

replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/YoutubeMusicChartsRepository.kt",
    "                override fun onFailure(call: Call, error: IOException) {\n",
    "                override fun onFailure(call: Call, e: IOException) {\n",
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/YoutubeMusicChartsRepository.kt",
    "                            if (!current.isSuccessful) null else current.body?.string()?.takeIf(String::isNotBlank)\n",
    "                            if (!current.isSuccessful) null else current.body.string().takeIf(String::isNotBlank)\n",
)

replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/lore/ArtistLoreRepository.kt",
    "    override fun onFailure(call: Call, error: IOException) {\n",
    "    override fun onFailure(call: Call, e: IOException) {\n",
)

replace_once(
    "app/src/main/java/com/luc4n3x/levyra/player/offline/OfflineAudioExporter.kt",
    "@file:OptIn(androidx.media3.common.util.UnstableApi::class)\n",
    "@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)\n",
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/player/offline/OfflineAudioExporter.kt",
    "                override fun onFailure(call: Call, error: IOException) {\n                    deferred.completeExceptionally(error)\n",
    "                override fun onFailure(call: Call, e: IOException) {\n                    deferred.completeExceptionally(e)\n",
)

replace_once(
    "app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt",
    "import androidx.compose.material.icons.rounded.OpenInNew\n",
    "import androidx.compose.material.icons.automirrored.rounded.OpenInNew\n",
)
replace_all_exact(
    "app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt",
    "Icons.Rounded.OpenInNew",
    "Icons.AutoMirrored.Rounded.OpenInNew",
    2,
)

replace_once(
    "app/src/main/java/com/luc4n3x/levyra/ui/support/OpenSourceSupportPrompt.kt",
    "import androidx.compose.material.icons.rounded.OpenInNew\n",
    "import androidx.compose.material.icons.automirrored.rounded.OpenInNew\n",
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/ui/support/OpenSourceSupportPrompt.kt",
    "Icons.Rounded.OpenInNew",
    "Icons.AutoMirrored.Rounded.OpenInNew",
)
