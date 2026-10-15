package com.luc4n3x.levyra.widget.studio

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.luc4n3x.levyra.R
import com.luc4n3x.levyra.data.LevyraPreferences
import com.luc4n3x.levyra.ui.components.LevyraBackButton
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.WidgetStudioCopy
import com.luc4n3x.levyra.ui.i18n.WidgetStudioText
import com.luc4n3x.levyra.ui.i18n.widgetStudioCopy
import com.luc4n3x.levyra.ui.theme.LevyraInk
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraTheme
import com.luc4n3x.levyra.widget.LevyraWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WidgetStudioActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.enableEdgeToEdge(window)
        val configureRequest = intent?.action == AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
        val requestedId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        val requestedFamily = if (configureRequest && requestedId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            WidgetStudioEngine.familyOf(this, requestedId)
        } else {
            null
        }
        if (configureRequest) {
            setResult(Activity.RESULT_CANCELED, resultIntent(requestedId))
            if (requestedFamily == null) {
                finish()
                return
            }
        }
        val initialTarget = requestedFamily?.let { requestedId to it }
        setContent {
            val strings by produceState<LevyraStrings?>(initialValue = null) {
                value = withContext(Dispatchers.IO) {
                    LevyraStrings.forCode(LevyraPreferences(applicationContext).languageCode())
                }
            }
            val resolved = strings ?: return@setContent
            LevyraTheme {
                CompositionLocalProvider(LocalLevyraStrings provides resolved) {
                    WidgetStudioScreen(
                        copy = resolved.widgetStudioCopy,
                        initialTarget = initialTarget,
                        configureMode = configureRequest,
                        onExit = { finish() },
                        onDone = {
                            setResult(Activity.RESULT_OK, resultIntent(requestedId))
                            finish()
                        }
                    )
                }
            }
        }
    }

    private fun resultIntent(widgetId: Int) = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)

    companion object {
        fun intent(context: Context): Intent = Intent(context, WidgetStudioActivity::class.java)
    }
}

private val StudioCardShape = RoundedCornerShape(28.dp)
private val StudioStageShape = RoundedCornerShape(32.dp)
private const val THUMBNAIL_PIXEL_SCALE = 0.5f

@Composable
private fun WidgetStudioScreen(
    copy: WidgetStudioCopy,
    initialTarget: Pair<Int, StudioWidgetFamily>?,
    configureMode: Boolean,
    onExit: () -> Unit,
    onDone: () -> Unit
) {
    var target by remember { mutableStateOf(initialTarget) }
    val current = target
    BackHandler(enabled = current != null && !configureMode) { target = null }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LevyraInk)
            .statusBarsPadding()
    ) {
        if (current == null) {
            WidgetStudioOverview(copy = copy, onEdit = { target = it }, onExit = onExit)
        } else {
            WidgetStudioEditor(
                copy = copy,
                widgetId = current.first,
                family = current.second,
                configureMode = configureMode,
                onBack = { if (configureMode) onExit() else target = null },
                onDone = onDone
            )
        }
    }
}

@Composable
private fun StudioHeader(
    title: String,
    subtitle: String,
    backLabel: String,
    onBack: () -> Unit,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LevyraBackButton(onClick = onBack, contentDescription = backLabel)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                color = LevyraText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = LevyraMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        trailing()
    }
}

@Composable
private fun StudioSectionHeader(title: String, hint: String?) {
    Column(modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 12.dp, bottom = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = LevyraText,
            modifier = Modifier.semantics { heading() }
        )
        if (hint != null) {
            Text(text = hint, style = MaterialTheme.typography.bodySmall, color = LevyraMuted)
        }
    }
}

@Composable
private fun StudioGroup(title: String, hint: String?, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(StudioCardShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = LevyraText,
                modifier = Modifier.semantics { heading() }
            )
            if (hint != null) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = LevyraMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        content()
    }
}

@Composable
private fun WidgetStudioOverview(
    copy: WidgetStudioCopy,
    onEdit: (Pair<Int, StudioWidgetFamily>) -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        refresh++
        onPauseOrDispose { }
    }
    val placed by produceState(initialValue = emptyList<Pair<Int, StudioWidgetFamily>>(), refresh) {
        value = withContext(Dispatchers.IO) { WidgetStudioEngine.placedWidgets(context) }
    }
    val hasOriginal by produceState(initialValue = false, refresh) {
        value = withContext(Dispatchers.IO) { hasOriginalWidget(context) }
    }
    val pinSupported = remember { AppWidgetManager.getInstance(context)?.isRequestPinAppWidgetSupported == true }
    val demos by produceState(initialValue = emptyList<StudioPlaybackState>()) {
        value = withContext(Dispatchers.IO) { WidgetStudioDemo.states(context, StudioWidgetFamily.entries.size) }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        StudioHeader(copy[WidgetStudioText.Title], copy[WidgetStudioText.Intro], copy[WidgetStudioText.Back], onExit)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "placed-header", contentType = "header") {
                StudioSectionHeader(copy[WidgetStudioText.YourWidgets], copy[WidgetStudioText.YourWidgetsHint])
            }
            item(key = "placed-row", contentType = "carousel") {
                PlacedWidgets(copy = copy, placed = placed, hasOriginal = hasOriginal, onEdit = onEdit)
            }
            item(key = "progress-refresh", contentType = "group") {
                ProgressRefreshGroup(copy)
            }
            item(key = "add-header", contentType = "header") {
                StudioSectionHeader(copy[WidgetStudioText.AddWidgets], copy[WidgetStudioText.AddWidgetsHint])
            }
            if (!pinSupported) {
                item(key = "add-unsupported", contentType = "info") {
                    Text(copy[WidgetStudioText.AddUnsupported], style = MaterialTheme.typography.bodyMedium, color = LevyraMuted)
                }
            }
            items(StudioWidgetFamily.entries.chunked(2), key = { row -> row.first().key }, contentType = { "gallery" }) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { family ->
                        GalleryCard(
                            copy = copy,
                            family = family,
                            pinSupported = pinSupported,
                            demo = demos.getOrNull(family.ordinal % demos.size.coerceAtLeast(1)),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProgressRefreshGroup(copy: WidgetStudioCopy) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(WidgetStudioStore.progressRefresh(context)) }
    val options = StudioProgressRefresh.entries
    StudioGroup(copy[WidgetStudioText.ProgressRefresh], copy[WidgetStudioText.ProgressRefreshHint]) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = {
                        selected = option
                        WidgetStudioStore.setProgressRefresh(context, option)
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(
                        copy[
                            when (option) {
                                StudioProgressRefresh.Saver -> WidgetStudioText.RefreshSaver
                                StudioProgressRefresh.Balanced -> WidgetStudioText.RefreshBalanced
                                StudioProgressRefresh.Smooth -> WidgetStudioText.RefreshSmooth
                            }
                        ],
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun PlacedWidgets(
    copy: WidgetStudioCopy,
    placed: List<Pair<Int, StudioWidgetFamily>>,
    hasOriginal: Boolean,
    onEdit: (Pair<Int, StudioWidgetFamily>) -> Unit
) {
    if (placed.isEmpty() && !hasOriginal) {
        Text(
            text = copy[WidgetStudioText.NoWidgets],
            style = MaterialTheme.typography.bodyMedium,
            color = LevyraMuted,
            modifier = Modifier
                .fillMaxWidth()
                .clip(StudioCardShape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(20.dp)
        )
        return
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(placed, key = { it.first }) { entry ->
            PlacedWidgetCard(copy = copy, widgetId = entry.first, family = entry.second, onClick = { onEdit(entry) })
        }
        if (hasOriginal) {
            item(key = "original") { OriginalWidgetCard(copy) }
        }
    }
}

@Composable
private fun PlacedWidgetCard(copy: WidgetStudioCopy, widgetId: Int, family: StudioWidgetFamily, onClick: () -> Unit) {
    val context = LocalContext.current
    val config by produceState<StudioWidgetConfig?>(initialValue = null, widgetId) {
        value = withContext(Dispatchers.IO) { WidgetStudioStore.config(context, widgetId, family) }
    }
    val size = remember(widgetId) { placedSize(context, widgetId, family) }
    Column(
        modifier = Modifier
            .width(196.dp)
            .clip(StudioCardShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .levyraPressable(onClick = onClick, role = Role.Button, onClickLabel = copy[WidgetStudioText.Edit])
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ThumbnailStage(height = 132.dp) {
            config?.let { WidgetThumbnail(family = family, config = it, size = size, boxWidth = 176.dp, boxHeight = 116.dp) }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(familyName(family), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = LevyraText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(familyDescription(copy, family), style = MaterialTheme.typography.labelMedium, color = LevyraMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun OriginalWidgetCard(copy: WidgetStudioCopy) {
    Column(
        modifier = Modifier
            .width(196.dp)
            .clip(StudioCardShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), StudioCardShape)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ThumbnailStage(height = 132.dp) {
            Icon(
                painterResource(R.drawable.ic_widget_playing),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(copy[WidgetStudioText.OriginalName], style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = LevyraText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(copy[WidgetStudioText.OriginalDesc], style = MaterialTheme.typography.labelMedium, color = LevyraMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Rounded.Lock, contentDescription = copy[WidgetStudioText.Locked], tint = LevyraMuted, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun GalleryCard(
    copy: WidgetStudioCopy,
    family: StudioWidgetFamily,
    pinSupported: Boolean,
    demo: StudioPlaybackState?,
    modifier: Modifier
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .clip(StudioCardShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ThumbnailStage(height = 128.dp) {
            BoxWithConstraints(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                WidgetThumbnail(
                    family = family,
                    config = family.defaultConfig,
                    size = WidgetStudioRenderer.defaultSize(family),
                    boxWidth = maxWidth - 16.dp,
                    boxHeight = maxHeight - 16.dp,
                    demo = demo
                )
            }
        }
        Column(modifier = Modifier.padding(horizontal = 4.dp)) {
            Text(familyName(family), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = LevyraText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                familyDescription(copy, family),
                style = MaterialTheme.typography.bodySmall,
                color = LevyraMuted,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (pinSupported) {
            FilledTonalButton(
                onClick = { requestPin(context, family) },
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(copy[WidgetStudioText.AddToHome], maxLines = 1)
            }
        }
    }
}

@Composable
private fun ThumbnailStage(height: Dp, content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(scheme.surfaceContainerHighest, scheme.surfaceContainerLow)))
    ) {
        content()
    }
}

private fun hasOriginalWidget(context: Context): Boolean =
    AppWidgetManager.getInstance(context)
        ?.getAppWidgetIds(ComponentName(context, LevyraWidgetProvider::class.java))
        ?.isNotEmpty() == true

private fun requestPin(context: Context, family: StudioWidgetFamily) {
    val manager = AppWidgetManager.getInstance(context) ?: return
    runCatching { manager.requestPinAppWidget(ComponentName(context, family.providerClass), null, null) }
}

private fun placedSize(context: Context, widgetId: Int, family: StudioWidgetFamily): StudioWidgetSize =
    WidgetStudioRenderer.sizeOf(context, AppWidgetManager.getInstance(context)?.getAppWidgetOptions(widgetId), family)

@Composable
private fun WidgetStudioEditor(
    copy: WidgetStudioCopy,
    widgetId: Int,
    family: StudioWidgetFamily,
    configureMode: Boolean,
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val stored by produceState<StudioWidgetConfig?>(initialValue = null, widgetId) {
        value = withContext(Dispatchers.IO) { WidgetStudioStore.config(context, widgetId, family) }
    }
    var draft by remember(widgetId) { mutableStateOf<StudioWidgetConfig?>(null) }
    val config = draft ?: stored ?: return
    val size = remember(widgetId) { placedSize(context, widgetId, family) }
    val apply: (StudioWidgetConfig) -> Unit = { next ->
        val normalized = family.normalize(next)
        draft = normalized
        WidgetStudioStore.saveConfig(context, widgetId, family, normalized)
        WidgetStudioEngine.requestRender(context)
    }
    val isDefault = config == family.defaultConfig
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    Column(modifier = Modifier.fillMaxSize()) {
        StudioHeader(
            title = familyName(family),
            subtitle = copy[WidgetStudioText.LiveHint],
            backLabel = copy[WidgetStudioText.Back],
            onBack = onBack
        ) {
            TextButton(onClick = { apply(family.defaultConfig) }, enabled = !isDefault) {
                Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(copy[WidgetStudioText.Restore], maxLines = 1)
            }
        }
        EditorStage(
            family = family,
            config = config,
            size = size,
            maxHeight = screenHeight * 0.36f,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = if (configureMode) 104.dp else 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            editorSections(copy, family, config, apply)
        }
    }
    if (configureMode) ConfigureDoneButton(copy[WidgetStudioText.Done], onDone)
}

@Composable
private fun ConfigureDoneButton(label: String, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
        androidx.compose.material3.Button(
            onClick = onClick,
            modifier = Modifier
                .navigationBarsPadding()
                .padding(20.dp)
                .height(56.dp)
        ) {
            Icon(Icons.Rounded.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(label)
        }
    }
}

@Composable
private fun EditorStage(
    family: StudioWidgetFamily,
    config: StudioWidgetConfig,
    size: StudioWidgetSize,
    maxHeight: Dp,
    modifier: Modifier
) {
    val scheme = MaterialTheme.colorScheme
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .clip(StudioStageShape)
            .background(Brush.verticalGradient(listOf(scheme.surfaceContainerHigh, scheme.surfaceContainerLow)))
            .border(1.dp, scheme.outlineVariant.copy(alpha = 0.35f), StudioStageShape)
            .padding(vertical = 20.dp, horizontal = 16.dp)
    ) {
        WidgetThumbnail(
            family = family,
            config = config,
            size = size,
            boxWidth = maxWidth,
            boxHeight = maxHeight - 40.dp,
            pixelScale = 1f
        )
    }
}

@Composable
private fun StyleCard(
    family: StudioWidgetFamily,
    config: StudioWidgetConfig,
    label: String,
    selected: Boolean,
    selectedLabel: String,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val background by animateColorAsState(if (selected) scheme.secondaryContainer else scheme.surfaceContainer, label = "style-card")
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(background)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) scheme.primary else scheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(24.dp)
            )
            .levyraPressable(onClick = onClick, role = Role.RadioButton, onClickLabel = label)
            .semantics {
                this.selected = selected
                if (selected) stateDescription = selectedLabel
            }
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.25f)
                .clip(RoundedCornerShape(18.dp))
                .background(scheme.surfaceContainerLowest)
        ) {
            BoxWithConstraints(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                WidgetThumbnail(
                    family = family,
                    config = config,
                    size = WidgetStudioRenderer.defaultSize(family),
                    boxWidth = maxWidth - 12.dp,
                    boxHeight = maxHeight - 12.dp
                )
            }
        }
        StyleCardLabel(label, selected)
    }
}

@Composable
private fun StyleCardLabel(label: String, selected: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold),
            color = if (selected) scheme.onSecondaryContainer else LevyraText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(scheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(15.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaletteGrid(copy: WidgetStudioCopy, config: StudioWidgetConfig, onSelect: (StudioPalette) -> Unit) {
    val context = LocalContext.current
    val artworkSeed = remember {
        val state = WidgetStudioStore.playbackState(context)
        WidgetStudioArtwork.seed(state.artworkUrl) ?: state.accentSeed.takeIf { it != 0 }
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        maxItemsInEachRow = 3,
        modifier = Modifier.fillMaxWidth()
    ) {
        StudioPalette.entries.forEach { palette ->
            val scheme = remember(palette, config.customColor, artworkSeed) {
                WidgetStudioColors.scheme(context, palette, config.customColor, artworkSeed)
            }
            PaletteChoice(
                label = paletteName(copy, palette),
                surface = Color(scheme.surfaceContainer),
                accent = Color(scheme.primary),
                selected = palette == config.palette,
                onClick = { onSelect(palette) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PaletteChoice(
    label: String,
    surface: Color,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) scheme.secondaryContainer else Color.Transparent)
            .levyraPressable(onClick = onClick, role = Role.RadioButton, onClickLabel = label)
            .semantics { this.selected = selected }
            .padding(vertical = 10.dp, horizontal = 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(0.5f to surface, 0.5f to accent))
                .border(if (selected) 3.dp else 1.dp, if (selected) scheme.primary else scheme.outlineVariant, CircleShape)
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(scheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(16.dp))
                }
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) scheme.onSecondaryContainer else LevyraText,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SwatchRow(label: String, selected: Int, onSelect: (Int) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(12.dp)
    ) {
        StudioWidgetConfig.customSwatches.forEachIndexed { index, color ->
            val isSelected = color == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(if (isSelected) RoundedCornerShape(14.dp) else CircleShape)
                    .background(Color(color))
                    .levyraPressable(onClick = { onSelect(color) }, role = Role.RadioButton, onClickLabel = "$label ${index + 1}")
                    .semantics { this.selected = isSelected }
            ) {
                if (isSelected) Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ArtworkSegments(
    copy: WidgetStudioCopy,
    family: StudioWidgetFamily,
    selected: StudioArtworkMode,
    onSelect: (StudioArtworkMode) -> Unit
) {
    val modes = family.artworkModes
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        modes.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = mode == selected,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(artworkName(copy, mode), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ControlTiles(
    copy: WidgetStudioCopy,
    family: StudioWidgetFamily,
    selected: Set<StudioControl>,
    onToggle: (StudioControl) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 3,
        modifier = Modifier.fillMaxWidth()
    ) {
        family.controls.forEach { control ->
            val on = control in selected
            val label = controlName(copy, control)
            val background by animateColorAsState(if (on) scheme.primaryContainer else scheme.surfaceContainer, label = "control-tile")
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(if (on) 22.dp else 16.dp))
                    .background(background)
                    .levyraPressable(onClick = { onToggle(control) }, role = Role.Switch, onClickLabel = label)
                    .semantics { this.selected = on }
                    .padding(vertical = 14.dp, horizontal = 6.dp)
            ) {
                Icon(
                    painterResource(controlIcon(control)),
                    contentDescription = null,
                    tint = if (on) scheme.onPrimaryContainer else LevyraMuted,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (on) FontWeight.Bold else FontWeight.Medium),
                    color = if (on) scheme.onPrimaryContainer else LevyraText,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        repeat((3 - family.controls.size % 3) % 3) { Spacer(modifier = Modifier.weight(1f)) }
    }
}

@Composable
private fun WidgetThumbnail(
    family: StudioWidgetFamily,
    config: StudioWidgetConfig,
    size: StudioWidgetSize,
    boxWidth: Dp,
    boxHeight: Dp,
    pixelScale: Float = THUMBNAIL_PIXEL_SCALE,
    demo: StudioPlaybackState? = null
) {
    val context = LocalContext.current
    val artworkVersion by WidgetStudioArtwork.version.collectAsState()
    val views by produceState<RemoteViews?>(initialValue = null, family, config, size, artworkVersion, demo) {
        value = withContext(Dispatchers.Default) {
            val state = demo ?: WidgetStudioStore.playbackState(context)
            if (state.hasTrack && WidgetStudioArtwork.source(state.artworkUrl) == null) {
                WidgetStudioArtwork.request(state.artworkUrl, skipWhenStale = demo == null) { }
            }
            runCatching { WidgetStudioRenderer.build(context, family, config, state, size, interactive = false, pixelScale = pixelScale) }
                .getOrNull()
        }
    }
    val scale = minOf(boxWidth.value / size.widthDp, boxHeight.value / size.heightDp, 1f).coerceAtLeast(0.1f)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size((size.widthDp * scale).dp, (size.heightDp * scale).dp)
    ) {
        val current = views ?: return@Box
        AndroidView(
            factory = { FrameLayout(it) },
            update = { container ->
                container.removeAllViews()
                runCatching { current.apply(container.context, container) }
                    .onSuccess { container.addView(it, FrameLayout.LayoutParams(-1, -1)) }
            },
            modifier = Modifier
                .requiredSize(size.widthDp.dp, size.heightDp.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        )
    }
}

private fun LazyListScope.editorSections(
    copy: WidgetStudioCopy,
    family: StudioWidgetFamily,
    config: StudioWidgetConfig,
    apply: (StudioWidgetConfig) -> Unit
) {
    if (family.styles.size > 1) {
        item(key = "style", contentType = "group") { StyleGroup(copy, family, config, apply) }
    }
    item(key = "colors", contentType = "group") { ColorsGroup(copy, config, apply) }
    if (family.artworkModes.size > 1) {
        item(key = "artwork", contentType = "group") {
            StudioGroup(copy[WidgetStudioText.Artwork], null) {
                ArtworkSegments(copy = copy, family = family, selected = config.artwork, onSelect = { apply(config.copy(artwork = it)) })
            }
        }
    }
    if (family.controls.isNotEmpty()) {
        item(key = "controls", contentType = "group") {
            StudioGroup(copy[WidgetStudioText.Controls], copy[WidgetStudioText.ControlsHint]) {
                ControlTiles(
                    copy = copy,
                    family = family,
                    selected = config.controls,
                    onToggle = { control -> apply(config.copy(controls = toggled(family, config.controls, control))) }
                )
            }
        }
    }
}

@Composable
private fun StyleGroup(
    copy: WidgetStudioCopy,
    family: StudioWidgetFamily,
    config: StudioWidgetConfig,
    apply: (StudioWidgetConfig) -> Unit
) {
    StudioGroup(copy[WidgetStudioText.Style], copy[WidgetStudioText.StyleHint]) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            family.styles.forEach { style ->
                StyleCard(
                    family = family,
                    config = config.copy(style = style),
                    label = styleName(copy, style),
                    selected = style == config.style,
                    selectedLabel = copy[WidgetStudioText.Selected],
                    onClick = { apply(config.copy(style = style)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ColorsGroup(copy: WidgetStudioCopy, config: StudioWidgetConfig, apply: (StudioWidgetConfig) -> Unit) {
    StudioGroup(copy[WidgetStudioText.Colors], copy[WidgetStudioText.ColorsHint]) {
        PaletteGrid(copy = copy, config = config, onSelect = { apply(config.copy(palette = it)) })
        AnimatedVisibility(visible = config.palette == StudioPalette.Custom) {
            SwatchRow(
                label = copy[WidgetStudioText.Custom],
                selected = config.customColor,
                onSelect = { apply(config.copy(customColor = it)) }
            )
        }
        if (config.palette == StudioPalette.MaterialYou && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            Text(copy[WidgetStudioText.MaterialYouUnavailable], style = MaterialTheme.typography.bodySmall, color = LevyraMuted)
        }
    }
}

private fun toggled(family: StudioWidgetFamily, current: Set<StudioControl>, control: StudioControl): Set<StudioControl> {
    val next = if (control in current) current - control else current + control
    return family.controls.filterTo(linkedSetOf()) { it in next }
}

private fun familyName(family: StudioWidgetFamily): String = when (family) {
    StudioWidgetFamily.Mini -> "Levyra Mini"
    StudioWidgetFamily.Slim -> "Levyra Slim"
    StudioWidgetFamily.Expressive -> "Levyra Expressive"
    StudioWidgetFamily.Hero -> "Levyra Artwork Hero"
    StudioWidgetFamily.Immersive -> "Levyra Immersive"
    StudioWidgetFamily.Vinyl -> "Levyra Vinyl"
    StudioWidgetFamily.QuickActions -> "Levyra Quick Actions"
}

private fun familyDescription(copy: WidgetStudioCopy, family: StudioWidgetFamily): String = copy[
    when (family) {
        StudioWidgetFamily.Mini -> WidgetStudioText.DescMini
        StudioWidgetFamily.Slim -> WidgetStudioText.DescSlim
        StudioWidgetFamily.Expressive -> WidgetStudioText.DescExpressive
        StudioWidgetFamily.Hero -> WidgetStudioText.DescHero
        StudioWidgetFamily.Immersive -> WidgetStudioText.DescImmersive
        StudioWidgetFamily.Vinyl -> WidgetStudioText.DescVinyl
        StudioWidgetFamily.QuickActions -> WidgetStudioText.DescQuick
    }
]

private fun controlIcon(control: StudioControl): Int = when (control) {
    StudioControl.Like -> R.drawable.ic_studio_favorite
    StudioControl.Previous -> R.drawable.ic_studio_previous
    StudioControl.Shuffle -> R.drawable.ic_studio_shuffle
    StudioControl.Repeat -> R.drawable.ic_studio_repeat
    StudioControl.Progress -> R.drawable.ic_studio_progress
    StudioControl.Lyrics -> R.drawable.ic_studio_lyrics
    StudioControl.Queue -> R.drawable.ic_studio_queue
    StudioControl.Flow -> R.drawable.ic_studio_flow
    StudioControl.Downloads -> R.drawable.ic_studio_download
}

private fun styleName(copy: WidgetStudioCopy, style: StudioWidgetStyle): String = copy[
    when (style) {
        StudioWidgetStyle.Expressive -> WidgetStudioText.StyleExpressive
        StudioWidgetStyle.Minimal -> WidgetStudioText.StyleMinimal
        StudioWidgetStyle.ArtworkFocus -> WidgetStudioText.StyleArtworkFocus
        StudioWidgetStyle.ArtworkControls -> WidgetStudioText.StyleArtworkControls
        StudioWidgetStyle.Vinyl -> WidgetStudioText.StyleVinyl
        StudioWidgetStyle.Immersive -> WidgetStudioText.StyleImmersive
    }
]

private fun paletteName(copy: WidgetStudioCopy, palette: StudioPalette): String = copy[
    when (palette) {
        StudioPalette.Artwork -> WidgetStudioText.PaletteArtwork
        StudioPalette.MaterialYou -> WidgetStudioText.PaletteMaterialYou
        StudioPalette.Levyra -> WidgetStudioText.PaletteLevyra
        StudioPalette.Light -> WidgetStudioText.PaletteLight
        StudioPalette.Dark -> WidgetStudioText.PaletteDark
        StudioPalette.Custom -> WidgetStudioText.PaletteCustom
    }
]

private fun artworkName(copy: WidgetStudioCopy, mode: StudioArtworkMode): String = copy[
    when (mode) {
        StudioArtworkMode.Normal -> WidgetStudioText.ArtworkNormal
        StudioArtworkMode.Large -> WidgetStudioText.ArtworkLarge
        StudioArtworkMode.FullBleed -> WidgetStudioText.ArtworkFullBleed
        StudioArtworkMode.Circular -> WidgetStudioText.ArtworkCircular
    }
]

private fun controlName(copy: WidgetStudioCopy, control: StudioControl): String = copy[
    when (control) {
        StudioControl.Like -> WidgetStudioText.ControlLike
        StudioControl.Previous -> WidgetStudioText.ControlPrevious
        StudioControl.Shuffle -> WidgetStudioText.ControlShuffle
        StudioControl.Repeat -> WidgetStudioText.ControlRepeat
        StudioControl.Progress -> WidgetStudioText.ControlProgress
        StudioControl.Lyrics -> WidgetStudioText.ControlLyrics
        StudioControl.Queue -> WidgetStudioText.ControlQueue
        StudioControl.Flow -> WidgetStudioText.ControlFlow
        StudioControl.Downloads -> WidgetStudioText.ControlDownloads
    }
]
