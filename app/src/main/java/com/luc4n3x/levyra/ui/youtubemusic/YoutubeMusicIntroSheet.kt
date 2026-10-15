package com.luc4n3x.levyra.ui.youtubemusic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.RoundedPolygon
import com.luc4n3x.levyra.ui.LevyraLogoMark
import com.luc4n3x.levyra.ui.components.levyraGroupedListShape
import com.luc4n3x.levyra.ui.i18n.OnboardingYoutubeMusicCopy

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun YoutubeMusicIntroSheet(copy: OnboardingYoutubeMusicCopy, onConnect: () -> Unit, onSkip: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onSkip,
        sheetState = rememberBottomSheetState(SheetValue.Hidden, setOf(SheetValue.Hidden, SheetValue.Expanded)),
        containerColor = colors.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(colors.surfaceContainerHighest, MaterialShapes.Cookie9Sided.toShape()),
                    contentAlignment = Alignment.Center
                ) {
                    LevyraLogoMark(size = 36.dp, contentDescription = null)
                }
                Row(Modifier.padding(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(3) { index ->
                        Box(
                            Modifier
                                .size(6.dp)
                                .background(colors.primary.copy(alpha = 0.35f + 0.25f * index), CircleShape)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(colors.primary, MaterialShapes.SoftBurst.toShape()),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.LibraryMusic, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(30.dp))
                }
            }
            Spacer(Modifier.height(22.dp))
            Text(
                text = copy.announceTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = colors.onSurface,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.height(8.dp))
            Text(copy.body, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))
            IntroRow(Icons.Rounded.Schedule, MaterialShapes.Cookie4Sided, copy.optionalTitle, copy.optionalBody, levyraGroupedListShape(0, 2))
            Spacer(Modifier.height(3.dp))
            IntroRow(Icons.Rounded.Lock, MaterialShapes.Clover4Leaf, copy.secureTitle, copy.secureBody, levyraGroupedListShape(1, 2))
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onConnect,
                shapes = ButtonDefaults.shapes(),
                contentPadding = PaddingValues(horizontal = 24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(copy.connect, fontSize = 16.sp, fontWeight = FontWeight.Black, maxLines = 1)
            }
            TextButton(
                onClick = onSkip,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(copy.skip, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun IntroRow(icon: ImageVector, polygon: RoundedPolygon, title: String, body: String, shape: Shape) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceContainerHigh, shape)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(colors.primaryContainer, polygon.toShape()),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = colors.onSurface)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
    }
}
