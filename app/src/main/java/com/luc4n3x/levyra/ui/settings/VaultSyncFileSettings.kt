package com.luc4n3x.levyra.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Refresh
import com.luc4n3x.levyra.domain.LevyraVaultStatus
import com.luc4n3x.levyra.domain.VaultSyncFileAction
import com.luc4n3x.levyra.domain.VaultSyncFileState
import com.luc4n3x.levyra.ui.SettingsButton
import com.luc4n3x.levyra.ui.SettingsGroupCollector
import com.luc4n3x.levyra.ui.SettingsInfoCard
import com.luc4n3x.levyra.ui.SettingsSectionLabel
import com.luc4n3x.levyra.ui.formatVaultTimestamp
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.vaultSyncFileCopy

internal fun SettingsGroupCollector.vaultSyncFileItems(
    strings: LevyraStrings,
    state: VaultSyncFileState,
    vaultStatus: LevyraVaultStatus,
    onAction: (VaultSyncFileAction) -> Unit
) {
    val copy = strings.vaultSyncFileCopy()
    standaloneItem { SettingsSectionLabel(copy.section) }
    item {
        SettingsInfoCard(icon = Icons.Rounded.CloudSync, title = copy.title, subtitle = copy.steps)
    }
    if (state.uri == null) {
        item {
            SettingsButton(
                icon = Icons.Rounded.CloudSync,
                title = copy.create,
                subtitle = copy.createSubtitle,
                onClick = { onAction(VaultSyncFileAction.Create) }
            )
        }
        item {
            SettingsButton(
                icon = Icons.Rounded.Link,
                title = copy.useExisting,
                subtitle = copy.useExistingSubtitle,
                onClick = { onAction(VaultSyncFileAction.UseExisting) }
            )
        }
        return
    }
    item {
        SettingsInfoCard(
            icon = if (state.accessLost) Icons.Rounded.CloudOff else Icons.Rounded.CloudSync,
            title = state.displayName.ifBlank { copy.title },
            subtitle = when {
                state.accessLost -> copy.accessLost
                !state.writesEnabled -> copy.paused
                state.lastWriteAtMs > 0L -> copy.updated(formatVaultTimestamp(state.lastWriteAtMs, strings.code))
                else -> copy.neverUpdated
            }
        )
    }
    if (state.accessLost) {
        item {
            SettingsButton(
                icon = Icons.Rounded.Link,
                title = copy.useExisting,
                subtitle = copy.useExistingSubtitle,
                onClick = { onAction(VaultSyncFileAction.UseExisting) }
            )
        }
    } else {
        item {
            SettingsButton(
                icon = Icons.Rounded.Refresh,
                title = if (vaultStatus == LevyraVaultStatus.Running) strings.vaultBusy else copy.updateNow,
                subtitle = copy.updateNowSubtitle,
                enabled = vaultStatus != LevyraVaultStatus.Running,
                onClick = { onAction(VaultSyncFileAction.UpdateNow) }
            )
        }
        item {
            SettingsButton(
                icon = Icons.Rounded.History,
                title = copy.restore,
                subtitle = copy.restoreSubtitle,
                onClick = { onAction(VaultSyncFileAction.Restore) }
            )
        }
    }
    item {
        SettingsButton(
            icon = Icons.Rounded.LinkOff,
            title = copy.unlink,
            subtitle = copy.unlinkSubtitle,
            onClick = { onAction(VaultSyncFileAction.Unlink) }
        )
    }
}
