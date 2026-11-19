package com.luc4n3x.levyra.ui.support

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.SupportDonationCopy
import com.luc4n3x.levyra.ui.i18n.supportDonationCopy

private val donationHeart = Color(0xFFEA4AAA)

private data class DonationChannel(
    val name: String,
    val url: String,
    val icon: ImageVector,
    val accent: Color,
    val recurring: Boolean
)

private val donationChannels = listOf(
    DonationChannel("GitHub Sponsors", "https://github.com/sponsors/LUC4N3X", Icons.Rounded.Favorite, donationHeart, recurring = true),
    DonationChannel("Ko-fi", "https://ko-fi.com/luc4n3x", Icons.Rounded.LocalCafe, Color(0xFF29ABE0), recurring = false),
    DonationChannel("Buy Me a Coffee", "https://buymeacoffee.com/luc4n3x", Icons.Rounded.Coffee, Color(0xFFFFDD00), recurring = false)
)

@Composable
fun SupportDonationSettingsCard(
    languageCode: String,
    modifier: Modifier = Modifier
) {
    val copy = remember(languageCode) { supportDonationCopy(languageCode) }
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(DonationCardShape)
            .background(donationHeart.copy(alpha = 0.10f))
            .clickable(role = Role.Button, onClickLabel = copy.title) { sheetOpen = true }
            .heightIn(min = 72.dp)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(donationHeart.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.VolunteerActivism, contentDescription = null, tint = donationHeart, modifier = Modifier.size(24.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = copy.title,
                color = scheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = copy.subtitle,
                color = scheme.onSurfaceVariant.copy(alpha = 0.88f),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    if (sheetOpen) {
        SupportDonationSheet(copy, languageCode) { sheetOpen = false }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SupportDonationSheet(copy: SupportDonationCopy, languageCode: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val failure = remember(languageCode) { LevyraStrings.forCode(languageCode).cannotOpenExternalLink }
    val scheme = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(donationHeart.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.VolunteerActivism, contentDescription = null, tint = donationHeart, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = copy.title,
                color = scheme.onSurface,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = copy.body,
                color = scheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(22.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                donationChannels.forEach { channel ->
                    DonationChannelRow(
                        channel = channel,
                        detail = if (channel.recurring) copy.recurring else copy.oneTime
                    ) {
                        openDonationLink(context, channel.url, failure)
                    }
                }
            }
        }
    }
}

@Composable
private fun DonationChannelRow(channel: DonationChannel, detail: String, onOpen: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DonationRowShape)
            .background(scheme.surfaceContainerHigh)
            .clickable(role = Role.Button, onClickLabel = channel.name, onClick = onOpen)
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(channel.accent.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(channel.icon, contentDescription = null, tint = channel.accent, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(channel.name, color = scheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(detail, color = scheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(
            Icons.AutoMirrored.Rounded.OpenInNew,
            contentDescription = null,
            tint = scheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

private fun openDonationLink(context: Context, url: String, failureMessage: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        .onFailure { Toast.makeText(context, failureMessage, Toast.LENGTH_LONG).show() }
}

private val DonationCardShape = RoundedCornerShape(20.dp)
private val DonationRowShape = RoundedCornerShape(18.dp)
