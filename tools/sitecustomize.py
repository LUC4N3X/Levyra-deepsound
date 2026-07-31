from __future__ import annotations

from pathlib import Path

_original_write_text = Path.write_text


def _write_text_with_valid_kotlin_json(self: Path, data: str, *args, **kwargs):
    if self.as_posix().endswith("EditorialCatalogParserTest.kt"):
        data = data.replace(
            '        val youtube = youtubeMusic?.let { ","youtubeMusic":$it" }.orEmpty()\n',
            '        val quote = 34.toChar()\n'
            '        val youtube = youtubeMusic?.let { ",${quote}youtubeMusic${quote}:$it" }.orEmpty()\n',
        )
        data = data.replace(
            '            ?.let { ","artworkUrl":"$it"" }\n',
            '            ?.let { ",${quote}artworkUrl${quote}:${quote}$it${quote}" }\n',
        )
    return _original_write_text(self, data, *args, **kwargs)


Path.write_text = _write_text_with_valid_kotlin_json
