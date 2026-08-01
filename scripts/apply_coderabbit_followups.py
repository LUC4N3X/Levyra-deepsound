from pathlib import Path


def replace_once(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"expected exactly one match in {path}, found {count}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


playback = Path("app/src/main/java/com/luc4n3x/levyra/data/PlaybackResolver.kt")
replace_once(
    playback,
    '''    val artworkLocked = presented.source.equals("Levyra Editorial", ignoreCase = true) ||
        EDITORIAL_ARTWORK_LOCK_TAG in presented.moodTags ||
        presented.moodTags.any { it.equals("chart", ignoreCase = true) } ||
        presented.source.contains("Charts", ignoreCase = true)
''',
    '''    val artworkLocked = presented.source.equals("Levyra Editorial", ignoreCase = true) ||
        EDITORIAL_ARTWORK_LOCK_TAG in presented.moodTags
''',
)

ui = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
replace_once(
    ui,
    '''                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = artScale
                                        scaleY = artScale
                                        translationY = artOffset.toPx()
                                        shadowElevation = artShadow
                                        shape = RoundedCornerShape(artCorner)
                                    }
''',
    '''                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = artScale
                                        scaleY = artScale
                                        translationY = artOffset.toPx()
                                        shape = RoundedCornerShape(artCorner)
                                    }
''',
)
replace_once(
    ui,
    '''                modifier = Modifier.pressable(enabled = canOpenComments, onClick = onComments)
''',
    '''                modifier = Modifier
                    .sizeIn(minHeight = 48.dp)
                    .pressable(enabled = canOpenComments, onClick = onComments)
''',
)
replace_once(
    ui,
    '''                .height(if (compact) 31.dp else 33.dp)
''',
    '''                .sizeIn(minHeight = 48.dp)
''',
)

builder = Path("scripts/build_community_canvas_index.py")
write_shards = '''    if output_dir.exists():
        shutil.rmtree(output_dir)
    shards_dir = output_dir / "v2" / shard_directory / "shards"
    shards_dir.mkdir(parents=True, exist_ok=True)
    for prefix, shard_rows in sorted(shards.items()):
        (shards_dir / f"{prefix}.json").write_bytes(serialized_shard(shard_rows))

'''
replace_once(builder, write_shards, "")
replace_once(
    builder,
    '''    manifest_path = output_dir / "v2" / "manifest.json"
    manifest_path.write_bytes(manifest_payload)
''',
    write_shards + '''    manifest_path = output_dir / "v2" / "manifest.json"
    manifest_path.write_bytes(manifest_payload)
''',
)
