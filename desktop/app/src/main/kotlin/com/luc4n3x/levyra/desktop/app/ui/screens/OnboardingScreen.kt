package com.luc4n3x.levyra.desktop.app.ui.screens

import com.luc4n3x.levyra.ui.i18n.LevyraOnboardingAppearanceStrings
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.animateColorAsState
import com.luc4n3x.levyra.desktop.app.ui.components.CookieShape
import com.luc4n3x.levyra.desktop.core.model.ThemeMode
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.draw.drawBehind
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.desktop.app.ui.catalog.CountryOption
import com.luc4n3x.levyra.desktop.app.ui.catalog.LocaleCatalog
import com.luc4n3x.levyra.desktop.app.ui.components.CountryFlag
import com.luc4n3x.levyra.desktop.app.ui.components.levyraIconPainter
import com.luc4n3x.levyra.desktop.app.ui.components.tracksTextInputFocus
import com.luc4n3x.levyra.desktop.app.ui.i18n.LocalStrings
import com.luc4n3x.levyra.desktop.app.ui.i18n.stringsFor
import com.luc4n3x.levyra.desktop.app.ui.theme.LevyraBrand
import com.luc4n3x.levyra.desktop.core.model.AppLanguage
import com.luc4n3x.levyra.desktop.core.model.DesktopSettings

@Composable
fun OnboardingScreen(
    initialSettings: DesktopSettings,
    onLanguagePreview: (AppLanguage) -> Unit,
    onComplete: (String, AppLanguage, Set<String>, String) -> Unit,
    modifier: Modifier = Modifier,
    onThemePreview: (ThemeMode) -> Unit = {}
) {
    var step by remember { mutableStateOf(OnboardingStep.INTRO) }
    var theme by remember { mutableStateOf(initialSettings.themeMode) }
    var language by remember { mutableStateOf(initialSettings.language) }
    var name by remember { mutableStateOf(initialSettings.displayName) }
    var tastes by remember { mutableStateOf(initialSettings.selectedTasteIds) }
    var country by remember {
        mutableStateOf(
            initialSettings.contentCountry.ifBlank {
                LocaleCatalog.countryForLanguage(initialSettings.language).code
            }
        )
    }
    val strings = stringsFor(language, name)
    val layoutDirection = if (language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    val canAdvance = step != OnboardingStep.TASTE || tastes.size >= MIN_TASTES

    CompositionLocalProvider(
        LocalStrings provides strings,
        LocalLayoutDirection provides layoutDirection
    ) {
        Row(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            OnboardingHero(step = step, appName = strings.appName, modifier = Modifier.weight(0.42f).fillMaxHeight())
            Column(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight()
                    .padding(horizontal = 56.dp, vertical = 44.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                OnboardingProgress(step)
                AnimatedContent(
                    targetState = step,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(140)) },
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 28.dp)
                ) { current ->
                    when (current) {
                        OnboardingStep.INTRO -> IntroStep(strings.onboardingWelcomeBadge, strings.onboardingWelcomeTitle)
                        OnboardingStep.LANGUAGE -> LanguageStep(
                            selected = language,
                            onSelect = { selected ->
                                language = selected
                                country = selected.defaultCountry
                                onLanguagePreview(selected)
                            }
                        )
                        OnboardingStep.APPEARANCE -> AppearanceStep(language, theme) { mode ->
                            theme = mode
                            onThemePreview(mode)
                        }
                        OnboardingStep.PROFILE -> ProfileStep(
                            name = name,
                            onNameChange = { value -> name = value.take(DesktopSettings.MAX_DISPLAY_NAME_LENGTH) }
                        )
                        OnboardingStep.TASTE -> TasteStep(
                            language = language,
                            selected = tastes,
                            onToggle = { id -> tastes = if (id in tastes) tastes - id else tastes + id }
                        )
                        OnboardingStep.REGION -> RegionStep(selectedCode = country, onSelect = { country = it.code })
                    }
                }
                OnboardingFooter(
                    step = step,
                    canAdvance = canAdvance,
                    onBack = { step = step.previous() },
                    onSkip = { step = step.next() },
                    onAdvance = {
                        if (step == OnboardingStep.REGION) onComplete(name.trim(), language, tastes, country) else step = step.next()
                    }
                )
            }
        }
    }
}

@Composable
private fun OnboardingHero(step: OnboardingStep, appName: String, modifier: Modifier = Modifier) {
    val accent by animateColorAsState(step.accent, tween(420))
    val surface = MaterialTheme.colorScheme.surfaceContainerLowest
    Box(
        modifier
            .background(surface)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        listOf(accent.copy(alpha = 0.34f), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.46f),
                        radius = size.minDimension * 0.75f
                    )
                )
            }
            .padding(40.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(levyraIconPainter(), contentDescription = null, modifier = Modifier.size(40.dp))
            Text(appName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Box(
            Modifier
                .align(Alignment.Center)
                .size(300.dp)
                .clip(CookieShape)
                .background(accent),
            contentAlignment = Alignment.Center
        ) {
            if (step == OnboardingStep.INTRO) {
                Image(levyraIconPainter(), contentDescription = null, modifier = Modifier.size(210.dp))
            } else {
                Icon(step.glyph, contentDescription = null, tint = Color(0xFF14121C), modifier = Modifier.size(120.dp))
            }
        }
    }
}

@Composable
private fun OnboardingProgress(step: OnboardingStep) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OnboardingStep.entries.forEach { item ->
            Box(
                Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(if (item.ordinal <= step.ordinal) step.accent else MaterialTheme.colorScheme.surfaceContainerHighest)
            )
        }
    }
}

@Composable
private fun IntroStep(badge: String, title: String) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text(badge.uppercase(), style = MaterialTheme.typography.labelLarge, color = LevyraBrand.soft)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AppearanceStep(language: AppLanguage, selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val copy = remember(language) { LevyraOnboardingAppearanceStrings.forCode(language.tag) }
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        StepHeader(copy.title, copy.body)
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            listOf(
                Triple(ThemeMode.DARK, copy.dark, copy.darkBody),
                Triple(ThemeMode.LIGHT, copy.light, copy.lightBody)
            ).forEach { (mode, title, body) ->
                val active = selected == mode
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(28.dp))
                        .background(if (active) LevyraBrand.soft.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainer)
                        .border(2.dp, if (active) LevyraBrand.soft else Color.Transparent, RoundedCornerShape(28.dp))
                        .clickable { onSelect(mode) }
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(20.dp))
                            .background(if (mode == ThemeMode.DARK) Color(0xFF0B0B0C) else Color(0xFFF3F3F5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(Modifier.size(64.dp).clip(CookieShape).background(LevyraBrand.soft))
                    }
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Text(copy.footnote, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun OnboardingFooter(
    step: OnboardingStep,
    canAdvance: Boolean,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onAdvance: () -> Unit
) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 22.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (step == OnboardingStep.INTRO) {
            Spacer(modifier = Modifier.width(112.dp))
        } else {
            OutlinedButton(onClick = onBack) {
                Text(strings.onboardingBack)
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (step != OnboardingStep.REGION && step != OnboardingStep.INTRO) {
                TextButton(onClick = onSkip) {
                    Text(strings.onboardingSkip)
                }
            }
            Button(
                enabled = canAdvance,
                onClick = onAdvance,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = LevyraBrand.soft, contentColor = LevyraBrand.onSoft),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp)
            ) {
                Text(
                    if (step == OnboardingStep.REGION) {
                        strings.onboardingStart
                    } else {
                        strings.onboardingContinue
                    }
                )
            }
        }
    }
}

@Composable
private fun LanguageStep(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit
) {
    val strings = LocalStrings.current
    Column(modifier = Modifier.fillMaxSize()) {
        StepHeader(
            title = strings.onboardingLanguageQuestion,
            subtitle = strings.onboardingWelcomeTitle
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(205.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(LocaleCatalog.languages, key = { it.tag }) { language ->
                LanguageCard(
                    language = language,
                    selected = language == selected,
                    onClick = { onSelect(language) }
                )
            }
        }
    }
}

@Composable
private fun LanguageCard(
    language: AppLanguage,
    selected: Boolean,
    onClick: () -> Unit
) {
    SelectableCard(
        selected = selected,
        onClick = onClick,
        height = 78.dp
    ) {
        CountryFlag(
            countryCode = language.defaultCountry,
            modifier = Modifier.width(42.dp).height(28.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = language.nativeName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (language.nativeName != language.englishName) {
                Text(
                    text = language.englishName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (selected) {
            Surface(
                modifier = Modifier.size(24.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "✓",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileStep(name: String, onNameChange: (String) -> Unit) {
    val strings = LocalStrings.current
    Column(modifier = Modifier.fillMaxSize()) {
        StepHeader(
            title = strings.onboardingNameQuestion,
            subtitle = strings.settingsProfile
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(30.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = strings.onboardingWelcomeBadge,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    placeholder = { Text(strings.onboardingNamePlaceholder) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().tracksTextInputFocus()
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = stringsFor(AppLanguage.fromTag(strings.languageCode), name).homeGreeting,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TasteStep(
    language: AppLanguage,
    selected: Set<String>,
    onToggle: (String) -> Unit
) {
    val strings = LocalStrings.current
    val options = remember(language) { tasteOptions(language) }
    Column(modifier = Modifier.fillMaxSize()) {
        StepHeader(
            title = strings.onboardingTasteQuestion,
            subtitle = "${selected.size}/$MIN_TASTES"
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(190.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(options, key = { it.id }) { option ->
                SelectableCard(
                    selected = option.id in selected,
                    onClick = { onToggle(option.id) },
                    height = 76.dp
                ) {
                    if (option.id == "local") {
                        CountryFlag(
                            countryCode = language.defaultCountry,
                            modifier = Modifier.width(38.dp).height(25.dp)
                        )
                    } else {
                        Text(text = option.emoji, fontSize = 24.sp)
                    }
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (option.id in selected) {
                        Text(
                            text = "✓",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RegionStep(
    selectedCode: String,
    onSelect: (CountryOption) -> Unit
) {
    val strings = LocalStrings.current
    Column(modifier = Modifier.fillMaxSize()) {
        StepHeader(
            title = strings.onboardingRegionQuestion,
            subtitle = strings.chartsSubtitle
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(205.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(LocaleCatalog.countries, key = { it.code }) { option ->
                SelectableCard(
                    selected = option.code.equals(selectedCode, ignoreCase = true),
                    onClick = { onSelect(option) },
                    height = 78.dp
                ) {
                    CountryFlag(
                        countryCode = option.code,
                        modifier = Modifier.width(42.dp).height(28.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = option.nativeName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = option.englishName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = option.code,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StepHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SelectableCard(
    selected: Boolean,
    onClick: () -> Unit,
    height: androidx.compose.ui.unit.Dp,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant
        ),
        shadowElevation = if (selected) 8.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    else MaterialTheme.colorScheme.surfaceContainer
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

private enum class OnboardingStep(val accent: Color, val glyph: ImageVector) {
    INTRO(Color(0xFF98CFEA), LevyraIcons.Disc),
    LANGUAGE(Color(0xFF9FB2FF), LevyraIcons.Chart),
    APPEARANCE(Color(0xFFB9A2F5), LevyraIcons.Settings),
    PROFILE(Color(0xFFF2A4C4), LevyraIcons.Artist),
    TASTE(Color(0xFFF4C27A), LevyraIcons.HeartFilled),
    REGION(Color(0xFF8FD9B6), LevyraIcons.Radio);

    fun next(): OnboardingStep = entries.getOrElse(ordinal + 1) { this }

    fun previous(): OnboardingStep = entries.getOrElse(ordinal - 1) { this }
}

private data class TasteOption(val id: String, val emoji: String, val label: String)

private fun tasteOptions(language: AppLanguage): List<TasteOption> {
    val labels = tasteLabels[language.tag] ?: tasteLabels.getValue("en")
    val ids = listOf("hits", "rap", "local", "pop", "gym", "chill", "focus", "sad", "party", "rock", "electro", "rnb")
    val emojis = listOf("🔥", "🎤", "", "✨", "🏋️", "😌", "🎧", "💔", "🎉", "🎸", "🎛️", "🕺")
    return ids.indices.map { index -> TasteOption(ids[index], emojis[index], labels[index]) }
}

private val tasteLabels = mapOf(
    "en" to listOf("Hits", "Rap", "Local", "Pop", "Workout", "Chill", "Focus", "Melancholy", "Party", "Rock", "Electronic", "R&B"),
    "it" to listOf("Hit", "Rap", "Italiana", "Pop", "Allenamento", "Chill", "Focus", "Malinconia", "Festa", "Rock", "Elettronica", "R&B"),
    "es" to listOf("Éxitos", "Rap", "Local", "Pop", "Entrenamiento", "Relax", "Concentración", "Melancolía", "Fiesta", "Rock", "Electrónica", "R&B"),
    "fr" to listOf("Hits", "Rap", "Locale", "Pop", "Sport", "Détente", "Concentration", "Mélancolie", "Fête", "Rock", "Électro", "R&B"),
    "de" to listOf("Hits", "Rap", "Lokal", "Pop", "Training", "Chill", "Fokus", "Melancholie", "Party", "Rock", "Elektronisch", "R&B"),
    "pt" to listOf("Hits", "Rap", "Local", "Pop", "Treino", "Relax", "Foco", "Melancolia", "Festa", "Rock", "Eletrônica", "R&B"),
    "nl" to listOf("Hits", "Rap", "Lokaal", "Pop", "Training", "Chill", "Focus", "Melancholie", "Feest", "Rock", "Elektronisch", "R&B"),
    "pl" to listOf("Hity", "Rap", "Lokalne", "Pop", "Trening", "Chill", "Skupienie", "Melancholia", "Impreza", "Rock", "Elektronika", "R&B"),
    "ro" to listOf("Hituri", "Rap", "Local", "Pop", "Antrenament", "Relaxare", "Concentrare", "Melancolie", "Petrecere", "Rock", "Electronică", "R&B"),
    "el" to listOf("Επιτυχίες", "Ραπ", "Τοπικά", "Ποπ", "Προπόνηση", "Χαλάρωση", "Συγκέντρωση", "Μελαγχολία", "Πάρτι", "Ροκ", "Ηλεκτρονική", "R&B"),
    "sv" to listOf("Hits", "Rap", "Lokalt", "Pop", "Träning", "Chill", "Fokus", "Melankoli", "Fest", "Rock", "Elektroniskt", "R&B"),
    "da" to listOf("Hits", "Rap", "Lokalt", "Pop", "Træning", "Chill", "Fokus", "Melankoli", "Fest", "Rock", "Elektronisk", "R&B"),
    "cs" to listOf("Hity", "Rap", "Místní", "Pop", "Trénink", "Chill", "Soustředění", "Melancholie", "Párty", "Rock", "Elektronika", "R&B"),
    "uk" to listOf("Хіти", "Реп", "Українське", "Поп", "Тренування", "Релакс", "Фокус", "Меланхолія", "Вечірка", "Рок", "Електроніка", "R&B"),
    "ru" to listOf("Хиты", "Рэп", "Местное", "Поп", "Тренировка", "Релакс", "Фокус", "Меланхолия", "Вечеринка", "Рок", "Электроника", "R&B"),
    "tr" to listOf("Hitler", "Rap", "Yerel", "Pop", "Antrenman", "Rahat", "Odak", "Melankoli", "Parti", "Rock", "Elektronik", "R&B"),
    "ar" to listOf("الأكثر رواجًا", "راب", "محلي", "بوب", "تمارين", "استرخاء", "تركيز", "حزن", "حفلة", "روك", "إلكترونية", "R&B"),
    "zh" to listOf("热门", "说唱", "华语", "流行", "健身", "放松", "专注", "伤感", "派对", "摇滚", "电子", "R&B"),
    "ja" to listOf("ヒット", "ラップ", "邦楽", "ポップ", "ワークアウト", "チル", "集中", "メランコリー", "パーティー", "ロック", "エレクトロ", "R&B"),
    "ko" to listOf("인기곡", "랩", "국내 음악", "팝", "운동", "휴식", "집중", "감성", "파티", "록", "일렉트로닉", "R&B"),
    "hi" to listOf("हिट", "रैप", "स्थानीय", "पॉप", "वर्कआउट", "चिल", "फोकस", "उदासी", "पार्टी", "रॉक", "इलेक्ट्रॉनिक", "R&B"),
    "id" to listOf("Hits", "Rap", "Lokal", "Pop", "Olahraga", "Santai", "Fokus", "Melankolis", "Pesta", "Rock", "Elektronik", "R&B"),
    "vi" to listOf("Thịnh hành", "Rap", "Nhạc Việt", "Pop", "Tập luyện", "Thư giãn", "Tập trung", "Tâm trạng", "Tiệc", "Rock", "Điện tử", "R&B"),
    "th" to listOf("เพลงฮิต", "แรป", "เพลงไทย", "ป๊อป", "ออกกำลังกาย", "ชิล", "โฟกัส", "เศร้า", "ปาร์ตี้", "ร็อก", "อิเล็กทรอนิกส์", "R&B"),
    "fil" to listOf("Mga hit", "Rap", "OPM", "Pop", "Workout", "Chill", "Focus", "Malungkot", "Party", "Rock", "Electronic", "R&B"),
    "he" to listOf("להיטים", "ראפ", "מקומי", "פופ", "אימון", "רגוע", "ריכוז", "מלנכוליה", "מסיבה", "רוק", "אלקטרוני", "R&B")
)

private const val MIN_TASTES = 3
