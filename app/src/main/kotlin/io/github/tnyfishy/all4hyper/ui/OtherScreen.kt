package io.github.tnyfishy.all4hyper.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.tnyfishy.all4hyper.Config
import io.github.tnyfishy.all4hyper.Link
import io.github.tnyfishy.all4hyper.R
import io.github.tnyfishy.all4hyper.Store

/** Hooks outside Security Center and Google: Power Keeper for now. */
fun LazyListScope.otherPage(store: Store, link: Link, requestScope: () -> Unit) {
    item { PowerKeeperCard(store, link, requestScope) }
}

@Composable
private fun PowerKeeperCard(store: Store, link: Link, requestScope: () -> Unit) {
    SectionCard(stringResource(R.string.card_power_keeper)) {
        val ready = link as? Link.Ready
        if (ready != null) {
            val scoped = Config.POWER_KEEPER in ready.scope && Config.MI_SETTINGS in ready.scope
            StatusRow(
                Icons.Rounded.BatteryChargingFull,
                stringResource(if (scoped) R.string.pk_scope_ok else R.string.pk_scope_missing),
                stringResource(R.string.pk_scope_detail),
                if (scoped) Health.Ok else Health.Bad,
            )
            if (!scoped) {
                FilledTonalButton(onClick = requestScope, modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)) {
                    Text(stringResource(R.string.action_add_scope))
                }
            }
            CardDivider()
        }
        Toggles(store, Features.powerKeeper)
        Text(
            stringResource(R.string.pk_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }
}
