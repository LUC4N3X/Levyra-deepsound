from pathlib import Path

path = Path('.github/scripts/pr299_fix_app.py')
text = path.read_text(encoding='utf-8')
text = text.replace(
    """    '''import java.util.concurrent.TimeUnit;\n''',
    '''import java.util.concurrent.CancellationException;\nimport java.util.concurrent.TimeUnit;\nimport java.util.logging.Level;\nimport java.util.logging.Logger;\n''',
    'YoutubeParsingHelper logging imports',
""",
    """    '''import java.util.*;\n''',
    '''import java.util.*;\nimport java.util.concurrent.CancellationException;\nimport java.util.logging.Level;\nimport java.util.logging.Logger;\n''',
    'YoutubeParsingHelper logging imports',
""",
)
text = text.replace(
    """    '''public final class YoutubeParsingHelper {\n''',
    '''public final class YoutubeParsingHelper {\n    private static final Logger LOGGER = Logger.getLogger(YoutubeParsingHelper.class.getName());\n    private static final String PLAYER_ENDPOINT = \"player\";\n''',
""",
    """    '''public final class\nYoutubeParsingHelper {\n''',
    '''public final class\nYoutubeParsingHelper {\n    private static final Logger LOGGER = Logger.getLogger(YoutubeParsingHelper.class.getName());\n    private static final String PLAYER_ENDPOINT = \"player\";\n''',
""",
)
path.write_text(text, encoding='utf-8')
