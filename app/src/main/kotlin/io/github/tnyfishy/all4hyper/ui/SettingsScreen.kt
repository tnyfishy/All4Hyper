package io.github.tnyfishy.all4hyper.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.tnyfishy.all4hyper.Appearance
import io.github.tnyfishy.all4hyper.Config
import io.github.tnyfishy.all4hyper.Language
import io.github.tnyfishy.all4hyper.R
import io.github.tnyfishy.all4hyper.Store

private const val MEOWPOWER_URL = "https://github.com/Xposed-Modules-Repo/io.github.yuhj319.meowpower"

fun LazyListScope.settingsPage(store: Store, appearance: Appearance) {
    item {
        SectionCard(stringResource(R.string.card_hooks)) {
            ToggleRow(
                title = stringResource(R.string.t_debug),
                summary = stringResource(R.string.t_debug_s),
                checked = store.bool(Config.DEBUG_LOG),
                enabled = store.writable,
                onChange = { store.put(Config.DEBUG_LOG, it) },
            )
        }
    }
    item {
        SectionCard(stringResource(R.string.card_appearance)) {
            val context = LocalContext.current
            ChoiceRow(
                title = stringResource(R.string.lang_title),
                summary = null,
                options = listOf(R.string.lang_vi, R.string.lang_en).map { stringResource(it) },
                selected = Language.supported.indexOf(Language.current(LocalConfiguration.current)),
                enabled = true,
                onSelect = { Language.set(context, Language.supported[it]) },
            )
            CardDivider()
            ChoiceRow(
                title = stringResource(R.string.mode_title),
                summary = null,
                options = listOf(R.string.mode_system, R.string.mode_light, R.string.mode_dark, R.string.mode_amoled).map { stringResource(it) },
                selected = appearance.mode.ordinal,
                enabled = true,
                onSelect = { appearance.mode = Appearance.Mode.entries[it] },
            )
            CardDivider()
            ToggleRow(
                title = stringResource(R.string.t_dynamic),
                summary = stringResource(R.string.t_dynamic_s),
                checked = appearance.dynamicColor,
                enabled = true,
                onChange = { appearance.dynamicColor = it },
            )
        }
    }
    item { AboutCard() }
}

@Composable
private fun AboutCard() {
    val context = LocalContext.current
    val version = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }
    SectionCard(stringResource(R.string.card_about)) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.about_version, version), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.about_credit),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(MEOWPOWER_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }) {
                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.about_open), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}
