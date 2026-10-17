package io.github.tnyfishy.all4hyper.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.tnyfishy.all4hyper.Link
import io.github.tnyfishy.all4hyper.R
import io.github.tnyfishy.all4hyper.Store

enum class RootState { Checking, Granted, Denied }

/** What the restart button can restart. */
enum class RestartTarget { SecurityCenter, SystemUi, System }

class HomeActions(
    val restart: (RestartTarget) -> Unit,
    val addScope: () -> Unit,
)

fun LazyListScope.homePage(link: Link, store: Store, actions: HomeActions) {
    item { Hero(link, actions) }
    item { ActiveCard(store) }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Hero(link: Link, actions: HomeActions) {
    val colors = MaterialTheme.colorScheme
    val ready = link as? Link.Ready
    val healthy = ready?.scoped == true
    val container = when {
        link is Link.Connecting -> colors.surfaceContainerHigh
        healthy -> colors.primaryContainer
        else -> colors.errorContainer
    }
    val onContainer = when {
        link is Link.Connecting -> colors.onSurface
        healthy -> colors.onPrimaryContainer
        else -> colors.onErrorContainer
    }
    val (title, detail, icon) = when {
        link is Link.Connecting -> Triple(R.string.home_connecting, null, null)
        link is Link.Missing -> Triple(R.string.home_inactive, R.string.home_inactive_detail, Icons.Rounded.Warning)
        !healthy -> Triple(R.string.home_no_scope, R.string.home_no_scope_detail, Icons.Rounded.Warning)
        else -> Triple(R.string.home_active, null, Icons.Rounded.CheckCircle)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = onContainer),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                    if (icon == null) LoadingIndicator(Modifier.size(64.dp))
                    else Icon(icon, contentDescription = null, modifier = Modifier.size(48.dp))
                }
                Column {
                    Text(stringResource(title), style = MaterialTheme.typography.headlineSmall)
                    if (detail != null) Text(stringResource(detail), style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (ready != null && !ready.scoped) {
                FilledTonalButton(onClick = actions.addScope) { Text(stringResource(R.string.action_add_scope)) }
            }
        }
    }
}

/** Every hook whose switch is on, grouped by the card it sits on. */
@Composable
private fun ActiveCard(store: Store) {
    val active = Features.groups.mapNotNull { (card, toggles) ->
        val on = toggles.filter { toggle ->
            store.bool(toggle.key) && Features.parents[toggle.key]?.let { store.bool(it) } != false
        }
        if (on.isEmpty()) null else card to on
    }
    val count = active.sumOf { it.second.size }
    SectionCard(
        if (count > 0) stringResource(R.string.card_running_count, count) else stringResource(R.string.card_running),
    ) {
        when {
            !store.writable -> RunningNote(R.string.running_unavailable)
            active.isEmpty() -> RunningNote(R.string.running_none)
            else -> active.forEachIndexed { index, (card, toggles) ->
                if (index > 0) CardDivider()
                Text(
                    stringResource(card),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 2.dp),
                )
                toggles.forEach { RunningRow(stringResource(it.title)) }
            }
        }
    }
}

@Composable
private fun RunningRow(title: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(28.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Bolt, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp),
                )
            }
        }
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun RunningNote(@StringRes text: Int) {
    Text(
        stringResource(text),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
    )
}

/**
 * The top bar's restart action: one button that restarts Security Center, SystemUI or the whole
 * system, chosen from a menu. It spins while a restart runs.
 */
@Composable
fun RestartAction(enabled: Boolean, restarting: Boolean, onRestart: (RestartTarget) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    var confirmSystem by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { menu = true }, enabled = enabled && !restarting) {
            if (restarting) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp)
            } else {
                Icon(Icons.Rounded.RestartAlt, contentDescription = stringResource(R.string.action_restart))
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            RestartItem(R.string.restart_security_center, Icons.Rounded.Security) {
                menu = false
                onRestart(RestartTarget.SecurityCenter)
            }
            RestartItem(R.string.restart_system_ui, Icons.Rounded.PhoneAndroid) {
                menu = false
                onRestart(RestartTarget.SystemUi)
            }
            RestartItem(R.string.restart_system, Icons.Rounded.Memory) {
                menu = false
                confirmSystem = true
            }
        }
    }
    if (confirmSystem) {
        AlertDialog(
            onDismissRequest = { confirmSystem = false },
            icon = { Icon(Icons.Rounded.PowerSettingsNew, contentDescription = null) },
            title = { Text(stringResource(R.string.restart_system_title)) },
            text = { Text(stringResource(R.string.restart_system_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmSystem = false
                    onRestart(RestartTarget.System)
                }) { Text(stringResource(R.string.action_restart)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmSystem = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun RestartItem(label: Int, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(label)) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        onClick = onClick,
    )
}
