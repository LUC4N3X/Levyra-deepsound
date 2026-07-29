package com.luc4n3x.levyra.ui.support

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Star
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.luc4n3x.levyra.data.LevyraPreferences
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraOrange
import com.luc4n3x.levyra.ui.theme.LevyraViolet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val REPOSITORY_URL = "https://github.com/LUC4N3X/Levyra-deepsound"
private const val PROMPT_DELAY_MS = 800L
private const val SUPPORT_PREFERENCES = "levyra_support_prompt"
private const val KEY_PROMPT_SEEN = "open_source_support_prompt_v1_seen"

private object OpenSourceSupportPromptStore {
    fun hasSeen(context: Context): Boolean = preferences(context).getBoolean(KEY_PROMPT_SEEN, false)

    fun markSeen(context: Context) {
        preferences(context).edit().putBoolean(KEY_PROMPT_SEEN, true).apply()
    }

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(SUPPORT_PREFERENCES, Context.MODE_PRIVATE)
}

@Composable
fun OpenSourceSupportPromptGate(
    enabled: Boolean,
    languageCode: String
) {
    val context = LocalContext.current.applicationContext
    var visible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(enabled, languageCode) {
        visible = false
        if (!enabled) return@LaunchedEffect
        val eligible = withContext(Dispatchers.IO) {
            LevyraPreferences(context).isOnboarded() && !OpenSourceSupportPromptStore.hasSeen(context)
        }
        if (!eligible) return@LaunchedEffect
        delay(PROMPT_DELAY_MS)
        visible = withContext(Dispatchers.IO) { !OpenSourceSupportPromptStore.hasSeen(context) }
    }

    if (!visible) return

    val copy = remember(languageCode) { OpenSourceSupportStrings.forCode(languageCode) }
    val layoutDirection = if (LevyraLanguageCatalog.isRtl(languageCode)) LayoutDirection.Rtl else LayoutDirection.Ltr
    val dismiss = {
        OpenSourceSupportPromptStore.markSeen(context)
        visible = false
    }
    val openRepository = {
        dismiss()
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(REPOSITORY_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        OpenSourceSupportPrompt(
            copy = copy,
            onStar = openRepository,
            onDismiss = dismiss
        )
    }
}

@Composable
private fun OpenSourceSupportPrompt(
    copy: OpenSourceSupportCopy,
    onStar: () -> Unit,
    onDismiss: () -> Unit
) {
    val shape = RoundedCornerShape(30.dp)
    val accentBrush = Brush.linearGradient(
        listOf(
            LevyraCyan.copy(alpha = 0.95f),
            LevyraViolet.copy(alpha = 0.95f),
            LevyraOrange.copy(alpha = 0.90f)
        )
    )

    Dialog(
        onDismissRequest = onDismiss,
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
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(accentBrush, CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.30f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
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
                            imageVector = Icons.Rounded.Code,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = copy.badge,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            letterSpacing = 0.8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = copy.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = copy.body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = onStar,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(Modifier.width(9.dp))
                        Text(
                            text = copy.starAction,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Icon(
                            imageVector = Icons.Rounded.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = copy.continueAction,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
