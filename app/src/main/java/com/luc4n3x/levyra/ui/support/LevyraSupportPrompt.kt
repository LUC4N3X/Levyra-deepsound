package com.luc4n3x.levyra.ui.support

import android.animation.ValueAnimator
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.R
import com.luc4n3x.levyra.data.LevyraPreferences
import com.luc4n3x.levyra.data.SupportPromptSchedule
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.StarThanksCopy
import com.luc4n3x.levyra.ui.i18n.starThanksCopy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val SUPPORT_PROMPT_DELAY_MS = 1_200L

@Composable
internal fun LevyraSupportPromptGate(
    enabled: Boolean,
    languageCode: String,
    onDecided: (Boolean) -> Unit,
    onClosed: () -> Unit
) {
    val context = LocalContext.current.applicationContext
    val preferences = remember(context) { LevyraPreferences(context) }
    var due by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var visible by rememberSaveable { mutableStateOf(false) }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle, enabled) {
        if (!enabled) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val decided = withContext(Dispatchers.IO) {
                if (!preferences.isOnboarded()) return@withContext false
                val next = SupportPromptSchedule.recordOpen(
                    state = preferences.supportPromptState(),
                    versionCode = BuildConfig.VERSION_CODE,
                    nowMs = System.currentTimeMillis()
                ) ?: return@withContext false
                preferences.setSupportPromptState(next)
                SupportPromptSchedule.isDue(next)
            }
            if (decided) due = true
            onDecided(decided || due == true)
        }
    }

    LaunchedEffect(enabled, due) {
        if (!enabled || due != true || visible) return@LaunchedEffect
        delay(SUPPORT_PROMPT_DELAY_MS)
        visible = true
    }

    if (!visible || !enabled) return
    val strings = remember(languageCode) { LevyraStrings.forCode(languageCode) }
    val copy = remember(strings.code) { starThanksCopy(strings.code) }
    val layoutDirection = if (LevyraLanguageCatalog.isRtl(languageCode)) LayoutDirection.Rtl else LayoutDirection.Ltr
    val close = {
        due = false
        visible = false
        onClosed()
    }
    val finish = {
        preferences.finishSupportPrompt()
        close()
    }
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        LevyraSupportPromptDialog(
            copy = copy,
            onStar = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(LEVYRA_REPOSITORY_URL))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(intent) }
                    .onSuccess { finish() }
                    .onFailure { Toast.makeText(context, strings.cannotOpenExternalLink, Toast.LENGTH_LONG).show() }
            },
            onAlreadyDone = finish,
            onLater = close
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LevyraSupportPromptDialog(
    copy: StarThanksCopy,
    onStar: () -> Unit,
    onAlreadyDone: () -> Unit,
    onLater: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val animate = remember { ValueAnimator.areAnimatorsEnabled() }
    val heroScale = remember { Animatable(if (animate) 0.8f else 1f) }
    val badgeScale = remember { Animatable(if (animate) 0f else 1f) }
    val heroSpec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    val badgeSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    val shapeTurn = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (!animate) return@LaunchedEffect
        launch { heroScale.animateTo(1f, heroSpec) }
        launch {
            shapeTurn.animateTo(
                targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(durationMillis = 24_000, easing = LinearEasing))
            )
        }
        delay(220L)
        badgeScale.animateTo(1f, badgeSpec)
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .widthIn(max = 380.dp)
                .heightIn(max = screenHeight * 0.88f),
            shape = MaterialTheme.shapes.extraLarge,
            color = colors.surfaceContainerHigh,
            contentColor = colors.onSurface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            0f to colors.primaryContainer.copy(alpha = 0.55f),
                            0.45f to colors.surfaceContainerHigh
                        )
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(184.dp)
                        .background(
                            Brush.radialGradient(listOf(colors.primary.copy(alpha = 0.28f), Color.Transparent)),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(148.dp)
                            .graphicsLayer {
                                scaleX = heroScale.value
                                scaleY = heroScale.value
                                rotationZ = shapeTurn.value
                            }
                            .clip(MaterialShapes.Cookie12Sided.toShape())
                            .background(
                                Brush.linearGradient(listOf(colors.primaryContainer, colors.tertiaryContainer))
                            )
                    )
                    Image(
                        painter = painterResource(R.drawable.levyra_logo),
                        contentDescription = copy.logoDescription,
                        modifier = Modifier
                            .size(132.dp)
                            .graphicsLayer {
                                scaleX = heroScale.value
                                scaleY = heroScale.value
                            }
                    )
                    Surface(
                        shape = MaterialShapes.Sunny.toShape(),
                        color = colors.tertiary,
                        contentColor = colors.onTertiary,
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 12.dp, bottom = 12.dp)
                            .size(48.dp)
                            .graphicsLayer {
                                scaleX = badgeScale.value
                                scaleY = badgeScale.value
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Star, contentDescription = null, modifier = Modifier.size(24.dp))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = copy.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { heading() }
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = copy.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onStar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Rounded.Star, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(copy.star, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(4.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
                ) {
                    TextButton(onClick = onLater, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(copy.later, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                    TextButton(onClick = onAlreadyDone, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(copy.done, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
