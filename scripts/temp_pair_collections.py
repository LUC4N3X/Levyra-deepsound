from pathlib import Path

engine_path = Path("app/src/main/java/com/luc4n3x/levyra/data/HomeEditorialEngine.kt")
engine_text = engine_path.read_text(encoding="utf-8")

replacements = [
    (
        "    private const val maximumCollectionCount = 7",
        "    private const val maximumCollectionCount = 8",
    ),
    (
        "        if (collections.size <= maximumCollectionCount) return collections",
        """        if (collections.size <= maximumCollectionCount) {
            val pairedSize = collections.size - collections.size % 2
            return collections.take(pairedSize)
        }""",
    ),
    (
        "        return selected\n    }\n\n    private fun collectionQuality",
        """        val pairedSize = selected.size - selected.size % 2
        return selected.take(pairedSize)
    }

    private fun collectionQuality""",
    ),
]

for old, new in replacements:
    if engine_text.count(old) != 1:
        raise RuntimeError(f"expected exactly one match for {old!r}")
    engine_text = engine_text.replace(old, new, 1)

engine_path.write_text(engine_text, encoding="utf-8")

test_path = Path("app/src/test/java/com/luc4n3x/levyra/data/HomeEditorialEngineTest.kt")
test_text = test_path.read_text(encoding="utf-8")
old_test = "            assertTrue(collections.size <= 7)"
new_test = """            assertTrue(collections.size <= 8)
            assertEquals(0, collections.size % 2)"""
if test_text.count(old_test) != 1:
    raise RuntimeError("expected exactly one collection-count assertion")
test_path.write_text(test_text.replace(old_test, new_test, 1), encoding="utf-8")
