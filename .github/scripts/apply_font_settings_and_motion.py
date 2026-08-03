from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    file = Path(path)
    text = file.read_text()
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected one match, found {count}\n--- OLD ---\n{old}")
    file.write_text(text.replace(old, new))


experience = "app/src/main/java/com/luc4n3x/levyra/domain/LevyraExperienceSettings.kt"
preferences = "app/src/main/java/com/luc4n3x/levyra/data/LevyraPreferences.kt"
backup = "app/src/main/java/com/luc4n3x/levyra/data/LevyraBackupManager.kt"
theme = "app/src/main/java/com/luc4n3x/levyra/ui/theme/LevyraTheme.kt"
activity = "app/src/main/java/com/luc4n3x/levyra/MainActivity.kt"
viewmodel = "app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt"
strings = "app/src/main/java/com/luc4n3x/levyra/ui/i18n/LevyraStrings.kt"
app = "app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt"

# Interface model.
replace_once(
    experience,
    '''    val showCharts: Boolean = true,
    val playerGesturesEnabled: Boolean = true,
''',
    '''    val showCharts: Boolean = true,
    val fontPreset: LevyraFontPreset = LevyraFontPreset.Outfit,
    val playerGesturesEnabled: Boolean = true,
'''
)

# DataStore persistence.
replace_once(
    preferences,
    'import com.luc4n3x.levyra.domain.LevyraInterfaceSettings\n',
    'import com.luc4n3x.levyra.domain.LevyraInterfaceSettings\nimport com.luc4n3x.levyra.domain.LevyraFontPreset\n'
)
replace_once(
    preferences,
    '''            mutable[KEY_UI_CHARTS] = normalizedInterface.showCharts
            mutable[KEY_UI_PLAYER_GESTURES] = normalizedInterface.playerGesturesEnabled
''',
    '''            mutable[KEY_UI_CHARTS] = normalizedInterface.showCharts
            mutable[KEY_UI_FONT_PRESET] = normalizedInterface.fontPreset.name
            mutable[KEY_UI_PLAYER_GESTURES] = normalizedInterface.playerGesturesEnabled
'''
)
replace_once(
    preferences,
    '''            it[KEY_UI_CHARTS] = normalized.showCharts
            it[KEY_UI_PLAYER_GESTURES] = normalized.playerGesturesEnabled
''',
    '''            it[KEY_UI_CHARTS] = normalized.showCharts
            it[KEY_UI_FONT_PRESET] = normalized.fontPreset.name
            it[KEY_UI_PLAYER_GESTURES] = normalized.playerGesturesEnabled
'''
)
replace_once(
    preferences,
    '''        showCharts = preferences[KEY_UI_CHARTS] ?: true,
        playerGesturesEnabled = preferences[KEY_UI_PLAYER_GESTURES] ?: true,
''',
    '''        showCharts = preferences[KEY_UI_CHARTS] ?: true,
        fontPreset = LevyraFontPreset.from(preferences[KEY_UI_FONT_PRESET].orEmpty()),
        playerGesturesEnabled = preferences[KEY_UI_PLAYER_GESTURES] ?: true,
'''
)
replace_once(
    preferences,
    '''        val KEY_UI_CHARTS = booleanPreferencesKey("ui_show_charts")
        val KEY_UI_PLAYER_GESTURES = booleanPreferencesKey("ui_player_gestures")
''',
    '''        val KEY_UI_CHARTS = booleanPreferencesKey("ui_show_charts")
        val KEY_UI_FONT_PRESET = stringPreferencesKey("ui_font_preset")
        val KEY_UI_PLAYER_GESTURES = booleanPreferencesKey("ui_player_gestures")
'''
)

# Backup round-trip.
replace_once(
    backup,
    'import com.luc4n3x.levyra.domain.LevyraInterfaceSettings\n',
    'import com.luc4n3x.levyra.domain.LevyraInterfaceSettings\nimport com.luc4n3x.levyra.domain.LevyraFontPreset\n'
)
replace_once(
    backup,
    '''        .put("showCharts", value.showCharts)
        .put("playerGesturesEnabled", value.playerGesturesEnabled)
''',
    '''        .put("showCharts", value.showCharts)
        .put("fontPreset", value.fontPreset.name)
        .put("playerGesturesEnabled", value.playerGesturesEnabled)
'''
)
replace_once(
    backup,
    '''            showCharts = json.optBoolean("showCharts", true),
            playerGesturesEnabled = json.optBoolean("playerGesturesEnabled", true),
''',
    '''            showCharts = json.optBoolean("showCharts", true),
            fontPreset = LevyraFontPreset.from(json.optString("fontPreset")),
            playerGesturesEnabled = json.optBoolean("playerGesturesEnabled", true),
'''
)

# Reactive typography controller.
replace_once(
    theme,
    'import androidx.compose.ui.graphics.Color\n',
    'import androidx.compose.ui.graphics.Color\nimport com.luc4n3x.levyra.domain.LevyraFontPreset\n'
)
replace_once(
    theme,
    'private val activePaletteState = mutableStateOf(LevyraThemes.cosmic)\n',
    '''private val activePaletteState = mutableStateOf(LevyraThemes.cosmic)
private val activeFontPresetState = mutableStateOf(LevyraFontPreset.Outfit)

object LevyraTypographyController {
    fun apply(preset: LevyraFontPreset) {
        if (activeFontPresetState.value != preset) {
            activeFontPresetState.value = preset
        }
    }
}
'''
)
replace_once(
    theme,
    '        typography = LevyraTypography,\n',
    '        typography = levyraTypographyFor(activeFontPresetState.value),\n'
)

# Apply saved font before the first Compose frame.
replace_once(
    activity,
    'import com.luc4n3x.levyra.data.LevyraArtworkCache\n',
    'import com.luc4n3x.levyra.data.LevyraArtworkCache\nimport com.luc4n3x.levyra.data.LevyraPreferences\n'
)
replace_once(
    activity,
    'import com.luc4n3x.levyra.ui.theme.LevyraThemeController\n',
    'import com.luc4n3x.levyra.ui.theme.LevyraThemeController\nimport com.luc4n3x.levyra.ui.theme.LevyraTypographyController\n'
)
replace_once(
    activity,
    '''        val startPalette = LevyraThemes.byId(LevyraThemes.APPLE_MUSIC)
        LevyraThemeController.apply(startPalette.id)
''',
    '''        val startPalette = LevyraThemes.byId(LevyraThemes.APPLE_MUSIC)
        LevyraThemeController.apply(startPalette.id)
        LevyraTypographyController.apply(LevyraPreferences(this).interfaceSettings().fontPreset)
'''
)

# Apply typography immediately on change and after restore.
replace_once(
    viewmodel,
    'import com.luc4n3x.levyra.ui.theme.LevyraThemes\n',
    'import com.luc4n3x.levyra.ui.theme.LevyraThemes\nimport com.luc4n3x.levyra.ui.theme.LevyraTypographyController\n'
)
replace_once(
    viewmodel,
    '''    fun setInterfaceSettings(value: LevyraInterfaceSettings) {
        val normalized = value.normalized()
        preferences.setInterfaceSettings(normalized)
        _state.update { it.copy(interfaceSettings = normalized) }
    }
''',
    '''    fun setInterfaceSettings(value: LevyraInterfaceSettings) {
        val normalized = value.normalized()
        preferences.setInterfaceSettings(normalized)
        LevyraTypographyController.apply(normalized.fontPreset)
        _state.update { it.copy(interfaceSettings = normalized) }
    }
'''
)
replace_once(
    viewmodel,
    '''        applyLanguageContent(snapshot.languageCode, refreshRemote = true)
        player.setSkipSilence(snapshot.skipSilence)
''',
    '''        LevyraTypographyController.apply(snapshot.interfaceSettings.fontPreset)
        applyLanguageContent(snapshot.languageCode, refreshRemote = true)
        player.setSkipSilence(snapshot.skipSilence)
'''
)

# Localized settings labels without expanding every translation map contract.
replace_once(
    strings,
    '''    val theme: String get() = value("theme")
    val themeSubtitle: String get() = value("themeSubtitle")
''',
    '''    val theme: String get() = value("theme")
    val themeSubtitle: String get() = value("themeSubtitle")
    val appFont: String get() = when (code) {
        "it" -> "Carattere dell'app"
        "es" -> "Fuente de la app"
        "fr" -> "Police de l’application"
        "de" -> "App-Schrift"
        "pt" -> "Fonte da aplicação"
        "nl" -> "App-lettertype"
        "pl" -> "Czcionka aplikacji"
        "ro" -> "Fontul aplicației"
        "el" -> "Γραμματοσειρά εφαρμογής"
        "sv" -> "Appens typsnitt"
        "da" -> "Appens skrifttype"
        "cs" -> "Písmo aplikace"
        "uk" -> "Шрифт застосунку"
        "ru" -> "Шрифт приложения"
        "tr" -> "Uygulama yazı tipi"
        "ar" -> "خط التطبيق"
        "zh" -> "应用字体"
        "ja" -> "アプリのフォント"
        "ko" -> "앱 글꼴"
        "hi" -> "ऐप फ़ॉन्ट"
        "id" -> "Font aplikasi"
        "vi" -> "Phông chữ ứng dụng"
        "th" -> "แบบอักษรของแอป"
        "fil" -> "Font ng app"
        "he" -> "גופן האפליקציה"
        else -> "App font"
    }
    val appFontSubtitle: String get() = when (code) {
        "it" -> "Scegli il font usato in tutta Levyra"
        "es" -> "Elige la tipografía usada en toda Levyra"
        "fr" -> "Choisissez la police utilisée dans Levyra"
        "de" -> "Wähle die Schriftart für ganz Levyra"
        "pt" -> "Escolha o tipo de letra usado em toda a Levyra"
        "nl" -> "Kies het lettertype voor heel Levyra"
        "pl" -> "Wybierz krój pisma używany w całej Levyra"
        "ro" -> "Alege fontul folosit în toată aplicația Levyra"
        "el" -> "Επίλεξε τη γραμματοσειρά για όλο το Levyra"
        "sv" -> "Välj typsnittet som används i hela Levyra"
        "da" -> "Vælg skrifttypen til hele Levyra"
        "cs" -> "Vyberte písmo používané v celé aplikaci Levyra"
        "uk" -> "Виберіть шрифт для всього Levyra"
        "ru" -> "Выберите шрифт для всего Levyra"
        "tr" -> "Levyra genelinde kullanılacak yazı tipini seç"
        "ar" -> "اختر الخط المستخدم في جميع أنحاء Levyra"
        "zh" -> "选择 Levyra 全局使用的字体"
        "ja" -> "Levyra 全体で使用するフォントを選択"
        "ko" -> "Levyra 전체에서 사용할 글꼴을 선택하세요"
        "hi" -> "पूरे Levyra में इस्तेमाल होने वाला फ़ॉन्ट चुनें"
        "id" -> "Pilih font yang digunakan di seluruh Levyra"
        "vi" -> "Chọn phông chữ dùng trong toàn bộ Levyra"
        "th" -> "เลือกแบบอักษรที่ใช้ทั่วทั้ง Levyra"
        "fil" -> "Piliin ang font na gagamitin sa buong Levyra"
        "he" -> "בחרו את הגופן שישמש בכל Levyra"
        else -> "Choose the typeface used across Levyra"
    }
'''
)

# Settings UI and occasional settings-button rotation.
replace_once(
    app,
    'import androidx.compose.material.icons.rounded.ViewCompact\n',
    'import androidx.compose.material.icons.rounded.ViewCompact\nimport androidx.compose.material.icons.rounded.TextFields\n'
)
replace_once(
    app,
    'import com.luc4n3x.levyra.domain.LevyraInterfaceSettings\n',
    'import com.luc4n3x.levyra.domain.LevyraInterfaceSettings\nimport com.luc4n3x.levyra.domain.LevyraFontPreset\n'
)
replace_once(
    app,
    '''                            userName = state.userName,
                            isResolving = state.isResolving,
                            onSearch = viewModel::openSearch,
''',
    '''                            userName = state.userName,
                            isResolving = state.isResolving,
                            animationsEnabled = state.animationsEnabled,
                            onSearch = viewModel::openSearch,
'''
)
replace_once(
    app,
    '''private fun GreetingBar(
    userName: String,
    isResolving: Boolean,
    onSearch: () -> Unit,
''',
    '''private fun GreetingBar(
    userName: String,
    isResolving: Boolean,
    animationsEnabled: Boolean,
    onSearch: () -> Unit,
'''
)
replace_once(
    app,
    '''        HomeHeaderIconButton(
            icon = Icons.Rounded.Settings,
            contentDescription = strings.settings,
            loading = isResolving,
            onClick = onSettings
        )
''',
    '''        OccasionallyRotatingSettingsButton(
            enabled = animationsEnabled && !isResolving,
            contentDescription = strings.settings,
            loading = isResolving,
            onClick = onSettings
        )
'''
)
replace_once(
    app,
    '''@Composable
private fun HomeHeaderIconButton(
''',
    '''@Composable
private fun OccasionallyRotatingSettingsButton(
    enabled: Boolean,
    contentDescription: String,
    loading: Boolean,
    onClick: () -> Unit
) {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(enabled) {
        rotation.snapTo(0f)
        if (!enabled) return@LaunchedEffect
        delay(6_000L)
        while (true) {
            rotation.animateTo(
                targetValue = 360f,
                animationSpec = tween(durationMillis = 780, easing = FastOutSlowInEasing)
            )
            rotation.snapTo(0f)
            delay(28_000L)
        }
    }
    Box(modifier = Modifier.graphicsLayer { rotationZ = rotation.value }) {
        HomeHeaderIconButton(
            icon = Icons.Rounded.Settings,
            contentDescription = contentDescription,
            loading = loading,
            onClick = onClick
        )
    }
}

@Composable
private fun HomeHeaderIconButton(
'''
)
replace_once(
    app,
    '''                                    ThemeSelector(selectedId = themePreset, onSelect = onThemePreset)
                                }
                            }
                            item {
                                SettingsToggle(
''',
    '''                                    ThemeSelector(selectedId = themePreset, onSelect = onThemePreset)
                                }
                            }
                            item {
                                SettingsChoiceRow(
                                    icon = Icons.Rounded.TextFields,
                                    title = strings.appFont,
                                    subtitle = strings.appFontSubtitle,
                                    options = LevyraFontPreset.entries.map { preset -> preset.name to preset.displayName },
                                    selected = interfaceSettings.fontPreset.name,
                                    onSelect = { value ->
                                        onInterfaceSettings(
                                            interfaceSettings.copy(fontPreset = LevyraFontPreset.from(value))
                                        )
                                    }
                                )
                            }
                            item {
                                SettingsToggle(
'''
)
