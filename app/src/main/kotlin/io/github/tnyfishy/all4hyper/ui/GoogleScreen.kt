package io.github.tnyfishy.all4hyper.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.tnyfishy.all4hyper.CompanionState
import io.github.tnyfishy.all4hyper.Config
import io.github.tnyfishy.all4hyper.Link
import io.github.tnyfishy.all4hyper.R
import io.github.tnyfishy.all4hyper.Store

class GoogleActions(
    val install: () -> Unit,
    val recheck: () -> Unit,
    /** Turns a Google switch on or off, including any change to the system it needs. */
    val toggle: (key: String, enabled: Boolean) -> Unit,
    val requestScopes: (List<String>) -> Unit,
)

fun LazyListScope.googlePage(link: Link, store: Store, companion: CompanionState, root: RootState, actions: GoogleActions) {
    item { CompanionCard(companion, root, actions) }
    item {
        FeatureCard(R.string.card_power_key, link, store, actions, Features.googlePower)
    }
    item {
        FeatureCard(R.string.card_nav, link, store, actions, Features.googleNav, Features.googleVibrate)
    }
    item {
        FeatureCard(R.string.card_gms, link, store, actions, Features.googleGms)
    }
    item {
        FeatureCard(R.string.card_live, link, store, actions, Features.googleLive)
    }
    item { ScopesCard(link, store, actions) }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
private fun CompanionCard(state: CompanionState, root: RootState, actions: GoogleActions) {
    val busy = state == CompanionState.Checking || state == CompanionState.Installing
    val (title, detail) = when (state) {
        CompanionState.Unknown -> R.string.companion_unknown to R.string.companion_unknown_detail
        CompanionState.Checking -> R.string.companion_checking to R.string.companion_summary
        CompanionState.Installing -> R.string.companion_installing to R.string.companion_summary
        CompanionState.Missing -> R.string.companion_missing to R.string.companion_missing_detail
        CompanionState.Disabled -> R.string.companion_disabled to R.string.companion_disabled_detail
        CompanionState.Pending -> R.string.companion_pending to R.string.companion_pending_detail
        CompanionState.Ready -> R.string.companion_ready to R.string.companion_ready_detail
        CompanionState.Outdated -> R.string.companion_outdated to R.string.companion_outdated_detail
    }
    SectionCard(stringResource(R.string.card_companion)) {
        StatusRow(
            Icons.Rounded.Extension, stringResource(title), stringResource(detail),
            when (state) {
                CompanionState.Checking, CompanionState.Installing -> Health.Checking
                CompanionState.Ready, CompanionState.Pending -> Health.Ok
                else -> Health.Bad
            },
        )
        if (state == CompanionState.Installing) {
            LinearWavyProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp))
        }
        Text(
            stringResource(R.string.companion_summary),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        FlowRow(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val canInstall = root == RootState.Granted && (state == CompanionState.Missing || state == CompanionState.Outdated)
            if (state == CompanionState.Missing || state == CompanionState.Outdated || state == CompanionState.Installing) {
                Button(onClick = actions.install, enabled = canInstall) {
                    if (state == CompanionState.Installing) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    }
                    Text(
                        stringResource(if (state == CompanionState.Outdated) R.string.action_update else R.string.action_install),
                        modifier = Modifier.padding(start = if (state == CompanionState.Installing) 8.dp else 0.dp),
                    )
                }
            }
            OutlinedButton(onClick = actions.recheck, enabled = !busy) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.action_recheck), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

/** A switch (and an optional sub-switch that follows it) with the LSPosed scopes it works in. */
@Composable
private fun FeatureCard(
    @StringRes card: Int,
    link: Link,
    store: Store,
    actions: GoogleActions,
    main: Toggle,
    sub: Toggle? = null,
) {
    SectionCard(stringResource(card)) {
        val on = store.bool(main.key)
        ToggleRow(
            title = stringResource(main.title),
            summary = stringResource(main.summary),
            checked = on,
            enabled = store.writable,
            onChange = { actions.toggle(main.key, it) },
        )
        if (sub != null) {
            CardDivider()
            ToggleRow(
                title = stringResource(sub.title),
                summary = stringResource(sub.summary),
                checked = store.bool(sub.key),
                enabled = store.writable && on,
                onChange = { actions.toggle(sub.key, it) },
            )
        }
        ScopeChips(GoogleScopes.of(main.key), link)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScopeChips(required: List<String>, link: Link) {
    val scope = (link as? Link.Ready)?.scope
    FlowRow(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        required.forEach { name ->
            val present = scope?.contains(name)
            SuggestionChip(
                onClick = {},
                label = { Text(stringResource(GoogleScopes.label(name))) },
                icon = when (present) {
                    true -> {
                        { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) }
                    }
                    false -> {
                        { Icon(Icons.Rounded.Warning, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) }
                    }
                    null -> null
                },
            )
        }
    }
}

@Composable
private fun ScopesCard(link: Link, store: Store, actions: GoogleActions) {
    val ready = link as? Link.Ready
    // Only the features that are turned on need their scopes.
    val missing = ready?.let { r ->
        GoogleScopes.features.filter { store.bool(it) }.flatMap(GoogleScopes::of).distinct().filter { it !in r.scope }
    }.orEmpty()
    val requestable = missing.filter { it != Config.SYSTEM_SCOPE }
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.card_scopes), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.scopes_info), style = MaterialTheme.typography.bodyMedium)
            if (ready != null && missing.isEmpty()) {
                Text(
                    stringResource(R.string.scopes_complete),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (requestable.isNotEmpty()) {
                FilledTonalButton(onClick = { actions.requestScopes(requestable) }) {
                    Text(stringResource(R.string.action_request_scopes))
                }
            }
            Text(
                stringResource(R.string.g_overlap_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
