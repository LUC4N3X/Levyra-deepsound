package com.luc4n3x.levyra.ui.update

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.R
import com.luc4n3x.levyra.data.LevyraPreferences
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.starThanksCopy
import com.luc4n3x.levyra.ui.support.LEVYRA_REPOSITORY_URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val STAR_SHEET_DELAY_MS = 500L

internal fun shouldShowStarSheet(firstInstallTime: Long, lastUpdateTime: Long, alreadyShown: Boolean): Boolean =
    !alreadyShown && lastUpdateTime > firstInstallTime

@Composable
internal fun LevyraStarThanksGate(enabled: Boolean, languageCode: String) {
    val context = LocalContext.current.applicationContext
    val preferences = remember(context) { LevyraPreferences(context) }
    var visible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(enabled) {
        if (!enabled || visible) return@LaunchedEffect
        val eligible = withContext(Dispatchers.IO) {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            preferences.isOnboarded() &&
                shouldShowStarSheet(info.firstInstallTime, info.lastUpdateTime, preferences.starThanksShown())
        }
        if (!eligible) return@LaunchedEffect
        delay(STAR_SHEET_DELAY_MS)
        visible = true
    }

    if (!visible || !enabled) return
    val strings = remember(languageCode) { LevyraStrings.forCode(languageCode) }
    val layoutDirection = if (LevyraLanguageCatalog.isRtl(languageCode)) LayoutDirection.Rtl else LayoutDirection.Ltr
    val dismiss = {
        preferences.setStarThanksShown()
        visible = false
    }
    CompositionLocalProvider(
        LocalLevyraStrings provides strings,
        LocalLayoutDirection provides layoutDirection
    ) {
        LevyraStarThanksSheet(
            onStar = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(LEVYRA_REPOSITORY_URL))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(intent) }
                    .onFailure { Toast.makeText(context, strings.cannotOpenExternalLink, Toast.LENGTH_LONG).show() }
                dismiss()
            },
            onDismiss = dismiss
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LevyraStarThanksSheet(onStar: () -> Unit, onDismiss: () -> Unit) {
    val copy = starThanksCopy(LocalLevyraStrings.current.code)
    val colors = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val closeThen: (() -> Unit) -> Unit = { action ->
        scope.launch { sheetState.hide() }.invokeOnCompletion { action() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surfaceContainerLow,
        contentColor = colors.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(40.dp))
                Box(
                    modifier = Modifier
                        .size(184.dp)
                        .background(
                            Brush.radialGradient(listOf(colors.primary.copy(alpha = 0.18f), Color.Transparent)),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(128.dp)
                            .clip(MaterialShapes.Cookie9Sided.toShape())
                            .background(colors.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.levyra_logo),
                            contentDescription = copy.logoDescription,
                            modifier = Modifier.size(88.dp)
                        )
                    }
                    Surface(
                        shape = MaterialShapes.Sunny.toShape(),
                        color = colors.tertiary,
                        contentColor = colors.onTertiary,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 22.dp, bottom = 22.dp)
                            .size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Star, contentDescription = null, modifier = Modifier.size(22.dp))
                        }
                    }
                }
                Spacer(Modifier.height(28.dp))
                Text(
                    text = copy.title,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { heading() }
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = copy.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { closeThen(onStar) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                ) {
                    Icon(Icons.Rounded.Star, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(copy.star, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                }
                FilledTonalButton(
                    onClick = { closeThen(onDismiss) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                ) {
                    Text(copy.done, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
