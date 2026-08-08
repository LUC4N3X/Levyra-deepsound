from pathlib import Path


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one match, found {count}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


sponsor = Path("app/src/main/java/com/luc4n3x/levyra/data/SponsorBlockRepository.kt")
replace_once(
    sponsor,
    "import java.io.Closeable\nimport java.io.InputStream\n",
    "import java.io.Closeable\nimport java.io.IOException\nimport java.io.InputStream\n",
    "SponsorBlock IOException import",
)
replace_once(
    sponsor,
    "import kotlinx.coroutines.Dispatchers\n",
    "import kotlinx.coroutines.CancellationException\nimport kotlinx.coroutines.Dispatchers\n",
    "SponsorBlock cancellation import",
)
replace_once(
    sponsor,
    "        val response = runCatching { fetcher.fetch(url) }.getOrNull() ?: return@withContext emptyList()\n",
    """        val response = try {
            fetcher.fetch(url)
        } catch (error: CancellationException) {
            throw error
        } catch (_: IOException) {
            return@withContext emptyList()
        }
""",
    "SponsorBlock fetch cancellation",
)
replace_once(
    sponsor,
    "            readTimeout = 11000\n            setRequestProperty(\"Accept\", \"application/json\")\n",
    "            readTimeout = 11000\n            setInstanceFollowRedirects(false)\n            setRequestProperty(\"Accept\", \"application/json\")\n",
    "SponsorBlock redirect policy",
)

importer = Path("app/src/main/java/com/luc4n3x/levyra/data/UniversalPlaylistImporter.kt")
replace_once(
    importer,
    "                continuation.resume(response)\n",
    "                continuation.resume(response) { _, value, _ -> value.close() }\n",
    "Spotify cancellation-aware resume",
)

test = Path("app/src/test/java/com/luc4n3x/levyra/data/SponsorBlockRepositoryTest.kt")
replace_once(
    test,
    "import java.io.ByteArrayInputStream\n",
    "import java.io.ByteArrayInputStream\nimport kotlinx.coroutines.CancellationException\n",
    "SponsorBlock test cancellation import",
)
marker = """    @Test
    fun negativeCacheExpiresAndThenAcceptsPositiveSegments() {
"""
new_test = """    @Test
    fun cancellationFromFetcherPropagates() {
        val repository = SponsorBlockRepository(
            SponsorBlockHttpFetcher { throw CancellationException("cancelled") }
        ) { 1_000L }

        try {
            repositorySegments(repository, "video")
            throw AssertionError("Expected CancellationException")
        } catch (error: CancellationException) {
            assertEquals("cancelled", error.message)
        }
    }

"""
replace_once(test, marker, new_test + marker, "SponsorBlock cancellation regression test")

print("CodeRabbit fixes applied")
