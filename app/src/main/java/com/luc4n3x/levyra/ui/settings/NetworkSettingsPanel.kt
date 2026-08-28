package com.luc4n3x.levyra.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.LevyraDnsMode
import com.luc4n3x.levyra.domain.LevyraNetworkSettings
import com.luc4n3x.levyra.domain.LevyraNetworkSettingsError
import com.luc4n3x.levyra.domain.LevyraNetworkTestOutcome
import com.luc4n3x.levyra.domain.LevyraProxyMode
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraOnAccent
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraText

private val DnsModes = listOf(
    LevyraDnsMode.System,
    LevyraDnsMode.Cloudflare,
    LevyraDnsMode.Google,
    LevyraDnsMode.AdGuard,
    LevyraDnsMode.Quad9,
    LevyraDnsMode.Custom
)

private val ProxyModes = listOf(
    LevyraProxyMode.Disabled,
    LevyraProxyMode.Http,
    LevyraProxyMode.Socks
)

@Composable
internal fun NetworkSettingsPanel(
    settings: LevyraNetworkSettings,
    proxyPasswordSet: Boolean,
    testing: Boolean,
    testOutcome: LevyraNetworkTestOutcome?,
    errors: List<LevyraNetworkSettingsError>,
    onApply: (LevyraNetworkSettings, String?) -> Unit,
    onTest: (LevyraNetworkSettings, String?) -> Unit
) {
    val strings = LocalLevyraStrings.current
    var dnsMode by rememberSaveable(settings.dnsMode) { mutableStateOf(settings.dnsMode.id) }
    var customDohUrl by rememberSaveable(settings.customDohUrl) { mutableStateOf(settings.customDohUrl) }
    var proxyMode by rememberSaveable(settings.proxyMode) { mutableStateOf(settings.proxyMode.id) }
    var proxyHost by rememberSaveable(settings.proxyHost) { mutableStateOf(settings.proxyHost) }
    var proxyPort by rememberSaveable(settings.proxyPort) { mutableStateOf(settings.proxyPort.toString()) }
    var proxyAuth by rememberSaveable(settings.proxyAuthenticationEnabled) {
        mutableStateOf(settings.proxyAuthenticationEnabled)
    }
    var proxyUsername by rememberSaveable(settings.proxyUsername) { mutableStateOf(settings.proxyUsername) }
    var proxyPassword by remember { mutableStateOf("") }
    var bypassStreams by rememberSaveable(settings.bypassProxyForStreams) {
        mutableStateOf(settings.bypassProxyForStreams)
    }

    val edited = LevyraNetworkSettings(
        dnsMode = LevyraDnsMode.fromId(dnsMode),
        customDohUrl = customDohUrl,
        proxyMode = LevyraProxyMode.fromId(proxyMode),
        proxyHost = proxyHost,
        proxyPort = proxyPort.toIntOrNull() ?: 0,
        proxyUsername = proxyUsername,
        proxyAuthenticationEnabled = proxyAuth,
        bypassProxyForStreams = bypassStreams
    )
    val passwordArgument: String? = when {
        proxyPassword.isNotEmpty() -> proxyPassword
        !proxyAuth -> ""
        else -> null
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        NetworkCard {
            Text(strings.networkDns, color = LevyraText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(strings.networkSubtitle, color = LevyraMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            DnsModes.forEach { mode ->
                NetworkOptionRow(
                    label = dnsModeLabel(mode, strings),
                    selected = mode.id == dnsMode,
                    onClick = { dnsMode = mode.id }
                )
            }
            if (LevyraDnsMode.fromId(dnsMode) == LevyraDnsMode.Custom) {
                OutlinedTextField(
                    value = customDohUrl,
                    onValueChange = { customDohUrl = it.take(LevyraNetworkSettings.MAX_URL_LENGTH) },
                    singleLine = true,
                    label = { Text(strings.networkCustomDohUrl) },
                    isError = errors.any { it.name.startsWith("CustomDohUrl") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        NetworkCard {
            Text(strings.networkProxy, color = LevyraText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            ProxyModes.forEach { mode ->
                NetworkOptionRow(
                    label = proxyModeLabel(mode, strings),
                    selected = mode.id == proxyMode,
                    onClick = { proxyMode = mode.id }
                )
            }
            if (LevyraProxyMode.fromId(proxyMode) != LevyraProxyMode.Disabled) {
                OutlinedTextField(
                    value = proxyHost,
                    onValueChange = { proxyHost = it.take(LevyraNetworkSettings.MAX_HOST_LENGTH) },
                    singleLine = true,
                    label = { Text(strings.networkProxyHost) },
                    isError = errors.contains(LevyraNetworkSettingsError.ProxyHostMissing) ||
                        errors.contains(LevyraNetworkSettingsError.ProxyHostInvalid),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = proxyPort,
                    onValueChange = { value -> proxyPort = value.filter(Char::isDigit).take(5) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text(strings.networkProxyPort) },
                    isError = errors.contains(LevyraNetworkSettingsError.ProxyPortOutOfRange),
                    modifier = Modifier.fillMaxWidth()
                )
                NetworkToggleRow(
                    label = strings.networkProxyAuthentication,
                    checked = proxyAuth,
                    onCheckedChange = { proxyAuth = it }
                )
                if (proxyAuth) {
                    OutlinedTextField(
                        value = proxyUsername,
                        onValueChange = { proxyUsername = it.take(LevyraNetworkSettings.MAX_CREDENTIAL_LENGTH) },
                        singleLine = true,
                        label = { Text(strings.networkProxyUsername) },
                        isError = errors.contains(LevyraNetworkSettingsError.ProxyUsernameMissing),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = proxyPassword,
                        onValueChange = { proxyPassword = it.take(LevyraNetworkSettings.MAX_CREDENTIAL_LENGTH) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        label = { Text(strings.networkProxyPassword) },
                        isError = errors.contains(LevyraNetworkSettingsError.ProxyPasswordMissing),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (proxyPasswordSet && proxyPassword.isEmpty()) {
                        Text(strings.saved, color = LevyraMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                NetworkToggleRow(
                    label = strings.networkBypassStreams,
                    checked = bypassStreams,
                    onCheckedChange = { bypassStreams = it },
                    subtitle = strings.networkBypassStreamsSubtitle
                )
            }
        }

        if (errors.isNotEmpty()) {
            NetworkCard {
                errors.distinct().forEach { error ->
                    Text(
                        networkErrorText(error, strings),
                        color = LevyraText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        testOutcome?.let { outcome ->
            NetworkCard {
                Text(
                    networkTestText(outcome, strings),
                    color = if (outcome == LevyraNetworkTestOutcome.Success) LevyraCyan else LevyraText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Button(
            onClick = { onApply(edited, passwordArgument) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = LevyraCyan, contentColor = LevyraOnAccent)
        ) {
            Text(strings.save, fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = { onTest(edited, proxyPassword.takeIf { it.isNotEmpty() }) },
            enabled = !testing,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = LevyraPanel, contentColor = LevyraText)
        ) {
            Text(if (testing) strings.checking else strings.networkTest, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun NetworkCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(LevyraPanel)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}

@Composable
private fun NetworkOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick
            )
    ) {
        Text(
            label,
            color = if (selected) LevyraCyan else LevyraMuted,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun NetworkToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = LevyraText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            subtitle?.let {
                Text(it, color = LevyraMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun dnsModeLabel(mode: LevyraDnsMode, strings: LevyraStrings): String = when (mode) {
    LevyraDnsMode.System -> strings.networkDnsSystem
    LevyraDnsMode.Cloudflare -> "Cloudflare"
    LevyraDnsMode.Google -> "Google"
    LevyraDnsMode.AdGuard -> "AdGuard"
    LevyraDnsMode.Quad9 -> "Quad9"
    LevyraDnsMode.Custom -> strings.networkDnsCustom
}

private fun proxyModeLabel(mode: LevyraProxyMode, strings: LevyraStrings): String = when (mode) {
    LevyraProxyMode.Disabled -> strings.networkProxyDisabled
    LevyraProxyMode.Http -> "HTTP"
    LevyraProxyMode.Socks -> "SOCKS"
}

private fun networkErrorText(error: LevyraNetworkSettingsError, strings: LevyraStrings): String = when (error) {
    LevyraNetworkSettingsError.ProxyHostMissing,
    LevyraNetworkSettingsError.ProxyHostInvalid -> strings.networkErrorProxyHost
    LevyraNetworkSettingsError.ProxyPortOutOfRange -> strings.networkErrorProxyPort
    LevyraNetworkSettingsError.ProxyUsernameMissing,
    LevyraNetworkSettingsError.ProxyPasswordMissing -> strings.networkErrorProxyCredentials
    LevyraNetworkSettingsError.CustomDohUrlMissing,
    LevyraNetworkSettingsError.CustomDohUrlNotHttps,
    LevyraNetworkSettingsError.CustomDohUrlInvalid -> strings.networkErrorDohUrl
}

private fun networkTestText(outcome: LevyraNetworkTestOutcome, strings: LevyraStrings): String = when (outcome) {
    LevyraNetworkTestOutcome.Success -> strings.networkTestSuccess
    LevyraNetworkTestOutcome.InvalidConfiguration -> strings.networkTestInvalid
    LevyraNetworkTestOutcome.DnsResolutionFailed -> strings.networkTestDnsFailed
    LevyraNetworkTestOutcome.ProxyAuthenticationFailed -> strings.networkTestProxyAuthFailed
    LevyraNetworkTestOutcome.Timeout -> strings.networkTestTimeout
    LevyraNetworkTestOutcome.ConnectionRefused -> strings.networkTestRefused
    LevyraNetworkTestOutcome.TlsFailure -> strings.networkTestTls
    LevyraNetworkTestOutcome.UnknownError -> strings.networkTestUnknown
}
