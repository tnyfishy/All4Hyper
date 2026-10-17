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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.tnyfishy.all4hyper.Link
import io.github.tnyfishy.all4hyper.R
import io.github.tnyfishy.all4hyper.Store

enum class RootState { Checking, Granted, Denied }

class HomeActions(
    val restart: () -> Unit,
    val recheck: () -> Unit,
    val addScope: () -> Unit,
    val recommended: () -> Unit,
    val reset: () -> Unit,
)

fun LazyListScope.homePage(link: Link, store: Store, root: RootState, restarting: Boolean, actions: HomeActions) {
    item { Hero(link, root, restarting, actions) }
    item { QuickCard(store, actions) }
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
        else -> Triple(R.string.home_active, R.string.home_active_detail, Icons.Rounded.CheckCircle)
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
                Button(onClick = actions.restart, enabled = root == RootState.Granted && !restarting) {
                    if (restarting) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = colors.onPrimary)
                    } else {
                        Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        stringResource(if (restarting) R.string.action_restarting else R.string.action_restart),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                if (ready != null && !ready.scoped) {
                    FilledTonalButton(onClick = actions.addScope) { Text(stringResource(R.string.action_add_scope)) }
                }
                OutlinedButton(onClick = actions.recheck) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.action_recheck), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickCard(store: Store, actions: HomeActions) {
    SectionCard(stringResource(R.string.quick_title)) {
        FlowRow(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val on = Features.all.count { store.bool(it.key) }
            SuggestionChip(onClick = {}, label = { Text(stringResource(R.string.quick_features, on)) })
            AssistChip(
                onClick = actions.recommended,
                enabled = store.writable,
                label = { Text(stringResource(R.string.action_recommended)) },
                leadingIcon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp)) },
            )
            AssistChip(
                onClick = actions.reset,
                enabled = store.writable,
                label = { Text(stringResource(R.string.action_reset)) },
                leadingIcon = { Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp)) },
            )
        }
    }
}
