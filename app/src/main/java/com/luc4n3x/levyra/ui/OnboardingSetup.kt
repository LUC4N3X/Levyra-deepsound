package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.ui.components.LevyraBackButton
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Nightlife
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.domain.LevyraLanguageOption
import com.luc4n3x.levyra.domain.MoodEngine
import com.luc4n3x.levyra.domain.Taste
import com.luc4n3x.levyra.ui.components.LevyraMorphShape
import com.luc4n3x.levyra.ui.components.LevyraWavyProgress
import com.luc4n3x.levyra.ui.components.levyraGroupedListShape
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicAccount
import com.luc4n3x.levyra.ui.i18n.LevyraOnboardingYoutubeMusicStrings
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.OnboardingYoutubeMusicCopy
import com.luc4n3x.levyra.ui.i18n.youtubeMusicCopy
import com.luc4n3x.levyra.ui.youtubemusic.YoutubeMusicLoginScreen
import com.luc4n3x.levyra.viewmodel.youtubemusic.YoutubeMusicAccountError
import com.luc4n3x.levyra.viewmodel.youtubemusic.YoutubeMusicAccountUiState
import com.luc4n3x.levyra.ui.theme.LevyraTypeRhythm
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.semantics.Role
import com.luc4n3x.levyra.ui.i18n.LevyraOnboardingAppearanceStrings
import com.luc4n3x.levyra.ui.i18n.OnboardingAppearanceCopy
import com.luc4n3x.levyra.ui.theme.LevyraPalette
import com.luc4n3x.levyra.ui.theme.LevyraThemes
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape

private val SetupBase: Color get() = if (LevyraIsLight) Color(0xFFF5F7FB) else Color(0xFF07070B)
private val SetupInk: Color get() = if (LevyraIsLight) Color(0xFF10141C) else Color(0xFFF4F4F8)
private val SetupMuted: Color get() = if (LevyraIsLight) Color(0xFF59606E) else Color(0xFFA6A8B5)
private val SetupSurface: Color get() = if (LevyraIsLight) Color(0xFFFFFFFF) else Color(0xFF15151D)
private val SetupSurfaceHigh: Color get() = if (LevyraIsLight) Color(0xFFE6EBF3) else Color(0xFF1F1F2A)
private val SetupOnAccent: Color get() = if (LevyraIsLight) Color(0xFFFFFFFF) else Color(0xFF0A0A10)
private val SetupVeil: Color get() = if (LevyraIsLight) Color.Black else Color.White
private val SetupLilac: Color get() = if (LevyraIsLight) Color(0xFF6346D0) else Color(0xFFB7A2FF)
private val SetupMint: Color get() = if (LevyraIsLight) Color(0xFF2F7A47) else Color(0xFFA6E3B5)
private val SetupCoral: Color get() = if (LevyraIsLight) Color(0xFFB8442B) else Color(0xFFFF9B85)
private val SetupSun: Color get() = if (LevyraIsLight) Color(0xFF8A6100) else Color(0xFFFFD36E)
private val SetupSky: Color get() = if (LevyraIsLight) Color(0xFF1767A8) else Color(0xFF8FD3FF)
private val SetupPeach: Color get() = if (LevyraIsLight) Color(0xFF9C4317) else Color(0xFFFFB693)
private val SetupRose: Color get() = if (LevyraIsLight) Color(0xFF9E2F66) else Color(0xFFFFAFD2)
private val SetupLavender: Color get() = if (LevyraIsLight) Color(0xFF5B43C2) else Color(0xFFCDBDFF)
private val SetupOnHero: Color get() = if (LevyraIsLight) Color.White else Color(0xFF2A1220)
private val SetupHeroGradient: Brush
    get() = Brush.linearGradient(listOf(SetupPeach, SetupRose, SetupLavender))
private val SetupError: Color get() = if (LevyraIsLight) Color(0xFFBA1A1A) else Color(0xFFFFB4AB)
private val SetupContentMaxWidth = 560.dp
private val SetupHeroIntroSize = 112.dp
private val SetupHeroStepSize = 88.dp
private const val SetupHeroTurnDegrees = 45f
private const val SetupLogoSpinMillis = 36_000

private fun onboardingAccent(step: OnboardingStep): Color = when (step) {
    OnboardingStep.Intro -> SetupRose
    OnboardingStep.Language -> SetupLilac
    OnboardingStep.Appearance -> SetupMint
    OnboardingStep.Profile -> SetupCoral
    OnboardingStep.Taste -> SetupSun
    OnboardingStep.YoutubeMusic -> SetupSky
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun onboardingHeroPolygon(step: OnboardingStep): RoundedPolygon = when (step) {
    OnboardingStep.Intro -> MaterialShapes.Cookie12Sided
    OnboardingStep.Language -> MaterialShapes.Clover4Leaf
    OnboardingStep.Appearance -> MaterialShapes.Sunny
    OnboardingStep.Profile -> MaterialShapes.Cookie9Sided
    OnboardingStep.Taste -> MaterialShapes.SoftBurst
    OnboardingStep.YoutubeMusic -> MaterialShapes.Flower
}

@Composable
internal fun OnboardingOverlay(
    selectedLanguageCode: String,
    artwork: Map<String, List<String>>,
    youtubeMusic: YoutubeMusicAccountUiState,
    onTasteLanguage: (String) -> Unit,
    onConnectYoutubeMusic: (String) -> Unit,
    onYoutubeMusicCookie: (String) -> Unit,
    onCancelYoutubeMusicLogin: () -> Unit,
    onAppearance: (Boolean) -> Unit,
    onDone: (String, Set<String>, String) -> Unit
) {
    val currentLocale = LocalLocale.current.platformLocale
    val deviceLanguageCode = remember(currentLocale, selectedLanguageCode) {
        LevyraLanguageCatalog.normalize(currentLocale.toLanguageTag().ifBlank { selectedLanguageCode })
    }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var name by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(OnboardingStep.Intro) }
    var languageCode by remember(deviceLanguageCode) { mutableStateOf(deviceLanguageCode) }
    val moodEngine = remember { MoodEngine() }
    val tastes = remember(languageCode) { moodEngine.tastesForLanguage(languageCode) }
    val strings = LevyraStrings.forCode(languageCode)
    val youtubeMusicCopy = LevyraOnboardingYoutubeMusicStrings.forCode(languageCode)
    val youtubeMusicAccount = youtubeMusic.active
    val youtubeMusicError = youtubeMusic.error?.takeIf { youtubeMusicAccount == null && !youtubeMusic.restoring }
    val animated = LocalAnimationsEnabled.current
    LaunchedEffect(languageCode) { onTasteLanguage(languageCode) }
    val accent by animateColorAsState(
        targetValue = onboardingAccent(step),
        animationSpec = if (animated) tween(520) else snap(),
        label = "setup-accent"
    )
    val introLevel = animateFloatAsState(
        targetValue = if (step == OnboardingStep.Intro) 1f else 0f,
        animationSpec = if (animated) tween(520) else snap(),
        label = "setup-intro-level"
    )
    val finish: () -> Unit = { onDone(name, selected, languageCode) }
    val advance: () -> Unit = {
        when {
            step != OnboardingStep.YoutubeMusic -> step = step.next()
            youtubeMusicAccount != null -> finish()
            else -> onConnectYoutubeMusic(languageCode)
        }
    }
    BackHandler(enabled = step != OnboardingStep.Intro) { step = step.previous() }
    val layoutDirection = if (LevyraLanguageCatalog.isRtl(languageCode)) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SetupBase)
                .setupAmbience(accent) { introLevel.value }
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = SetupContentMaxWidth)
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .windowInsetsPadding(LevyraHorizontalSafeInsets)
            ) {
                SetupHeader(step = step, accent = accent, backLabel = strings.back, onBack = { step = step.previous() })
                SetupHero(
                    step = step,
                    accent = accent,
                    initial = name.trim().take(1).uppercase(currentLocale),
                    selectedTasteCount = selected.size,
                    youtubeMusicConnected = youtubeMusicAccount != null,
                    animated = animated
                )
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        val direction = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                        val slide = spring<androidx.compose.ui.unit.IntOffset>(dampingRatio = 0.9f, stiffness = 380f)
                        slideInHorizontally(slide) { it * direction / 4 } + fadeIn(tween(260, delayMillis = 60)) togetherWith
                            slideOutHorizontally(slide) { -it * direction / 4 } + fadeOut(tween(140))
                    },
                    modifier = Modifier.weight(1f),
                    label = "setup-step"
                ) { activeStep ->
                    SetupStepContent(
                        step = activeStep,
                        strings = strings,
                        languageCode = languageCode,
                        accent = accent,
                        name = name,
                        tastes = tastes,
                        artwork = artwork,
                        selected = selected,
                        youtubeMusicCopy = youtubeMusicCopy,
                        youtubeMusicAccount = youtubeMusicAccount,
                        youtubeMusicError = youtubeMusicError,
                        animated = animated,
                        onLanguage = { languageCode = it },
                        onAppearance = onAppearance,
                        onName = { name = preserveProfileNameInput(it) },
                        onSubmit = advance,
                        onToggleTaste = { tasteId ->
                            selected = if (tasteId in selected) selected - tasteId else selected + tasteId
                        }
                    )
                }
                SetupFooter(
                    hero = step == OnboardingStep.Intro,
                    label = setupPrimaryLabel(step, strings, youtubeMusicCopy, youtubeMusicAccount != null, youtubeMusicError != null),
                    accent = accent,
                    enabled = setupPrimaryEnabled(step, selected.size, youtubeMusic),
                    skipLabel = setupSkipLabel(step, selected.size, strings, youtubeMusicCopy, youtubeMusicAccount != null),
                    onPrimary = advance,
                    onSkip = { if (step == OnboardingStep.Taste) step = step.next() else finish() }
                )
            }
            if (youtubeMusic.showLogin) {
                SetupYoutubeMusicLogin(
                    languageCode = languageCode,
                    backLabel = strings.back,
                    copy = youtubeMusicCopy,
                    state = youtubeMusic,
                    onCookie = onYoutubeMusicCookie,
                    onClose = onCancelYoutubeMusicLogin
                )
            }
        }
    }
}

@Composable
private fun SetupStepContent(
    step: OnboardingStep,
    strings: LevyraStrings,
    languageCode: String,
    accent: Color,
    name: String,
    tastes: List<Taste>,
    artwork: Map<String, List<String>>,
    selected: Set<String>,
    youtubeMusicCopy: OnboardingYoutubeMusicCopy,
    youtubeMusicAccount: YoutubeMusicAccount?,
    youtubeMusicError: YoutubeMusicAccountError?,
    animated: Boolean,
    onLanguage: (String) -> Unit,
    onAppearance: (Boolean) -> Unit,
    onName: (String) -> Unit,
    onSubmit: () -> Unit,
    onToggleTaste: (String) -> Unit
) {
    when (step) {
        OnboardingStep.Intro -> SetupIntroStage(strings = strings, accent = accent)
        OnboardingStep.Language -> SetupLanguageStage(
            strings = strings,
            languageCode = languageCode,
            accent = accent,
            onLanguage = onLanguage
        )
        OnboardingStep.Appearance -> SetupAppearanceStage(
            copy = LevyraOnboardingAppearanceStrings.forCode(languageCode),
            light = LevyraIsLight,
            accent = accent,
            onLight = onAppearance
        )
        OnboardingStep.Profile -> SetupProfileStage(
            strings = strings,
            name = name,
            accent = accent,
            onName = onName,
            onSubmit = onSubmit
        )
        OnboardingStep.Taste -> SetupTasteStage(
            strings = strings,
            tastes = tastes,
            artwork = artwork,
            selected = selected,
            accent = accent,
            onToggle = onToggleTaste
        )
        OnboardingStep.YoutubeMusic -> SetupYoutubeMusicStage(
            copy = youtubeMusicCopy,
            account = youtubeMusicAccount,
            error = youtubeMusicError?.let { onboardingYoutubeMusicErrorText(youtubeMusicCopy, it) },
            accent = accent,
            animated = animated
        )
    }
}

private fun setupPrimaryLabel(
    step: OnboardingStep,
    strings: LevyraStrings,
    copy: OnboardingYoutubeMusicCopy,
    connected: Boolean,
    failed: Boolean
): String = when {
    step == OnboardingStep.Intro -> strings.introStart
    step != OnboardingStep.YoutubeMusic -> strings.next
    connected -> strings.startListening
    failed -> copy.retry
    else -> copy.connect
}

private fun setupPrimaryEnabled(step: OnboardingStep, selectedTasteCount: Int, youtubeMusic: YoutubeMusicAccountUiState): Boolean =
    if (step == OnboardingStep.YoutubeMusic) youtubeMusic.active != null || !youtubeMusic.restoring
    else onboardingPrimaryEnabled(step, selectedTasteCount)

private fun setupSkipLabel(
    step: OnboardingStep,
    selectedTasteCount: Int,
    strings: LevyraStrings,
    copy: OnboardingYoutubeMusicCopy,
    connected: Boolean
): String? = when {
    step == OnboardingStep.Taste && selectedTasteCount < 3 -> strings.skipAndContinue
    step == OnboardingStep.YoutubeMusic && !connected -> copy.skip
    else -> null
}

@Composable
private fun SetupYoutubeMusicLogin(
    languageCode: String,
    backLabel: String,
    copy: OnboardingYoutubeMusicCopy,
    state: YoutubeMusicAccountUiState,
    onCookie: (String) -> Unit,
    onClose: () -> Unit
) {
    YoutubeMusicLoginScreen(
        copy = youtubeMusicCopy(languageCode).copy(
            login = copy.login,
            back = backLabel,
            checking = copy.checking,
            retry = copy.retry,
            networkError = copy.networkError
        ),
        checking = state.checkingLogin,
        error = state.error?.let { onboardingYoutubeMusicErrorText(copy, it) },
        onCookie = onCookie,
        onClose = onClose
    )
}

private fun onboardingYoutubeMusicErrorText(copy: OnboardingYoutubeMusicCopy, error: YoutubeMusicAccountError): String =
    when (error) {
        YoutubeMusicAccountError.Network -> copy.networkError
        YoutubeMusicAccountError.SignIn -> copy.signInError
        else -> copy.genericError
    }

private fun Modifier.setupAmbience(accent: Color, hero: () -> Float): Modifier = drawBehind {
    val heroLevel = hero().coerceIn(0f, 1f)
    if (heroLevel > 0f) {
        val strength = if (LevyraIsLight) 0.20f else 0.30f
        drawRect(
            Brush.radialGradient(
                colors = listOf(SetupLavender.copy(alpha = strength * heroLevel), Color.Transparent),
                center = Offset(size.width * 0.04f, size.height * 0.08f),
                radius = size.maxDimension * 0.58f
            )
        )
        drawRect(
            Brush.radialGradient(
                colors = listOf(SetupRose.copy(alpha = strength * 0.9f * heroLevel), Color.Transparent),
                center = Offset(size.width * 0.98f, size.height * 0.30f),
                radius = size.maxDimension * 0.5f
            )
        )
        drawRect(
            Brush.radialGradient(
                colors = listOf(SetupPeach.copy(alpha = strength * 0.75f * heroLevel), Color.Transparent),
                center = Offset(size.width * 0.5f, size.height * 1.02f),
                radius = size.maxDimension * 0.6f
            )
        )
    }
    val stepLevel = 1f - heroLevel
    if (stepLevel <= 0f) return@drawBehind
    val glow = Brush.radialGradient(
        colors = listOf(accent.copy(alpha = 0.26f * stepLevel), accent.copy(alpha = 0.06f * stepLevel), Color.Transparent),
        center = Offset(size.width * 0.82f, size.height * 0.06f),
        radius = size.maxDimension * 0.62f
    )
    drawRect(glow)
    val depth = Brush.radialGradient(
        colors = listOf(SetupLilac.copy(alpha = 0.12f * stepLevel), Color.Transparent),
        center = Offset(size.width * 0.08f, size.height * 0.96f),
        radius = size.maxDimension * 0.55f
    )
    drawRect(depth)
}

@Composable
private fun SetupHeader(step: OnboardingStep, accent: Color, backLabel: String, onBack: () -> Unit) {
    AnimatedVisibility(
        visible = onboardingShowsChrome(step),
        enter = fadeIn(tween(220)),
        exit = fadeOut(tween(160))
    ) {
        val steps = onboardingProgressSteps()
        val index = onboardingProgressIndex(step)
        val progress by animateFloatAsState(
            targetValue = (index + 1f) / steps.size,
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
            label = "setup-progress"
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 24.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LevyraBackButton(
                onClick = onBack,
                contentDescription = backLabel,
                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = SetupSurfaceHigh, contentColor = SetupInk)
            )
            LevyraWavyProgress(
                progress = progress,
                color = accent,
                trackColor = SetupVeil.copy(alpha = 0.12f),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${index + 1}/${steps.size}",
                color = SetupMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SetupHero(
    step: OnboardingStep,
    accent: Color,
    initial: String,
    selectedTasteCount: Int,
    youtubeMusicConnected: Boolean,
    animated: Boolean
) {
    val intro = step == OnboardingStep.Intro
    val heroSpring = spring<Dp>(dampingRatio = 0.82f, stiffness = 260f)
    val size by animateDpAsState(
        targetValue = if (intro) SetupHeroIntroSize else SetupHeroStepSize,
        animationSpec = if (animated) heroSpring else snap(),
        label = "setup-hero-size"
    )
    val bias by animateFloatAsState(
        targetValue = if (intro) 0f else -1f,
        animationSpec = if (animated) spring(dampingRatio = 0.86f, stiffness = 260f) else snap(),
        label = "setup-hero-bias"
    )
    val turn by animateFloatAsState(
        targetValue = step.ordinal * SetupHeroTurnDegrees,
        animationSpec = if (animated) spring(dampingRatio = 0.62f, stiffness = 200f) else snap(),
        label = "setup-hero-turn"
    )
    val shape = rememberSetupHeroShape(step, animated)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = if (intro) 16.dp else 12.dp, bottom = if (intro) 0.dp else 4.dp)
    ) {
        AnimatedVisibility(
            visible = intro,
            enter = fadeIn(tween(240)) + scaleIn(spring(dampingRatio = 0.7f, stiffness = 300f), initialScale = 0.8f),
            exit = fadeOut(tween(140)) + scaleOut(targetScale = 0.8f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            SetupLogoBloom(accent = accent, animated = animated)
        }
        if (!intro) Box(
            modifier = Modifier
                .align(BiasAlignment(bias, 0f))
                .size(size)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(accent.copy(alpha = 0.42f), Color.Transparent),
                            radius = this.size.minDimension * 0.82f
                        ),
                        radius = this.size.minDimension * 0.82f
                    )
                }
                .graphicsLayer { rotationZ = turn }
                .clip(shape)
                .background(Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.72f), SetupLilac.copy(alpha = 0.85f)))),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    fadeIn(tween(220, delayMillis = 80)) + scaleIn(spring(dampingRatio = 0.6f, stiffness = 420f), initialScale = 0.6f) togetherWith
                        fadeOut(tween(120)) + scaleOut(targetScale = 0.6f)
                },
                modifier = Modifier.graphicsLayer { rotationZ = -turn },
                label = "setup-hero-glyph"
            ) { glyphStep ->
                SetupHeroGlyph(
                    step = glyphStep,
                    initial = initial,
                    selectedTasteCount = selectedTasteCount,
                    youtubeMusicConnected = youtubeMusicConnected
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SetupLogoBloom(accent: Color, animated: Boolean) {
    val outerShape = MaterialShapes.SoftBurst.toShape()
    val innerShape = MaterialShapes.Cookie9Sided.toShape()
    val bloom = remember { Animatable(if (animated) 0f else 1f) }
    LaunchedEffect(animated) {
        if (animated) bloom.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 140f)) else bloom.snapTo(1f)
    }
    val spin = if (animated) {
        val transition = rememberInfiniteTransition(label = "setup-logo-spin")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(SetupLogoSpinMillis, easing = LinearEasing), RepeatMode.Restart),
            label = "setup-logo-spin-value"
        )
    } else {
        null
    }
    Box(
        modifier = Modifier.size(SetupHeroIntroSize * 1.55f),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val value = bloom.value
                    scaleX = 0.55f + 0.45f * value
                    scaleY = 0.55f + 0.45f * value
                    alpha = value.coerceIn(0f, 1f)
                    rotationZ = (spin?.value ?: 0f) - 40f * (1f - value)
                }
                .border(
                    width = 1.5.dp,
                    brush = Brush.sweepGradient(listOf(SetupPeach, SetupRose, SetupLavender, SetupPeach.copy(alpha = 0.2f), SetupPeach)),
                    shape = outerShape
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize(0.76f)
                .graphicsLayer {
                    val value = ((bloom.value - 0.15f) / 0.85f).coerceIn(0f, 1.2f)
                    scaleX = 0.6f + 0.4f * value
                    scaleY = 0.6f + 0.4f * value
                    alpha = value.coerceIn(0f, 1f)
                    rotationZ = -(spin?.value ?: 0f) * 1.6f + 60f * (1f - value)
                }
                .background(
                    Brush.linearGradient(
                        listOf(SetupPeach.copy(alpha = 0.16f), SetupRose.copy(alpha = 0.10f), SetupLavender.copy(alpha = 0.16f))
                    ),
                    innerShape
                )
        )
        Box(
            modifier = Modifier.graphicsLayer {
                val value = ((bloom.value - 0.25f) / 0.75f).coerceIn(0f, 1.15f)
                scaleX = 0.7f + 0.3f * value
                scaleY = 0.7f + 0.3f * value
                alpha = value.coerceIn(0f, 1f)
            }
        ) {
            LevyraLogoMark(size = SetupHeroIntroSize)
        }
    }
}

@Composable
private fun SetupHeroGlyph(step: OnboardingStep, initial: String, selectedTasteCount: Int, youtubeMusicConnected: Boolean) {
    when (step) {
        OnboardingStep.Intro -> Unit
        OnboardingStep.Language -> Icon(
            Icons.Rounded.Translate,
            contentDescription = null,
            tint = SetupOnAccent,
            modifier = Modifier.size(36.dp)
        )
        OnboardingStep.Appearance -> Icon(
            Icons.Rounded.Contrast,
            contentDescription = null,
            tint = SetupOnAccent,
            modifier = Modifier.size(36.dp)
        )
        OnboardingStep.Profile -> if (initial.isBlank()) {
            Icon(Icons.Rounded.Person, contentDescription = null, tint = SetupOnAccent, modifier = Modifier.size(38.dp))
        } else {
            Text(initial, color = SetupOnAccent, fontSize = 38.sp, fontWeight = FontWeight.Black)
        }
        OnboardingStep.Taste -> if (selectedTasteCount >= 3) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = SetupOnAccent, modifier = Modifier.size(38.dp))
        } else {
            Text("$selectedTasteCount/3", color = SetupOnAccent, fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
        OnboardingStep.YoutubeMusic -> Icon(
            if (youtubeMusicConnected) Icons.Rounded.Check else Icons.Rounded.Link,
            contentDescription = null,
            tint = SetupOnAccent,
            modifier = Modifier.size(38.dp)
        )
    }
}

@Composable
private fun rememberSetupHeroShape(step: OnboardingStep, animated: Boolean): LevyraMorphShape {
    var from by remember { mutableStateOf(step) }
    var to by remember { mutableStateOf(step) }
    val progress = remember { Animatable(1f) }
    LaunchedEffect(step, animated) {
        if (step == to) return@LaunchedEffect
        from = to
        to = step
        progress.snapTo(0f)
        if (animated) {
            progress.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = 240f))
        } else {
            progress.snapTo(1f)
        }
    }
    val morph = remember(from, to) {
        Morph(onboardingHeroPolygon(from).normalized(), onboardingHeroPolygon(to).normalized())
    }
    return LevyraMorphShape(morph, progress.value)
}

@Composable
private fun SetupTitle(text: String, modifier: Modifier = Modifier, textAlign: TextAlign = TextAlign.Start) {
    Text(
        text = text,
        color = SetupInk,
        fontSize = 34.sp,
        lineHeight = LevyraTypeRhythm.lineHeight(34.sp),
        fontWeight = FontWeight.Black,
        letterSpacing = (-1.1).sp,
        textAlign = textAlign,
        modifier = modifier
            .fillMaxWidth()
            .semantics { heading() }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SetupIntroStage(strings: LevyraStrings, accent: Color) {
    val features = remember(strings) {
        listOf(
            SetupFeature(Icons.Rounded.GraphicEq, strings.introFeatureSound, MaterialShapes.Cookie4Sided, SetupFeatureTone.Peach),
            SetupFeature(Icons.Rounded.TextFields, strings.introFeatureLyrics, MaterialShapes.Clover4Leaf, SetupFeatureTone.Rose),
            SetupFeature(Icons.Rounded.Download, strings.introFeatureOffline, MaterialShapes.Sunny, SetupFeatureTone.Lavender)
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LevyraWordmark(fontSize = 24.sp)
        Spacer(modifier = Modifier.height(10.dp))
        SetupTitle(text = strings.introHeadline, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = strings.introBody,
            color = SetupMuted,
            fontSize = 16.sp,
            lineHeight = 23.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(22.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            features.forEachIndexed { index, feature ->
                SetupFeatureTile(
                    feature = feature,
                    accent = feature.tone.color,
                    shape = levyraGroupedListShape(index, features.size)
                )
            }
        }
    }
}

private enum class SetupFeatureTone {
    Peach,
    Rose,
    Lavender;

    val color: Color
        get() = when (this) {
            Peach -> SetupPeach
            Rose -> SetupRose
            Lavender -> SetupLavender
        }
}

private class SetupFeature(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val polygon: RoundedPolygon,
    val tone: SetupFeatureTone
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SetupFeatureTile(feature: SetupFeature, accent: Color, shape: Shape, modifier: Modifier = Modifier) {
    val badgeShape = feature.polygon.toShape()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(accent.copy(alpha = if (LevyraIsLight) 0.10f else 0.12f).compositeOver(SetupSurface), shape)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(accent, badgeShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(feature.icon, contentDescription = null, tint = SetupOnHero, modifier = Modifier.size(24.dp))
        }
        Text(
            text = feature.label,
            color = SetupInk,
            fontSize = 15.sp,
            lineHeight = LevyraTypeRhythm.lineHeight(15.sp),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SetupAppearanceStage(
    copy: OnboardingAppearanceCopy,
    light: Boolean,
    accent: Color,
    onLight: (Boolean) -> Unit
) {
    val darkPalette = remember { LevyraThemes.byId(LevyraThemes.LEVYRA_AURA) }
    val lightPalette = remember { LevyraThemes.byId(LevyraThemes.FROST) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        SetupTitle(text = copy.title)
        Spacer(modifier = Modifier.height(8.dp))
        Text(copy.body, color = SetupMuted, fontSize = 16.sp, lineHeight = 23.sp)
        Spacer(modifier = Modifier.height(22.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SetupAppearanceCard(
                label = copy.dark,
                body = copy.darkBody,
                icon = Icons.Rounded.DarkMode,
                badge = MaterialShapes.Cookie9Sided,
                palette = darkPalette,
                selected = !light,
                accent = accent,
                onClick = { onLight(false) },
                modifier = Modifier.weight(1f)
            )
            SetupAppearanceCard(
                label = copy.light,
                body = copy.lightBody,
                icon = Icons.Rounded.LightMode,
                badge = MaterialShapes.Sunny,
                palette = lightPalette,
                selected = light,
                accent = accent,
                onClick = { onLight(true) },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Palette, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Text(copy.footnote, color = SetupMuted, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SetupAppearanceCard(
    label: String,
    body: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    badge: RoundedPolygon,
    palette: LevyraPalette,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val style = rememberSetupAppearanceStyle(selected, accent)
    val shape = RoundedCornerShape(style.corner)
    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = style.scale
                scaleY = style.scale
            }
            .clip(shape)
            .background(style.container)
            .border(style.borderWidth, style.outline, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box {
            SetupThemeMockup(palette = palette, modifier = Modifier.fillMaxWidth())
            androidx.compose.animation.AnimatedVisibility(
                visible = selected,
                enter = scaleIn(spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium)) + fadeIn(tween(160)),
                exit = scaleOut(tween(140)) + fadeOut(tween(120)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(accent, MaterialShapes.Cookie9Sided.toShape()),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = SetupOnAccent, modifier = Modifier.size(18.dp))
                }
            }
        }
        SetupAppearanceLabel(label = label, icon = icon, badge = badge, selected = selected, accent = accent)
        Text(
            body,
            color = SetupMuted,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            minLines = 2,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 6.dp)
        )
    }
}

private class SetupAppearanceStyle(
    val corner: Dp,
    val scale: Float,
    val outline: Color,
    val container: Color,
    val borderWidth: Dp
)

@Composable
private fun rememberSetupAppearanceStyle(selected: Boolean, accent: Color): SetupAppearanceStyle {
    val animated = LocalAnimationsEnabled.current
    val shapeSpring = if (animated) spring<Dp>(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow) else snap()
    val scaleSpring = if (animated) spring<Float>(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium) else snap()
    val tint = if (animated) tween<Color>(260) else snap()
    val corner by animateDpAsState(if (selected) 34.dp else 22.dp, shapeSpring, label = "setup-appearance-corner")
    val scale by animateFloatAsState(if (selected) 1f else 0.95f, scaleSpring, label = "setup-appearance-scale")
    val outline by animateColorAsState(
        targetValue = if (selected) accent else SetupVeil.copy(alpha = 0.10f),
        animationSpec = tint,
        label = "setup-appearance-outline"
    )
    val container by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = 0.12f).compositeOver(SetupSurface) else SetupSurface,
        animationSpec = tint,
        label = "setup-appearance-container"
    )
    return SetupAppearanceStyle(corner, scale, outline, container, if (selected) 3.dp else 1.dp)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SetupAppearanceLabel(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    badge: RoundedPolygon,
    selected: Boolean,
    accent: Color
) {
    Row(
        modifier = Modifier.padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(if (selected) accent else SetupSurfaceHigh, badge.toShape()),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = if (selected) SetupOnAccent else SetupInk, modifier = Modifier.size(18.dp))
        }
        Text(label, color = SetupInk, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SetupThemeMockup(palette: LevyraPalette, modifier: Modifier = Modifier) {
    val screen = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .aspectRatio(0.62f)
            .clip(screen)
            .background(palette.black)
            .border(1.dp, palette.text.copy(alpha = 0.08f), screen)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .background(palette.cyan, MaterialShapes.Cookie4Sided.toShape())
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(6.dp)
                    .background(palette.text.copy(alpha = 0.78f), CircleShape)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(palette.cyan, palette.violet, palette.pink)))
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(22.dp)
                    .background(palette.black.copy(alpha = 0.92f), MaterialShapes.Cookie9Sided.toShape())
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .height(7.dp)
                .background(palette.text.copy(alpha = 0.86f), CircleShape)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.46f)
                .height(5.dp)
                .background(palette.muted.copy(alpha = 0.7f), CircleShape)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .background(palette.panel, CircleShape)
                .padding(horizontal = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(Brush.linearGradient(listOf(palette.pink, palette.blue)), RoundedCornerShape(5.dp))
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .background(palette.text.copy(alpha = 0.5f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(palette.cyan, CircleShape)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .background(palette.panelSoft, CircleShape)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Box(
                modifier = Modifier
                    .width(30.dp)
                    .height(16.dp)
                    .background(palette.cyan.copy(alpha = 0.32f), CircleShape)
            )
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(palette.muted.copy(alpha = 0.8f), CircleShape)
                )
            }
        }
    }
}

@Composable
private fun SetupLanguageStage(
    strings: LevyraStrings,
    languageCode: String,
    accent: Color,
    onLanguage: (String) -> Unit
) {
    val languages = LevyraLanguageCatalog.languages
    val selectedCode = LevyraLanguageCatalog.normalize(languageCode)
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = languages.indexOfFirst { it.code == selectedCode }.coerceAtLeast(0)
    )
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        item(key = "setup-language-header") {
            Column(modifier = Modifier.padding(bottom = 18.dp)) {
                SetupTitle(text = strings.welcomeTitle)
                Spacer(modifier = Modifier.height(8.dp))
                Text(strings.languageQuestion, color = SetupMuted, fontSize = 16.sp, lineHeight = 23.sp)
            }
        }
        itemsIndexed(languages, key = { _, language -> language.code }) { index, language ->
            SetupLanguageRow(
                language = language,
                selected = language.code == selectedCode,
                accent = accent,
                shape = levyraGroupedListShape(index, languages.size),
                onClick = { onLanguage(language.code) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val SetupFlagMorph: Morph by lazy {
    Morph(MaterialShapes.Circle.normalized(), MaterialShapes.Cookie4Sided.normalized())
}

@Composable
private fun SetupLanguageRow(
    language: LevyraLanguageOption,
    selected: Boolean,
    accent: Color,
    shape: androidx.compose.ui.graphics.Shape,
    onClick: () -> Unit
) {
    val animated = LocalAnimationsEnabled.current
    val container by animateColorAsState(
        targetValue = if (selected) accent else SetupSurface,
        animationSpec = if (animated) tween(220) else snap(),
        label = "setup-language-container"
    )
    val flagMorph by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = if (animated) spring(dampingRatio = 0.6f, stiffness = 380f) else snap(),
        label = "setup-language-flag"
    )
    val contentColor = if (selected) SetupOnAccent else SetupInk
    Surface(
        color = container,
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { this.selected = selected }
            .levyraPressable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (selected) SetupOnAccent.copy(alpha = 0.14f) else SetupVeil.copy(alpha = 0.06f),
                        LevyraMorphShape(SetupFlagMorph, flagMorph)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = language.flag, fontSize = 20.sp)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = language.nativeName,
                    color = contentColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!language.englishName.equals(language.nativeName, ignoreCase = true)) {
                    Text(
                        text = language.englishName,
                        color = if (selected) SetupOnAccent.copy(alpha = 0.66f) else SetupMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            AnimatedVisibility(
                visible = selected,
                enter = scaleIn(spring(dampingRatio = 0.5f, stiffness = 420f)) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = SetupOnAccent, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun SetupProfileStage(
    strings: LevyraStrings,
    name: String,
    accent: Color,
    onName: (String) -> Unit,
    onSubmit: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val animated = LocalAnimationsEnabled.current
    val fieldCorner by animateDpAsState(
        targetValue = if (focused) 18.dp else 32.dp,
        animationSpec = if (animated) spring(dampingRatio = 0.7f, stiffness = 380f) else snap(),
        label = "setup-name-corner"
    )
    val fieldBorder by animateColorAsState(
        targetValue = if (focused) accent else SetupVeil.copy(alpha = 0.08f),
        animationSpec = if (animated) tween(200) else snap(),
        label = "setup-name-border"
    )
    val fieldShape = RoundedCornerShape(fieldCorner)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        SetupTitle(text = strings.nameQuestion)
        BasicTextField(
            value = name,
            onValueChange = onName,
            singleLine = true,
            textStyle = TextStyle(color = SetupInk, fontSize = 22.sp, fontWeight = FontWeight.Bold),
            cursorBrush = SolidColor(accent),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { onSubmit() }),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
                .background(SetupSurface, fieldShape)
                .border(BorderStroke(1.5.dp, fieldBorder), fieldShape)
                .padding(horizontal = 22.dp, vertical = 20.dp),
            decorationBox = { innerTextField ->
                Box {
                    if (name.isEmpty()) {
                        Text(strings.namePlaceholder, color = SetupMuted, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun SetupTasteStage(
    strings: LevyraStrings,
    tastes: List<Taste>,
    artwork: Map<String, List<String>>,
    selected: Set<String>,
    accent: Color,
    onToggle: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 156.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "setup-taste-header", span = { GridItemSpan(maxLineSpan) }) {
            SetupTitle(text = strings.tasteQuestion, modifier = Modifier.padding(bottom = 10.dp))
        }
        gridItemsIndexed(tastes, key = { _, taste -> taste.id }) { index, taste ->
            SetupTasteTile(
                taste = taste,
                badgePolygon = setupTastePolygon(index),
                portraits = artwork[taste.id].orEmpty(),
                selected = taste.id in selected,
                accent = accent,
                onClick = { onToggle(taste.id) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val SetupTastePolygons: List<RoundedPolygon> by lazy {
    listOf(
        MaterialShapes.Cookie9Sided,
        MaterialShapes.Clover4Leaf,
        MaterialShapes.Sunny,
        MaterialShapes.Flower,
        MaterialShapes.Cookie6Sided,
        MaterialShapes.Gem,
        MaterialShapes.SoftBurst,
        MaterialShapes.Puffy,
        MaterialShapes.Cookie12Sided,
        MaterialShapes.Clover8Leaf,
        MaterialShapes.Pentagon,
        MaterialShapes.Cookie7Sided
    )
}

private fun setupTastePolygon(index: Int): RoundedPolygon =
    SetupTastePolygons[Math.floorMod(index, SetupTastePolygons.size)]

private fun setupTasteIcon(id: String): ImageVector = when (id) {
    "hits" -> Icons.Rounded.LocalFireDepartment
    "rap" -> Icons.Rounded.Mic
    "italiana" -> Icons.Rounded.Public
    "pop" -> Icons.Rounded.AutoAwesome
    "gym" -> Icons.Rounded.FitnessCenter
    "chill" -> Icons.Rounded.SelfImprovement
    "focus" -> Icons.Rounded.Headphones
    "sad" -> Icons.Rounded.Cloud
    "party" -> Icons.Rounded.Celebration
    "rock" -> Icons.Rounded.ElectricBolt
    "electro" -> Icons.Rounded.GraphicEq
    "rnb" -> Icons.Rounded.Nightlife
    else -> Icons.Rounded.MusicNote
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SetupTasteTile(
    taste: Taste,
    badgePolygon: RoundedPolygon,
    portraits: List<String>,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    val tint = remember(taste.id) { setupTastePalette(taste.id).first }
    val style = rememberSetupTasteStyle(selected, accent)
    val shape = RoundedCornerShape(style.corner)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .clip(shape)
            .background(style.fill)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(tint.copy(alpha = if (selected) 0.18f else 0.34f), Color.Transparent),
                        center = Offset(size.width, 0f),
                        radius = size.width * 0.9f
                    )
                )
            }
            .border(BorderStroke(1.5.dp, style.outline), shape)
            .semantics(mergeDescendants = true) { this.selected = selected }
            .levyraPressable(onClick = onClick)
    ) {
        SetupTasteArtwork(portraits = portraits, selected = selected, accent = accent)
        val onImage = portraits.isNotEmpty()
        Box(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            SetupTasteIconBadge(id = taste.id, polygon = badgePolygon, selected = selected, accent = accent, onImage = onImage)
            SetupTasteBadge(visible = selected, accent = accent, modifier = Modifier.align(Alignment.TopEnd))
            Text(
                text = taste.label,
                color = if (onImage) Color.White else SetupInk,
                fontSize = 16.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(16.sp),
                fontWeight = FontWeight.Bold,
                style = if (onImage) SetupTasteImageLabel else LocalTextStyle.current,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
            )
        }
    }
}

private class SetupTasteStyle(val corner: Dp, val fill: Color, val outline: Color)

@Composable
private fun rememberSetupTasteStyle(selected: Boolean, accent: Color): SetupTasteStyle {
    val animated = LocalAnimationsEnabled.current
    val corner by animateDpAsState(
        targetValue = if (selected) 14.dp else 26.dp,
        animationSpec = if (animated) spring(dampingRatio = 0.6f, stiffness = 380f) else snap(),
        label = "setup-taste-corner"
    )
    val fill by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = 0.16f) else SetupSurface,
        animationSpec = if (animated) tween(200) else snap(),
        label = "setup-taste-fill"
    )
    val outline by animateColorAsState(
        targetValue = if (selected) accent else SetupVeil.copy(alpha = 0.06f),
        animationSpec = if (animated) tween(200) else snap(),
        label = "setup-taste-outline"
    )
    return SetupTasteStyle(corner, fill, outline)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SetupTasteIconBadge(id: String, polygon: RoundedPolygon, selected: Boolean, accent: Color, onImage: Boolean) {
    val idle = if (onImage) Color.White.copy(alpha = 0.20f) else SetupVeil.copy(alpha = 0.08f)
    Box(
        modifier = Modifier
            .size(34.dp)
            .background(if (selected) accent else idle, polygon.toShape()),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            setupTasteIcon(id),
            contentDescription = null,
            tint = when {
                selected -> SetupOnAccent
                onImage -> Color.White
                else -> SetupInk
            },
            modifier = Modifier.size(18.dp)
        )
    }
}

private val SetupTasteImageLabel = TextStyle(
    shadow = Shadow(color = Color.Black.copy(alpha = 0.55f), offset = Offset(0f, 1f), blurRadius = 6f)
)

@Composable
private fun SetupTasteArtwork(portraits: List<String>, selected: Boolean, accent: Color) {
    val lead = portraits.firstOrNull() ?: return
    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = lead,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.15f),
                            Color.Black.copy(alpha = 0.45f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
                .then(if (selected) Modifier.background(accent.copy(alpha = 0.22f)) else Modifier)
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 40.dp),
            horizontalArrangement = Arrangement.spacedBy((-10).dp)
        ) {
            portraits.drop(1).take(2).forEach { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, SetupBase, CircleShape)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SetupTasteBadge(visible: Boolean, accent: Color, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(spring(dampingRatio = 0.45f, stiffness = 420f)) + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(accent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = SetupOnAccent, modifier = Modifier.size(14.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SetupYoutubeMusicStage(
    copy: OnboardingYoutubeMusicCopy,
    account: YoutubeMusicAccount?,
    error: String?,
    accent: Color,
    animated: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        SetupTitle(text = copy.title)
        Spacer(modifier = Modifier.height(10.dp))
        Text(copy.body, color = SetupMuted, fontSize = 16.sp, lineHeight = 23.sp)
        Spacer(modifier = Modifier.height(24.dp))
        SetupYoutubeMusicLink(accent = accent, connected = account != null, animated = animated)
        Spacer(modifier = Modifier.height(24.dp))
        AnimatedContent(
            targetState = account,
            transitionSpec = {
                fadeIn(tween(240, delayMillis = 60)) + scaleIn(spring(dampingRatio = 0.8f, stiffness = 380f), initialScale = 0.94f) togetherWith
                    fadeOut(tween(120))
            },
            contentKey = { it?.id },
            label = "setup-youtube-music-state"
        ) { connected ->
            if (connected != null) {
                SetupYoutubeMusicAccountCard(copy = copy, account = connected, accent = accent)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    SetupYoutubeMusicInfoRow(
                        icon = Icons.Rounded.Schedule,
                        polygon = MaterialShapes.Cookie4Sided,
                        title = copy.optionalTitle,
                        body = copy.optionalBody,
                        accent = accent,
                        shape = levyraGroupedListShape(0, 2)
                    )
                    SetupYoutubeMusicInfoRow(
                        icon = Icons.Rounded.Lock,
                        polygon = MaterialShapes.Clover4Leaf,
                        title = copy.secureTitle,
                        body = copy.secureBody,
                        accent = accent,
                        shape = levyraGroupedListShape(1, 2)
                    )
                }
            }
        }
        AnimatedVisibility(visible = error != null && account == null, enter = fadeIn(tween(200)), exit = fadeOut(tween(140))) {
            Text(
                text = error.orEmpty(),
                color = SetupError,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SetupYoutubeMusicLink(accent: Color, connected: Boolean, animated: Boolean) {
    val link by animateFloatAsState(
        targetValue = if (connected) 1f else 0f,
        animationSpec = if (animated) spring(dampingRatio = 0.75f, stiffness = 220f) else snap(),
        label = "setup-youtube-music-link"
    )
    val targetColor by animateColorAsState(
        targetValue = if (connected) accent else accent.copy(alpha = 0.18f),
        animationSpec = if (animated) tween(320) else snap(),
        label = "setup-youtube-music-target"
    )
    val pulse = if (animated && !connected) {
        rememberInfiniteTransition(label = "setup-youtube-music-pulse").animateFloat(
            initialValue = -1f,
            targetValue = SetupLinkDots + 1f,
            animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
            label = "setup-youtube-music-pulse-value"
        )
    } else {
        null
    }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .background(SetupSurfaceHigh, MaterialShapes.Cookie9Sided.toShape())
                .border(1.dp, SetupVeil.copy(alpha = 0.08f), MaterialShapes.Cookie9Sided.toShape()),
            contentAlignment = Alignment.Center
        ) {
            LevyraLogoMark(size = 42.dp, contentDescription = null)
        }
        SetupLinkConnector(accent = accent, link = { link }, pulse = { pulse?.value }, mirrored = rtl)
        Box(
            modifier = Modifier
                .size(76.dp)
                .background(targetColor, MaterialShapes.SoftBurst.toShape()),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (connected) Icons.Rounded.Check else Icons.Rounded.LibraryMusic,
                contentDescription = null,
                tint = if (connected) SetupOnAccent else accent,
                modifier = Modifier.size(34.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SetupYoutubeMusicInfoRow(
    icon: ImageVector,
    polygon: RoundedPolygon,
    title: String,
    body: String,
    accent: Color,
    shape: androidx.compose.ui.graphics.Shape
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SetupSurface, shape)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(accent.copy(alpha = 0.18f), polygon.toShape()),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                color = SetupInk,
                fontSize = 15.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(15.sp),
                fontWeight = FontWeight.Bold
            )
            Text(text = body, color = SetupMuted, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SetupYoutubeMusicAccountCard(copy: OnboardingYoutubeMusicCopy, account: YoutubeMusicAccount, accent: Color) {
    val avatarShape = MaterialShapes.Cookie9Sided.toShape()
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(accent.copy(alpha = 0.12f), RoundedCornerShape(28.dp))
                .border(1.dp, accent.copy(alpha = 0.32f), RoundedCornerShape(28.dp))
                .padding(16.dp)
                .semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(avatarShape)
                    .background(accent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = account.name.trim().take(1).uppercase(),
                    color = SetupOnAccent,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
                if (account.avatarUrl.isNotBlank()) AsyncImage(
                    model = account.avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(copy.connectedTitle, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = account.name,
                    color = SetupInk,
                    fontSize = 19.sp,
                    lineHeight = LevyraTypeRhythm.lineHeight(19.sp),
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (account.handle.startsWith("@")) Text(
                    text = account.handle,
                    color = SetupMuted,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(copy.connectedBody, color = SetupMuted, fontSize = 15.sp, lineHeight = 22.sp)
    }
}

@Composable
private fun SetupLinkConnector(accent: Color, link: () -> Float, pulse: () -> Float?, mirrored: Boolean) {
    Canvas(
        modifier = Modifier
            .width(84.dp)
            .height(20.dp)
            .graphicsLayer { scaleX = if (mirrored) -1f else 1f }
    ) {
        val progress = link()
        val phase = pulse()
        val y = size.height / 2f
        val step = size.width / SetupLinkDots
        repeat(SetupLinkDots) { index ->
            drawCircle(
                color = accent.copy(alpha = (0.22f + 0.7f * setupLinkGlow(phase, index)) * (1f - progress)),
                radius = 3.dp.toPx(),
                center = Offset(step * (index + 0.5f), y)
            )
        }
        if (progress > 0f) drawLine(
            color = accent,
            start = Offset(step * 0.5f, y),
            end = Offset(step * 0.5f + (size.width - step) * progress, y),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

private fun setupLinkGlow(phase: Float?, index: Int): Float =
    if (phase == null) 0.35f else (1f - kotlin.math.abs(phase - index)).coerceIn(0f, 1f)

private const val SetupLinkDots = 5

private fun setupTastePalette(id: String): Pair<Color, Color> {
    val palette = listOf(
        Color(0xFFE8115B) to Color(0xFF8C0B37),
        Color(0xFF1E3264) to Color(0xFF0D1A36),
        Color(0xFF8D67AB) to Color(0xFF4F3A61),
        Color(0xFF148A08) to Color(0xFF0A4D04),
        Color(0xFFE1118C) to Color(0xFF7D0A4E),
        Color(0xFF0D73EC) to Color(0xFF07407F),
        Color(0xFFBA5D07) to Color(0xFF6B3604),
        Color(0xFF477D95) to Color(0xFF274654),
        Color(0xFFDC148C) to Color(0xFF7A0B4E),
        Color(0xFF503750) to Color(0xFF2B1E2B)
    )
    return palette[(id.hashCode() and Int.MAX_VALUE) % palette.size]
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SetupFooter(
    hero: Boolean,
    label: String,
    accent: Color,
    enabled: Boolean,
    skipLabel: String?,
    onPrimary: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onPrimary,
            enabled = enabled,
            shapes = if (hero) ButtonDefaults.shapes(shape = CircleShape, pressedShape = CircleShape) else ButtonDefaults.shapes(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (hero) Color.Transparent else accent,
                contentColor = if (hero) SetupOnHero else SetupOnAccent,
                disabledContainerColor = SetupVeil.copy(alpha = 0.10f),
                disabledContentColor = SetupMuted
            ),
            contentPadding = PaddingValues(horizontal = 28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .then(if (hero && enabled) Modifier.background(SetupHeroGradient, CircleShape) else Modifier)
        ) {
            Text(text = label, fontSize = 17.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(22.dp))
        }
        Box(modifier = Modifier.height(48.dp), contentAlignment = Alignment.Center) {
            if (skipLabel != null) {
                TextButton(onClick = onSkip) {
                    Text(skipLabel, color = SetupMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
