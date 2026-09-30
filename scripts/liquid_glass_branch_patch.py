from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    target = Path(path)
    text = target.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{path}: expected exactly one match, got {count}: {old[:120]!r}")
    target.write_text(text.replace(old, new, 1), encoding="utf-8")


prefs = "app/src/main/java/com/luc4n3x/levyra/data/LevyraPreferences.kt"
replace_once(
    prefs,
    '        val KEY_UI_VISUAL_PERFORMANCE = stringPreferencesKey("ui_visual_performance")\n',
    '        val KEY_UI_VISUAL_PERFORMANCE = stringPreferencesKey("ui_visual_performance")\n'
    '        val KEY_UI_LIQUID_GLASS = booleanPreferencesKey("ui_liquid_glass")\n',
)
replace_once(
    prefs,
    '            mutable[KEY_UI_VISUAL_PERFORMANCE] = normalizedInterface.visualPerformance.name\n',
    '            mutable[KEY_UI_VISUAL_PERFORMANCE] = normalizedInterface.visualPerformance.name\n'
    '            mutable[KEY_UI_LIQUID_GLASS] = normalizedInterface.liquidGlassEnabled\n',
)
replace_once(
    prefs,
    '            it[KEY_UI_VISUAL_PERFORMANCE] = normalized.visualPerformance.name\n',
    '            it[KEY_UI_VISUAL_PERFORMANCE] = normalized.visualPerformance.name\n'
    '            it[KEY_UI_LIQUID_GLASS] = normalized.liquidGlassEnabled\n',
)
replace_once(
    prefs,
    '            visualPerformance = LevyraVisualPerformance.from(preferences[KEY_UI_VISUAL_PERFORMANCE].orEmpty()),\n',
    '            visualPerformance = LevyraVisualPerformance.from(preferences[KEY_UI_VISUAL_PERFORMANCE].orEmpty()),\n'
    '            liquidGlassEnabled = preferences[KEY_UI_LIQUID_GLASS] ?: true,\n',
)

backup = "app/src/main/java/com/luc4n3x/levyra/data/LevyraBackupManager.kt"
replace_once(
    backup,
    '    .put("visualPerformance", value.visualPerformance.name)\n',
    '    .put("visualPerformance", value.visualPerformance.name)\n'
    '    .put("liquidGlassEnabled", value.liquidGlassEnabled)\n',
)
replace_once(
    backup,
    '        visualPerformance = LevyraVisualPerformance.from(json.optString("visualPerformance")),\n',
    '        visualPerformance = LevyraVisualPerformance.from(json.optString("visualPerformance")),\n'
    '        liquidGlassEnabled = json.optBoolean("liquidGlassEnabled", true),\n',
)

activity = "app/src/main/java/com/luc4n3x/levyra/MainActivity.kt"
text = Path(activity).read_text(encoding="utf-8")
if 'import androidx.compose.runtime.CompositionLocalProvider\n' not in text:
    replace_once(
        activity,
        'import androidx.compose.runtime.LaunchedEffect\n',
        'import androidx.compose.runtime.CompositionLocalProvider\nimport androidx.compose.runtime.LaunchedEffect\n',
    )
replace_once(
    activity,
    'import com.luc4n3x.levyra.ui.LevyraApp\n',
    'import com.luc4n3x.levyra.ui.LevyraApp\n'
    'import com.luc4n3x.levyra.ui.components.LocalLevyraLiquidGlassEnabled\n',
)
replace_once(
    activity,
    '    val fontPreset: LevyraFontPreset,\n',
    '    val fontPreset: LevyraFontPreset,\n'
    '    val liquidGlassEnabled: Boolean,\n',
)
replace_once(
    activity,
    '    fontPreset = interfaceSettings.fontPreset,\n',
    '    fontPreset = interfaceSettings.fontPreset,\n'
    '    liquidGlassEnabled = interfaceSettings.liquidGlassEnabled,\n',
)
replace_once(
    activity,
    '''                LevyraApp(
                    viewModel = viewModel,
                    isInPictureInPicture = pipMode.value,
                    onRetryPreUpdateBackup = ::retryPreUpdateBackup,
                    onContinueUpdateWithoutBackup = ::continueUpdateWithoutBackup
                )
''',
    '''                CompositionLocalProvider(
                    LocalLevyraLiquidGlassEnabled provides activityUiState.liquidGlassEnabled
                ) {
                    LevyraApp(
                        viewModel = viewModel,
                        isInPictureInPicture = pipMode.value,
                        onRetryPreUpdateBackup = ::retryPreUpdateBackup,
                        onContinueUpdateWithoutBackup = ::continueUpdateWithoutBackup
                    )
                }
''',
)

app = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt").read_text(encoding="utf-8")
needle = "visualPerformance"
contexts = []
start = 0
while True:
    index = app.find(needle, start)
    if index < 0:
        break
    left = max(0, index - 1200)
    right = min(len(app), index + 1800)
    contexts.append(f"--- occurrence {len(contexts)} at char {index} ---\n{app[left:right]}\n")
    start = index + len(needle)
Path("liquid_glass_ui_context.txt").write_text("\n".join(contexts), encoding="utf-8")
Path(".github/workflows/liquid-glass-branch-patch.yml").unlink(missing_ok=True)
