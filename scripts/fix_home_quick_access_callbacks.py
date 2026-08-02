from pathlib import Path

path = Path("app/src/main/java/com/luc4n3x/levyra/ui/HomeExperience.kt")
text = path.read_text(encoding="utf-8")

old_import = "import androidx.compose.runtime.remember\n"
if text.count(old_import) != 1:
    raise RuntimeError("Expected one remember import")
text = text.replace(old_import, "", 1)

start = text.index("    val items = remember(\n")
end_marker = "\n\n    Column(verticalArrangement = Arrangement.spacedBy(LevyraHomeDesign.TileGap))"
end = text.index(end_marker, start)
old_block = text[start:end]

list_start = old_block.index("        listOf(\n")
list_end = old_block.rfind("\n        )\n    }")
if list_end == -1:
    raise RuntimeError("Could not locate remembered list boundary")
list_body = old_block[list_start + 8:list_end + len("\n        )")]
new_block = "    val items = " + list_body.lstrip()

text = text[:start] + new_block + text[end:]
path.write_text(text, encoding="utf-8")
print("Removed remembered callbacks from Home quick-access items")
