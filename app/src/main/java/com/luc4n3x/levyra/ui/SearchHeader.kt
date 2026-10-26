@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.ui.theme.*
import com.luc4n3x.levyra.ui.components.LevyraBackButton
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import com.luc4n3x.levyra.ui.components.levyraExpressiveToggleCorner
import com.luc4n3x.levyra.ui.theme.LevyraMotion
import com.luc4n3x.levyra.ui.player.*
import androidx.compose.animation.core.snap
import android.app.Activity
import android.content.Intent
import android.widget.Toast
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.ui.draw.alpha
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.luc4n3x.levyra.ui.theme.LevyraIsNocturne
import com.luc4n3x.levyra.ui.theme.Nocturne78Design
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraActivePalette
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SearchHeader(
    query: String,
    placeholder: String,
    animatePlaceholder: Boolean,
    isSearching: Boolean,
    recognitionAvailable: Boolean,
    recognitionBusy: Boolean,
    onRecognize: () -> Unit,
    onCancelRecognition: () -> Unit,
    onQuery: (String) -> Unit,
    onSearch: (String) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    userName: String = "",
    onSettings: () -> Unit = {},
    spotifyFocus: MutableState<Boolean>? = null
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val strings = LocalLevyraStrings.current

    val (launchVoiceSearch, toggleRecognition) = rememberSearchVoiceActions(recognitionBusy, onRecognize, onCancelRecognition, onQuery, onSearch)
    if (LevyraIsYouTubeMusic) {
        YouTubeMusicSearchHeader(
            query = query,
            placeholder = placeholder,
            isSearching = isSearching,
            recognitionAvailable = recognitionAvailable,
            recognitionBusy = recognitionBusy,
            onQuery = onQuery,
            onSearch = { text ->
                focusManager.clearFocus()
                keyboardController?.hide()
                onSearch(text)
            },
            onClear = onClear,
            onBack = {
                if (query.isBlank()) onBack() else onClear()
                focusManager.clearFocus()
                keyboardController?.hide()
            },
            onVoice = launchVoiceSearch,
            onRecognize = toggleRecognition
        )
        return
    }
    if (LevyraIsAppleMusic) {
        AppleMusicSearchHeader(
            query, recognitionAvailable, recognitionBusy, onQuery, onSearch, onClear,
            onSettings, launchVoiceSearch, toggleRecognition, spotifyFocus
        )
        return
    }
    if (LevyraIsSpotify) {
        SpotifySearchHeader(
            userName, query, recognitionAvailable, recognitionBusy, onSettings, onQuery,
            onSearch, onClear, launchVoiceSearch, toggleRecognition, spotifyFocus
        )
        return
    }

    DefaultSearchHeader(query, placeholder, animatePlaceholder, isSearching, recognitionAvailable, recognitionBusy,
        onQuery, onClear, onBack, launchVoiceSearch, toggleRecognition)
}

private data class SearchVoiceActions(val voice: () -> Unit, val recognize: () -> Unit)

@Composable
private fun rememberSearchVoiceActions(
    recognitionBusy: Boolean, onRecognize: () -> Unit, onCancelRecognition: () -> Unit,
    onQuery: (String) -> Unit, onSearch: (String) -> Unit
): SearchVoiceActions {
    val context = LocalContext.current
    val strings = LocalLevyraStrings.current
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                val spokenText = matches[0]
                onQuery(spokenText)
                onSearch(spokenText)
            }
        }
    }
    val microphonePermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onRecognize()
    }
    val launchVoiceSearch = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.forLanguageTag(strings.code).toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.forLanguageTag(strings.code).toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PROMPT, strings.listeningPrompt)
        }
        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, strings.voiceSearchUnsupported, Toast.LENGTH_SHORT).show()
        }
    }
    val toggleRecognition = {
        if (recognitionBusy) {
            onCancelRecognition()
        } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            onRecognize()
        } else {
            microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    return SearchVoiceActions(launchVoiceSearch, toggleRecognition)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DefaultSearchHeader(
    query: String, placeholder: String, animatePlaceholder: Boolean, isSearching: Boolean,
    recognitionAvailable: Boolean, recognitionBusy: Boolean, onQuery: (String) -> Unit,
    onClear: () -> Unit, onBack: () -> Unit, launchVoiceSearch: () -> Unit, toggleRecognition: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val strings = LocalLevyraStrings.current
    val animationsEnabled = LocalAnimationsEnabled.current
    val fieldInteraction = remember { MutableInteractionSource() }
    val fieldFocused by fieldInteraction.collectIsFocusedAsState()
    val scheme = MaterialTheme.colorScheme
    val (fieldContainer, searchBadgeColor, focusRing, searchIconTint, fieldCorner) = rememberSearchHeaderAppearance(fieldFocused, query, animationsEnabled, scheme)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        LevyraBackButton(
            onClick = {
                if (query.isBlank()) onBack() else onClear()
                focusManager.clearFocus()
                keyboardController?.hide()
            },
            contentDescription = strings.back
        )

        Surface(
            color = fieldContainer,
            border = if (LevyraIsSpotify) null else BorderStroke(if (LevyraIsPrism) 1.5.dp else 2.dp, focusRing),
            shape = RoundedCornerShape(fieldCorner),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 60.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .background(searchBadgeColor, MaterialShapes.Cookie4Sided.toShape())
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = searchIconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }

                SearchHeaderTextField(query, placeholder, animatePlaceholder, fieldInteraction, onQuery, Modifier.weight(1f).padding(start = 4.dp))

                SearchHeaderActionButtons(query, isSearching, fieldFocused, recognitionAvailable, recognitionBusy, onClear, launchVoiceSearch, toggleRecognition)
            }
        }
    }
}


internal fun searchFieldContainer(focused: Boolean, scheme: androidx.compose.material3.ColorScheme): Color = when {
    LevyraIsTorque -> Color(0xFFF1F1F1)
    LevyraIsSpotify -> SpotifyDesign.searchField
    LevyraIsYouTubeMusic -> YouTubeMusicDesign.surfaceHigh
    LevyraIsAppleMusic -> AppleMusicDesign.fill
    LevyraIsIvory -> LevyraActivePalette.ink
    LevyraIsPrism -> if (focused) scheme.surfaceContainerHigh else scheme.surfaceContainer
    else -> if (focused) scheme.surfaceContainerHighest else scheme.surfaceContainerHigh
}

internal fun searchFieldIconTint(active: Boolean, scheme: androidx.compose.material3.ColorScheme): Color = when {
    LevyraIsTorque -> Color(0xFF111111)
    LevyraIsSpotify -> SpotifyDesign.searchFieldText
    LevyraIsYouTubeMusic -> YouTubeMusicDesign.textSecondary
    LevyraIsAppleMusic -> AppleMusicDesign.secondaryLabel
    LevyraIsIvory -> LevyraText
    LevyraIsPrism -> if (active) scheme.primary else scheme.onSurfaceVariant
    else -> if (active) scheme.onPrimaryContainer else scheme.onSurfaceVariant
}

internal fun searchFieldCorner(fallback: Dp): Dp = when {
    LevyraIsNocturne -> Nocturne78Design.cardCorner
    LevyraIsSpotify -> SpotifyDesign.corner
    LevyraIsYouTubeMusic -> 20.dp
    LevyraIsAppleMusic -> AppleMusicDesign.cornerL
    LevyraIsPrism -> PrismDesign.controlCorner
    else -> fallback
}

internal fun searchFocusRing(scheme: androidx.compose.material3.ColorScheme, focusRingAlpha: Float): Brush =
    if (LevyraIsPrism) SolidColor(scheme.primary.copy(alpha = 0.7f * focusRingAlpha)) else Brush.linearGradient(
        listOf(
            scheme.primary.copy(alpha = 0.9f * focusRingAlpha),
            scheme.tertiary.copy(alpha = 0.75f * focusRingAlpha),
            scheme.primary.copy(alpha = 0.35f * focusRingAlpha)
        )
    )

private data class SearchHeaderAppearance(val container: Color, val badge: Color, val ring: Brush, val icon: Color, val corner: Dp)

@Composable
private fun rememberSearchHeaderAppearance(fieldFocused: Boolean, query: String, animationsEnabled: Boolean, scheme: androidx.compose.material3.ColorScheme): SearchHeaderAppearance {
    val fieldContainer by animateColorAsState(
        targetValue = searchFieldContainer(fieldFocused, scheme),
        animationSpec = if (animationsEnabled) LevyraMotion.standard() else snap(),
        label = "search-field-container"
    )
    val focusRingAlpha by animateFloatAsState(
        targetValue = if (fieldFocused) 1f else 0f,
        animationSpec = if (animationsEnabled) LevyraMotion.standard() else snap(),
        label = "search-field-ring"
    )
    val searchBadgeColor by animateColorAsState(
        targetValue = if ((fieldFocused || query.isNotEmpty()) && !LevyraIsPrism) scheme.primaryContainer else Color.Transparent,
        animationSpec = if (animationsEnabled) LevyraMotion.standard() else snap(),
        label = "search-field-badge"
    )
    val focusRing = remember(scheme.primary, scheme.tertiary, focusRingAlpha) {
        searchFocusRing(scheme, focusRingAlpha)
    }
    val searchIconTint by animateColorAsState(
        targetValue = searchFieldIconTint(fieldFocused || query.isNotEmpty(), scheme),
        animationSpec = if (animationsEnabled) LevyraMotion.standard() else snap(),
        label = "search-field-icon"
    )
    val fieldCorner = levyraExpressiveToggleCorner(
        checked = fieldFocused,
        unchecked = searchFieldCorner(28.dp),
        checkedCorner = searchFieldCorner(20.dp),
        label = "search-field-corner"
    )

    return SearchHeaderAppearance(fieldContainer, searchBadgeColor, focusRing, searchIconTint, fieldCorner)
}

@Composable
private fun SearchHeaderTextField(query: String, placeholder: String, animatePlaceholder: Boolean, fieldInteraction: MutableInteractionSource, onQuery: (String) -> Unit, modifier: Modifier) {
    BasicTextField(
        value = query,
        onValueChange = onQuery,
        modifier = modifier,
        interactionSource = fieldInteraction,
        singleLine = true,
        textStyle = TextStyle(
            color = if (LevyraIsTorque) Color(0xFF111111) else if (LevyraIsSpotify) SpotifyDesign.searchFieldText else LevyraText,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            letterSpacing = (-0.1).sp
        ),
        cursorBrush = SolidColor(if (LevyraIsTorque) LevyraActivePalette.blue else if (LevyraIsSpotify) SpotifyDesign.greenPress else LevyraCyan),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Crossfade(
                        targetState = placeholder,
                        animationSpec = if (animatePlaceholder) tween(360) else snap(),
                        label = "search-taste-placeholder"
                    ) { text ->
                        BasicText(
                            text = text,
                            style = TextStyle(
                                color = if (LevyraIsTorque || LevyraIsSpotify) Color(0xFF5A5A5A) else LevyraMuted.copy(alpha = 0.82f),
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 15.sp)
                        )
                    }
                }
                innerTextField()
            }
        }
    )
}

@Composable
private fun SearchHeaderActionButtons(query: String, isSearching: Boolean, fieldFocused: Boolean, recognitionAvailable: Boolean, recognitionBusy: Boolean, onClear: () -> Unit, launchVoiceSearch: () -> Unit, toggleRecognition: () -> Unit) {
    val strings = LocalLevyraStrings.current
    val scheme = MaterialTheme.colorScheme
    SearchHeaderClearAction(query, isSearching, onClear)

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(scheme.surfaceContainerHighest.copy(alpha = if (fieldFocused) 0.9f else 0.6f)),
        verticalAlignment = Alignment.CenterVertically
    ) {
    IconButton(
        onClick = launchVoiceSearch,
        modifier = Modifier.size(38.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Mic,
            contentDescription = strings.voice,
            tint = scheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }

    if (recognitionAvailable) SearchHeaderRecognitionButton(recognitionBusy, toggleRecognition)
    }
}

@Composable
private fun SearchHeaderClearAction(query: String, isSearching: Boolean, onClear: () -> Unit) {
    val animationsEnabled = LocalAnimationsEnabled.current
    val strings = LocalLevyraStrings.current
    if (isSearching) {
        LevyraLoadingIndicator(
            modifier = Modifier.size(24.dp),
            color = LevyraCyan
        )
    } else {
        AnimatedVisibility(
            visible = query.isNotEmpty(),
            enter = if (animationsEnabled) fadeIn() + scaleIn(initialScale = 0.7f) else fadeIn(snap()),
            exit = if (animationsEnabled) fadeOut() + scaleOut(targetScale = 0.7f) else fadeOut(snap())
        ) {
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = strings.clear,
                    tint = LevyraMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SearchHeaderRecognitionButton(recognitionBusy: Boolean, toggleRecognition: () -> Unit) {
    val animationsEnabled = LocalAnimationsEnabled.current
    val strings = LocalLevyraStrings.current
    val scheme = MaterialTheme.colorScheme
    val recognitionRotation = if (recognitionBusy && animationsEnabled) {
        val spin = rememberInfiniteTransition(label = "search-recognition-spin")
        spin.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(2_400, easing = LinearEasing)),
            label = "search-recognition-rotation"
        ).value
    } else {
        0f
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(4.dp)
            .size(40.dp)
            .graphicsLayer { rotationZ = recognitionRotation }
            .clip(MaterialShapes.Cookie9Sided.toShape())
            .background(if (recognitionBusy) scheme.primary else scheme.primaryContainer)
            .clickable(onClickLabel = strings.recognizeMusic, role = Role.Button, onClick = toggleRecognition)
    ) {
        Icon(
            imageVector = Icons.Rounded.GraphicEq,
            contentDescription = strings.recognizeMusic,
            tint = if (recognitionBusy) scheme.onPrimary else scheme.onPrimaryContainer,
            modifier = Modifier
                .size(19.dp)
                .graphicsLayer { rotationZ = -recognitionRotation }
        )
    }
}
