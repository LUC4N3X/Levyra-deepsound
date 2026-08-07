from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly 1 match, found {count}")
    return text.replace(old, new, 1)


strings_path = Path("app/src/main/java/com/luc4n3x/levyra/ui/i18n/LevyraStrings.kt")
strings = strings_path.read_text()
strings = replace_once(
    strings,
    '"exploreMoods", "exploreSamples", "exploreSamplesSubtitle", "exploreFresh"',
    '"exploreMoods", "exploreSamples", "exploreSamplesSubtitle", "exploreSamplesError", "exploreSamplesRetry", "exploreFresh"',
    "required Explore localization keys",
)
strings_path.write_text(strings)


test_path = Path("app/src/test/java/com/luc4n3x/levyra/ui/i18n/LevyraStringsTest.kt")
test = test_path.read_text()
test = replace_once(
    test,
    "            assertTrue(strings.exploreSamplesSubtitle.isNotBlank())\n",
    "            assertTrue(strings.exploreSamplesSubtitle.isNotBlank())\n"
    "            assertTrue(strings.exploreSamplesError.isNotBlank())\n"
    "            assertTrue(strings.exploreSamplesRetry.isNotBlank())\n",
    "Explore Samples localization regression coverage",
)
test_path.write_text(test)

print("PR313_REVIEW_LOCALIZATION_FOLLOWUP_OK")
