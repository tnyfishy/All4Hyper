package io.github.tnyfishy.all4hyper.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.tnyfishy.all4hyper.Link
import io.github.tnyfishy.all4hyper.R

enum class RootState { Checking, Granted, Denied }

/** What the restart button can restart. */
enum class RestartTarget { SecurityCenter, SystemUi, System }

class HomeActions(
    val restart: (RestartTarget) -> Unit,
    val addScope: () -> Unit,
)

fun LazyListScope.homePage(link: Link, root: RootState, restarting: Boolean, actions: HomeActions) {
    item { Hero(link, root, restarting, actions) }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
private fun Hero(link: Link, root: RootState, restarting: Boolean, actions: HomeActions) {
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
            if (restarting) LinearWavyProgressIndicator(Modifier.fillMaxWidth())
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RestartButton(enabled = root == RootState.Granted && !restarting, restarting = restarting, onRestart = actions.restart)
                if (ready != null && !ready.scoped) {
                    FilledTonalButton(onClick = actions.addScope) { Text(stringResource(R.string.action_add_scope)) }
                }
            }
        }
    }
}

/** One button that restarts Security Center, SystemUI or the whole system, chosen from a menu. */
@Composable
private fun RestartButton(enabled: Boolean, restarting: Boolean, onRestart: (RestartTarget) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    var confirmSystem by remember { mutableStateOf(false) }
    Box {
        Button(onClick = { menu = true }, enabled = enabled) {
            if (restarting) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Text(
                stringResource(if (restarting) R.string.action_restarting else R.string.action_restart),
                modifier = Modifier.padding(start = 8.dp),
            )
            if (!restarting) Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
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
