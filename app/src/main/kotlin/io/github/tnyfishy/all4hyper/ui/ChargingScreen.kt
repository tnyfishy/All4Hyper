package io.github.tnyfishy.all4hyper.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.tnyfishy.all4hyper.Config
import io.github.tnyfishy.all4hyper.R
import io.github.tnyfishy.all4hyper.Store

fun LazyListScope.chargingPage(store: Store, onSaved: () -> Unit) {
    item {
        SectionCard(stringResource(R.string.card_night)) { Toggles(store, Features.nightCharge) }
    }
    item {
        SectionCard(stringResource(R.string.card_limit)) {
            Toggles(store, Features.chargeLimit)
            CardDivider()
            ToggleRow(
                title = stringResource(R.string.t_override_limit),
                summary = stringResource(R.string.t_override_limit_s),
                checked = store.bool(Config.OVERRIDE_LIMIT),
                enabled = store.writable,
                onChange = { store.put(Config.OVERRIDE_LIMIT, it) },
            )
            val limit = store.int(Config.LIMIT_PERCENT, Config.LIMIT_UNLIMITED)
            SliderRow(
                title = stringResource(R.string.limit_title),
                valueLabel = if (limit >= Config.LIMIT_UNLIMITED) stringResource(R.string.limit_unlimited)
                else stringResource(R.string.limit_value, limit),
                value = limit,
                range = 50..Config.LIMIT_UNLIMITED,
                enabled = store.writable && store.bool(Config.OVERRIDE_LIMIT),
                onChange = { store.put(Config.LIMIT_PERCENT, it) },
            )
        }
    }
    item {
        SectionCard(stringResource(R.string.card_fast)) {
            ChoiceRow(
                title = stringResource(R.string.fast_title),
                summary = stringResource(R.string.fast_summary),
                options = listOf(R.string.fast_stock, R.string.fast_on, R.string.fast_off).map { stringResource(it) },
                selected = store.int(Config.FAST_CHARGE_MODE, 0),
                enabled = store.writable,
                onSelect = { store.put(Config.FAST_CHARGE_MODE, it) },
            )
            CardDivider()
            ChoiceRow(
                title = stringResource(R.string.bypass_title),
                summary = stringResource(R.string.bypass_summary),
                options = listOf(R.string.bypass_stock, R.string.bypass_block_start, R.string.bypass_block_stop)
                    .map { stringResource(it) },
                selected = store.int(Config.BYPASS_MODE, 0),
                enabled = store.writable,
                onSelect = { store.put(Config.BYPASS_MODE, it) },
            )
            CardDivider()
            Toggles(store, Features.chargeExtras)
        }
    }
    item { HealthCard(store, onSaved) }
}

@Composable
private fun HealthCard(store: Store, onSaved: () -> Unit) {
    // Edited locally and only written on Save; refreshed when the connection (and so the store) changes.
    var text by remember(store) { mutableStateOf(store.text(Config.UI_HEALTH_TEXT)) }
    LaunchedEffect(store) { text = store.text(Config.UI_HEALTH_TEXT) }

    SectionCard(stringResource(R.string.card_health)) {
        ToggleRow(
            title = stringResource(R.string.t_fake_health),
            summary = stringResource(R.string.t_fake_health_s),
            checked = store.bool(Config.FAKE_HEALTH),
            enabled = store.writable,
            onChange = { store.put(Config.FAKE_HEALTH, it) },
        )
        CardDivider()
        ChoiceRow(
            title = stringResource(R.string.health_level_title),
            summary = stringResource(R.string.health_level_summary),
            options = (1..4).map { it.toString() },
            selected = store.int(Config.HEALTH_LEVEL, 4) - 1,
            enabled = store.writable && store.bool(Config.FAKE_HEALTH),
            onSelect = { store.put(Config.HEALTH_LEVEL, it + 1) },
        )
        CardDivider()
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.health_text_title), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.health_text_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(12) },
                enabled = store.writable,
                singleLine = true,
                placeholder = { Text(stringResource(R.string.health_text_hint)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(
                    enabled = store.writable && text != store.text(Config.UI_HEALTH_TEXT),
                    onClick = { store.put(Config.UI_HEALTH_TEXT, text.trim()); onSaved() },
                ) { Text(stringResource(R.string.action_save)) }
                TextButton(
                    enabled = store.writable && (text.isNotEmpty() || store.text(Config.UI_HEALTH_TEXT).isNotEmpty()),
                    onClick = { text = ""; store.put(Config.UI_HEALTH_TEXT, ""); onSaved() },
                ) { Text(stringResource(R.string.action_clear)) }
            }
        }
    }
}
