package com.luc4n3x.levyra.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Subject
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraInk
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraViolet

private data class LevyraIntroCopy(
    val eyebrow: String,
    val title: String,
    val subtitle: String,
    val action: String,
    val sound: String,
    val lyrics: String,
    val flow: String
)

private fun levyraIntroCopy(languageCode: String, strings: LevyraStrings): LevyraIntroCopy {
    return if (languageCode.startsWith("it", ignoreCase = true)) {
        LevyraIntroCopy(
            eyebrow = "LA MUSICA, DAVVERO TUA",
            title = "Entra nel suono.",
            subtitle = "Levyra unisce ascolto, testi e controllo audio in un'esperienza viva, costruita intorno a te.",
            action = "Scopri Levyra",
            sound = "Suono su misura",
            lyrics = "Testi immersivi",
            flow = "Ascolto continuo"
        )
    } else {
        LevyraIntroCopy(
            eyebrow = strings.welcomeBadge,
            title = strings.welcomeTitle,
            subtitle = "Music, lyrics and sound controls move together in one living experience, shaped around you.",
            action = strings.startListening,
            sound = "Sound shaped for you",
            lyrics = "Immersive lyrics",
            flow = "Continuous listening"
        )
    }
}

@Composable
internal fun LevyraIntroExperience(
    languageCode: String,
    onContinue: () -> Unit
) {
    val strings = LevyraStrings.forCode(languageCode)
    val copy = remember(languageCode) { levyraIntroCopy(languageCode, strings) }
    val transition = rememberInfiniteTransition(label = "levyra-intro")
    val orbitRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(18_000),
            repeatMode = RepeatMode.Restart
        ),
        label = "intro-orbit"
    )
    val breathing by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2_800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "intro-breathing"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(120f)
            .background(LevyraBlack)
            .blockOverlayTouches()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val expanded = maxWidth >= 720.dp

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 110.dp, y = (-90).dp)
                .size(if (expanded) 520.dp else 360.dp)
                .blur(92.dp)
                .background(
                    Brush.radialGradient(
                        listOf(LevyraViolet.copy(alpha = 0.30f), Color.Transparent)
                    ),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-120).dp, y = 100.dp)
                .size(if (expanded) 560.dp else 400.dp)
                .blur(98.dp)
                .background(
                    Brush.radialGradient(
                        listOf(LevyraCyan.copy(alpha = 0.23f), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        if (expanded) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 54.dp, vertical = 34.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(70.dp)
            ) {
                LevyraIntroMark(
                    rotation = orbitRotation,
                    breathing = breathing,
                    modifier = Modifier.weight(0.92f)
                )
                LevyraIntroContent(
                    copy = copy,
                    onContinue = onContinue,
                    modifier = Modifier.weight(1.08f)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(2.dp))
                LevyraIntroMark(
                    rotation = orbitRotation,
                    breathing = breathing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.88f)
                )
                LevyraIntroContent(
                    copy = copy,
                    onContinue = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.12f)
                )
            }
        }
    }
}

@Composable
private fun LevyraIntroMark(
    rotation: Float,
    breathing: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(290.dp)
                .graphicsLayer {
                    rotationZ = rotation
                    scaleX = breathing
                    scaleY = breathing
                }
                .border(
                    width = 1.dp,
                    brush = Brush.sweepGradient(
                        listOf(
                            Color.Transparent,
                            LevyraCyan.copy(alpha = 0.72f),
                            Color.Transparent,
                            LevyraViolet.copy(alpha = 0.68f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(220.dp)
                .graphicsLayer {
                    rotationZ = -rotation * 0.62f
                    scaleX = 2f - breathing
                    scaleY = 2f - breathing
                }
                .border(
                    1.dp,
                    LevyraViolet.copy(alpha = 0.30f),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(154.dp)
                .shadow(
                    elevation = 42.dp,
                    shape = RoundedCornerShape(48.dp),
                    clip = false,
                    ambientColor = LevyraCyan.copy(alpha = 0.42f),
                    spotColor = LevyraViolet.copy(alpha = 0.52f)
                )
                .background(
                    Brush.linearGradient(
                        listOf(
                            LevyraCyan,
                            LevyraViolet,
                            Color(0xFFFA67C8)
                        )
                    ),
                    RoundedCornerShape(48.dp)
                )
                .border(2.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(48.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(72.dp)
            )
        }
        Surface(
            color = LevyraInk.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 20.dp)
        ) {
            Text(
                text = "LEVYRA",
                color = LevyraText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.2.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun LevyraIntroContent(
    copy: LevyraIntroCopy,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = copy.eyebrow,
            color = LevyraCyan,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.1.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = copy.title,
            color = LevyraText,
            fontSize = 42.sp,
            lineHeight = 44.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1.4).sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = copy.subtitle,
            color = LevyraMuted,
            fontSize = 16.sp,
            lineHeight = 23.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            IntroFeature(
                icon = Icons.Rounded.GraphicEq,
                label = copy.sound,
                accent = LevyraCyan,
                modifier = Modifier.weight(1f)
            )
            IntroFeature(
                icon = Icons.AutoMirrored.Rounded.Subject,
                label = copy.lyrics,
                accent = LevyraViolet,
                modifier = Modifier.weight(1f)
            )
            IntroFeature(
                icon = Icons.Rounded.Headphones,
                label = copy.flow,
                accent = Color(0xFFFA67C8),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Surface(
            color = Color.Transparent,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .shadow(
                    20.dp,
                    RoundedCornerShape(22.dp),
                    clip = false,
                    ambientColor = LevyraCyan.copy(alpha = 0.22f),
                    spotColor = LevyraViolet.copy(alpha = 0.30f)
                )
                .clickable(onClick = onContinue)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(listOf(LevyraCyan, LevyraViolet)),
                        RoundedCornerShape(22.dp)
                    )
                    .padding(horizontal = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = copy.action,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun IntroFeature(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White.copy(alpha = 0.055f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.085f)),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.height(82.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 11.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(21.dp)
            )
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.76f),
                fontSize = 10.5.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

private fun Modifier.blockOverlayTouches(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Final)
            event.changes.forEach { change ->
                if (!change.isConsumed) change.consume()
            }
        }
    }
}
