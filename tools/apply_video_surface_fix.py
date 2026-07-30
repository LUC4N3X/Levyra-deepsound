from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt"
LAYOUT = ROOT / "app/src/main/res/layout/levyra_video_player_view.xml"


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one anchor, found {count}")
    return text.replace(old, new, 1)


text = APP.read_text(encoding="utf-8")

old_video_block = '''                            LevyraVideoSurface(
                                state = state,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                                    .graphicsLayer {
                                        scaleX = artScale
                                        scaleY = artScale
                                        shadowElevation = artShadow
                                        shape = RoundedCornerShape(artCorner)
                                        clip = true
                                    }
                                    .border(
                                        width = 1.dp,
                                        color = Color.White.copy(alpha = 0.18f),
                                        shape = RoundedCornerShape(artCorner)
                                    )
                            )
'''
new_video_block = '''                            LevyraVideoSurface(
                                state = state,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                                    .graphicsLayer {
                                        scaleX = artScale
                                        scaleY = artScale
                                        shadowElevation = artShadow
                                        shape = RoundedCornerShape(artCorner)
                                        clip = true
                                    }
                                    .border(
                                        width = 1.dp,
                                        color = Color.White.copy(alpha = 0.18f),
                                        shape = RoundedCornerShape(artCorner)
                                    )
                            )
                            Surface(
                                color = Color.Black.copy(alpha = 0.72f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                                shape = CircleShape,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(18.dp)
                                    .zIndex(40f)
                                    .pressable(onClick = viewModel::toggleVideoMode)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MusicNote,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Text(
                                        text = strings.song,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
'''
text = replace_once(text, old_video_block, new_video_block, "video exit control")

old_surface_modifier = '''    val surfaceModifier = if (pictureInPicture) {
        modifier
    } else {
        modifier.aspectRatio(aspectRatio.coerceIn(0.56f, 2.1f))
    }
    Box(
        modifier = surfaceModifier.background(Color.Black),
'''
new_surface_modifier = '''    Box(
        modifier = modifier.background(Color.Black),
'''
text = replace_once(text, old_surface_modifier, new_surface_modifier, "bounded video surface")

old_factory = '''                factory = { context ->
                    androidx.media3.ui.PlayerView(context).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        setShowBuffering(androidx.media3.ui.PlayerView.SHOW_BUFFERING_ALWAYS)
                        setBackgroundColor(android.graphics.Color.BLACK)
                        keepScreenOn = true
                        this.player = player
                    }
                },
'''
new_factory = '''                factory = { context ->
                    // TextureView stays inside Compose bounds; SurfaceView can cover sibling controls.
                    (android.view.LayoutInflater.from(context).inflate(
                        R.layout.levyra_video_player_view,
                        null,
                        false
                    ) as androidx.media3.ui.PlayerView).apply {
                        keepScreenOn = true
                        this.player = player
                    }
                },
'''
text = replace_once(text, old_factory, new_factory, "TextureView player factory")

APP.write_text(text, encoding="utf-8")

LAYOUT.parent.mkdir(parents=True, exist_ok=True)
LAYOUT.write_text(
    '''<?xml version="1.0" encoding="utf-8"?>
<androidx.media3.ui.PlayerView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/levyra_video_player_view"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@android:color/black"
    app:keep_content_on_player_reset="true"
    app:resize_mode="fit"
    app:show_buffering="always"
    app:shutter_background_color="@android:color/black"
    app:surface_type="texture_view"
    app:use_controller="false" />
''',
    encoding="utf-8",
)
