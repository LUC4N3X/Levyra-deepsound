from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    target = Path(path)
    text = target.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{path}: expected exactly one match, got {count}: {old[:120]!r}")
    target.write_text(text.replace(old, new, 1), encoding="utf-8")


def insert_after_all(path: str, old: str, addition: str, expected: int) -> None:
    target = Path(path)
    text = target.read_text(encoding="utf-8")
    count = text.count(old)
    if count != expected:
        raise RuntimeError(f"{path}: expected {expected} matches, got {count}: {old[:120]!r}")
    target.write_text(text.replace(old, old + addition), encoding="utf-8")


prefs = "app/src/main/java/com/luc4n3x/levyra/data/LevyraPreferences.kt"
replace_once(
    prefs,
    'private val KEY_UI_VISUAL_PERFORMANCE = stringPreferencesKey("ui_visual_performance")\n',
    'private val KEY_UI_VISUAL_PERFORMANCE = stringPreferencesKey("ui_visual_performance")\n'
    'private val KEY_UI_LIQUID_GLASS = booleanPreferencesKey("ui_liquid_glass")\n',
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
replace_once(
    activity,
    'import androidx.compose.runtime.Composable\n' if 'import androidx.compose.runtime.Composable\n' in Path(activity).read_text(encoding='utf-8') else 'import androidx.compose.runtime.LaunchedEffect\n',
    ('import androidx.compose.runtime.Composable\nimport androidx.compose.runtime.CompositionLocalProvider\n'
     if 'import androidx.compose.runtime.Composable\n' in Path(activity).read_text(encoding='utf-8')
     else 'import androidx.compose.runtime.CompositionLocalProvider\nimport androidx.compose.runtime.LaunchedEffect\n'),
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
old_app = '''                LevyraApp(
                    viewModel = viewModel,
                    isInPictureInPicture = pipMode.value,
                    onRetryPreUpdateBackup = ::retryPreUpdateBackup,
                    onContinueUpdateWithoutBackup = ::continueUpdateWithoutBackup
                )
'''
new_app = '''                CompositionLocalProvider(
                    LocalLevyraLiquidGlassEnabled provides activityUiState.liquidGlassEnabled
                ) {
                    LevyraApp(
                        viewModel = viewModel,
                        isInPictureInPicture = pipMode.value,
                        onRetryPreUpdateBackup = ::retryPreUpdateBackup,
                        onContinueUpdateWithoutBackup = ::continueUpdateWithoutBackup
                    )
                }
'''
replace_once(activity, old_app, new_app)

# Emit bounded diagnostic context for the giant UI file so the next patch can place the toggle
# next to Levyra's existing visual-performance controls without guessing.
app = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt").read_text(encoding="utf-8")
needle = "visualPerformance"
positions = []
start = 0
while True:
    index = app.find(needle, start)
    if index < 0:
        break
    positions.append(index)
    start = index + len(needle)
print(f"LEVYRA_APP_VISUAL_PERFORMANCE_OCCURRENCES={len(positions)}")
for i, index in enumerate(positions[:12]):
    left = max(0, index - 900)
    right = min(len(app), index + 1400)
    print(f"--- VISUAL_CONTEXT_{i} ---")
    print(app[left:right])

# Remove temporary branch-only patcher files from the resulting source commit.
Path("scripts/liquid_glass_branch_patch.py").unlink(missing_ok=True)
Path(".github/workflows/liquid-glass-branch-patch.yml").unlink(missing_ok=True)
