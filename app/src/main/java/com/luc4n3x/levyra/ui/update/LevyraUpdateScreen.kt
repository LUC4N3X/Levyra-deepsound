package com.luc4n3x.levyra.ui.update

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.R
import com.luc4n3x.levyra.domain.AppUpdateInfo
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.components.LevyraBackButton
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.updateReleaseCopy
import com.luc4n3x.levyra.ui.i18n.updateReleaseHighlights
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraGlassBorder
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraOnAccent
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraText

@Composable
fun LevyraUpdateScreen(
    update: AppUpdateInfo,
    strings: LevyraStrings,
    languageCode: String,
    onUpdate: () -> Unit,
    onLater: () -> Unit,
    modifier: Modifier = Modifier
) {
    val notes = remember(strings.code, update.latestVersionName, update.releaseHighlights) {
        strings.updateReleaseHighlights(update.latestVersionName, update.releaseHighlights).take(5)
    }
    val meta = remember(update.publishedAtEpochMs, update.assetSizeBytes, languageCode) {
        updateMetaLine(update.publishedAtEpochMs, update.assetSizeBytes, languageCode)
    }

    LevyraReleaseContent(
        version = update.latestVersionName,
        notes = notes,
        meta = meta,
        strings = strings,
        animated = LocalAnimationsEnabled.current,
        palette = ReleasePalette(LevyraBlack, LevyraCyan, LevyraOnAccent, LevyraText, LevyraMuted, LevyraPanel, LevyraGlassBorder),
        protectionDetail = strings.updateReleaseCopy().releaseProtectionDetail,
        onUpdate = onUpdate,
        onLater = onLater,
        brandArt = { artModifier ->
            AsyncImage(
                model = R.drawable.onboarding_levyra_brand,
                contentDescription = "Levyra Deepsound",
                contentScale = ContentScale.Fit,
                modifier = artModifier
            )
        },
        backButton = { LevyraBackButton(onClick = onLater, contentDescription = strings.later, modifier = it) },
        updateIcon = { Icon(Icons.Rounded.SystemUpdateAlt, contentDescription = null, modifier = it) },
        protectionIcon = { Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = LevyraCyan, modifier = it) },
        modifier = modifier,
        bannerInsets = Modifier.statusBarsPadding(),
        contentInsets = Modifier.navigationBarsPadding()
    )
}
