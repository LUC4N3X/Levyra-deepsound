from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    file = Path(path)
    text = file.read_text()
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected one match, found {count}\n--- OLD ---\n{old}")
    file.write_text(text.replace(old, new))


app = "app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt"
controls = "app/src/main/java/com/luc4n3x/levyra/ui/components/PlayerControls.kt"

replace_once(
    app,
    '''    val primarySurface = remember(primary) {
        primary.playerAdjustBackgroundFor(Color.White, PlayerStrongContrast).color
    }
    val secondarySurface = remember(secondary) {
        secondary.playerAdjustBackgroundFor(Color.White, PlayerStrongContrast).color
    }
    val mixedSurface = remember(primary, secondary) {
        primary.playerMix(secondary, 0.5f).playerAdjustBackgroundFor(Color.White, PlayerStrongContrast).color
    }
''',
    '''    val backgroundAccent = remember(primary) {
        primary
            .playerAdjustBackgroundFor(Color.White, PlayerStrongContrast)
            .color
            .playerMix(PlayerDarkSurface, 0.28f)
    }
'''
)

replace_once(
    app,
    '''                Brush.verticalGradient(
                    listOf(
                        primarySurface.playerMix(Color.Black, 0.18f),
                        mixedSurface.playerMix(Color.Black, 0.32f),
                        secondarySurface.playerMix(Color.Black, 0.46f),
                        Color(0xFF050508),
                        Color.Black
                    )
                )''',
    '''                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to backgroundAccent,
                        0.34f to backgroundAccent.playerMix(Color.Black, 0.46f),
                        0.66f to Color(0xFF0A090D),
                        1.00f to Color.Black
                    )
                )'''
)

replace_once(
    app,
    'targetValue = if (selected) activeColor.copy(alpha = 0.42f) else Color.Transparent,',
    'targetValue = if (selected) Color.White.copy(alpha = 0.14f) else Color.Transparent,'
)

replace_once(
    app,
    '''                            primary.playerMix(Color.White, 0.10f).copy(alpha = 0.34f),
                            secondary.copy(alpha = 0.18f),
                            primary.playerMix(Color.Black, 0.68f).copy(alpha = 0.08f),''',
    '''                            primary.playerMix(Color.Black, 0.60f).copy(alpha = 0.13f),
                            secondary.playerMix(Color.Black, 0.68f).copy(alpha = 0.07f),
                            primary.playerMix(Color.Black, 0.78f).copy(alpha = 0.04f),'''
)

replace_once(
    controls,
    '''import com.luc4n3x.levyra.ui.PlayerDarkSurface
import com.luc4n3x.levyra.ui.PlayerMinimumContrast
import com.luc4n3x.levyra.ui.playerCompositeOver
import com.luc4n3x.levyra.ui.playerContentColor
import com.luc4n3x.levyra.ui.playerContrastGradient
''',
    '''import com.luc4n3x.levyra.ui.PlayerMinimumContrast
import com.luc4n3x.levyra.ui.playerContrastGradient
'''
)

replace_once(
    controls,
    '''    val fill = if (active) accent.copy(alpha = 0.16f) else Color.Transparent
    val activeTint = remember(accentTarget) {
        accentTarget.playerContentColor(
            listOf(accentTarget.copy(alpha = 0.16f).playerCompositeOver(PlayerDarkSurface))
        )
    }
    val tint = if (active) activeTint else LevyraPlayerDesign.IconIdle
    val borderAlpha by animateFloatAsState(
        targetValue = if (active) 0.24f else 0f,''',
    '''    val fill = if (active) {
        Color.White.playerMix(accent, 0.06f).copy(alpha = 0.12f)
    } else {
        Color.Transparent
    }
    val activeTint = remember(accentTarget) {
        Color.White.playerMix(accentTarget, 0.04f)
    }
    val tint = if (active) activeTint else LevyraPlayerDesign.IconIdle
    val borderAlpha by animateFloatAsState(
        targetValue = if (active) 0.18f else 0f,'''
)

replace_once(
    controls,
    'BorderStroke(LevyraPlayerDesign.Hairline, accent.copy(alpha = borderAlpha)),',
    'BorderStroke(LevyraPlayerDesign.Hairline, Color.White.playerMix(accent, 0.08f).copy(alpha = borderAlpha)),'
)

replace_once(
    controls,
    '''        playerContrastGradient(
            start = accentTarget.playerMix(PlayerDarkSurface, 0.52f),
            end = accentSecondaryTarget.playerMix(PlayerDarkSurface, 0.58f),
            minimumContrast = PlayerMinimumContrast
        )''',
    '''        playerContrastGradient(
            start = Color(0xFFF2F2F4).playerMix(accentTarget, 0.04f),
            end = Color(0xFFDDDEE2).playerMix(accentSecondaryTarget, 0.03f),
            minimumContrast = PlayerMinimumContrast
        )'''
)

replace_once(controls, 'elevation = 5.dp,', 'elevation = 3.dp,')
