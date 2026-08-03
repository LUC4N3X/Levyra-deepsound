package com.luc4n3x.levyra.ui.support

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.luc4n3x.levyra.data.LevyraPreferences
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraOrange
import com.luc4n3x.levyra.ui.theme.LevyraViolet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val PROMPT_DELAY_MS = 3_000L

@Composable
fun RemoteAnnouncementGate(
    enabled: Boolean,
    languageCode: String,
    hasPositiveListeningMoment: Boolean
) {
    val context = LocalContext.current.applicationContext
    val repository = remember(context) { RemoteAnnouncementRepository(context) }
    var launchCount by remember { mutableIntStateOf(0) }
    var announcement by remember { mutableStateOf<RemoteAnnouncementPresentation?>(null) }

    LaunchedEffect(Unit) {
        launchCount = withContext(Dispatchers.IO) { repository.recordAppLaunch() }
    }

    LaunchedEffect(enabled, languageCode, hasPositiveListeningMoment, launchCount) {
        if (!enabled) {
            announcement = null
            return@LaunchedEffect
        }
        val onboarded = withContext(Dispatchers.IO) { LevyraPreferences(context).isOnboarded() }
        if (!onboarded) return@LaunchedEffect
        delay(PROMPT_DELAY_MS)
        announcement = repository.resolveForPrompt(
            languageCode = languageCode,
            launchCount = launchCount,
            hasPositiveListeningMoment = hasPositiveListeningMoment
        )
    }

    val current = announcement ?: return
    val linkFailureMessage = remember(languageCode) {
        LevyraStrings.forCode(languageCode).cannotOpenExternalLink
    }
    val layoutDirection = if (LevyraLanguageCatalog.isRtl(languageCode)) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }
    val passiveDismiss = {
        repository.snooze(current.id, RemoteAnnouncementPromptPolicy.PASSIVE_SNOOZE_MS)
        announcement = null
    }
    val later = {
        repository.snooze(current.id, RemoteAnnouncementPromptPolicy.LATER_SNOOZE_MS)
        announcement = null
    }
    val openAction = {
        openRepositoryLink(
            context = context,
            target = current.actionUrl ?: LEVYRA_REPOSITORY_URL,
            failureMessage = linkFailureMessage,
            onSuccess = {
                repository.markCompleted(current.id)
                announcement = null
            }
        )
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        RemoteAnnouncementDialog(
            announcement = current,
            onAction = openAction,
            onLater = later,
            onPassiveDismiss = passiveDismiss
        )
    }
}

@Composable
fun SupportLevyraSettingsCard(
    languageCode: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext
    val repository = remember(context) { RemoteAnnouncementRepository(context) }
    val announcement by produceState<RemoteAnnouncementPresentation?>(
        initialValue = null,
        key1 = languageCode
    ) {
        value = withContext(Dispatchers.IO) {
            repository.bundledSupportPresentation(languageCode)
        }
    }
    val current = announcement ?: return
    val strings = remember(languageCode) { LevyraStrings.forCode(languageCode) }
    val shape = RoundedCornerShape(20.dp)
    val openAction = {
        openRepositoryLink(
            context = context,
            target = current.actionUrl ?: LEVYRA_REPOSITORY_URL,
            failureMessage = strings.cannotOpenExternalLink,
            onSuccess = { repository.markCompleted(current.id) }
        )
    }

    Surface(
        onClick = openAction,
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                LevyraCyan.copy(alpha = 0.95f),
                                LevyraViolet.copy(alpha = 0.92f),
                                LevyraOrange.copy(alpha = 0.88f)
                            )
                        ),
                        RoundedCornerShape(15.dp)
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(23.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = current.copy.settingsTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = current.copy.settingsSubtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun RemoteAnnouncementDialog(
    announcement: RemoteAnnouncementPresentation,
    onAction: () -> Unit,
    onLater: () -> Unit,
    onPassiveDismiss: () -> Unit
) {
    val shape = RoundedCornerShape(30.dp)
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val visuals = announcementVisuals(announcement.style)

    Dialog(
        onDismissRequest = onPassiveDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 440.dp)
                .heightIn(max = screenHeight * 0.88f)
                .shadow(30.dp, shape),
            shape = shape,
            color = Color.Transparent,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.99f),
                                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.98f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.99f)
                            )
                        ),
                        shape
                    )
                    .padding(horizontal = 24.dp, vertical = 26.dp)
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(visuals.brush, CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.30f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = visuals.heroIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), RoundedCornerShape(50))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                RoundedCornerShape(50)
                            )
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = visuals.badgeIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = announcement.copy.badge,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            letterSpacing = 0.8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = announcement.copy.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = announcement.copy.body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        textAlign = TextAlign.Center
                    )

                    if (announcement.actionUrl != null && announcement.copy.starAction.isNotBlank()) {
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = onAction,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = visuals.actionIcon,
                                contentDescription = null,
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(Modifier.width(9.dp))
                            Text(
                                text = announcement.copy.starAction,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.width(9.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    } else {
                        Spacer(Modifier.height(14.dp))
                    }

                    TextButton(
                        onClick = onLater,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = announcement.copy.laterAction,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

private fun openRepositoryLink(
    context: Context,
    target: String,
    failureMessage: String,
    onSuccess: () -> Unit
) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
        .onSuccess { onSuccess() }
        .onFailure { Toast.makeText(context, failureMessage, Toast.LENGTH_LONG).show() }
}

@Composable
private fun announcementVisuals(style: AnnouncementStyle): AnnouncementVisuals {
    return when (style) {
        AnnouncementStyle.OPEN_SOURCE -> AnnouncementVisuals(
            heroIcon = Icons.Rounded.Star,
            badgeIcon = Icons.Rounded.Code,
            actionIcon = Icons.Rounded.Star,
            brush = Brush.linearGradient(
                listOf(
                    LevyraCyan.copy(alpha = 0.95f),
                    LevyraViolet.copy(alpha = 0.95f),
                    LevyraOrange.copy(alpha = 0.90f)
                )
            )
        )
        AnnouncementStyle.UPDATE -> AnnouncementVisuals(
            heroIcon = Icons.Rounded.SystemUpdateAlt,
            badgeIcon = Icons.Rounded.SystemUpdateAlt,
            actionIcon = Icons.Rounded.SystemUpdateAlt,
            brush = Brush.linearGradient(
                listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.tertiary,
                    MaterialTheme.colorScheme.secondary
                )
            )
        )
        AnnouncementStyle.INFO -> AnnouncementVisuals(
            heroIcon = Icons.Rounded.Info,
            badgeIcon = Icons.Rounded.Info,
            actionIcon = Icons.Rounded.Info,
            brush = Brush.linearGradient(
                listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary,
                    MaterialTheme.colorScheme.primaryContainer
                )
            )
        )
    }
}

private data class AnnouncementVisuals(
    val heroIcon: ImageVector,
    val badgeIcon: ImageVector,
    val actionIcon: ImageVector,
    val brush: Brush
)
