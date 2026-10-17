package io.github.tnyfishy.all4hyper.ui

import android.content.Intent
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.tnyfishy.all4hyper.CompanionState
import io.github.tnyfishy.all4hyper.R
import io.github.tnyfishy.all4hyper.Store

class GoogleActions(
    val install: () -> Unit,
    /** Turns a Google switch on or off, including any change to the system it needs. */
    val toggle: (key: String, enabled: Boolean) -> Unit,
)

fun LazyListScope.googlePage(store: Store, companion: CompanionState, root: RootState, actions: GoogleActions) {
    item { CompanionCard(companion, root, actions) }
    item {
        FeatureCard(R.string.card_power_key, store, actions, Features.googlePower) {
            AssistantShortcut()
        }
    }
    item {
        FeatureCard(R.string.card_nav, store, actions, Features.googleNav, Features.googleVibrate)
    }
    item {
        FeatureCard(R.string.card_gms, store, actions, Features.googleGms)
    }
    item {
        FeatureCard(R.string.card_live, store, actions, Features.googleLive)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CompanionCard(state: CompanionState, root: RootState, actions: GoogleActions) {
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
        if (state == CompanionState.Missing || state == CompanionState.Outdated || state == CompanionState.Installing) {
            val canInstall = root == RootState.Granted && state != CompanionState.Installing
            Button(
                onClick = actions.install,
                enabled = canInstall,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                if (state == CompanionState.Installing) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                }
                Text(
                    stringResource(if (state == CompanionState.Outdated) R.string.action_update else R.string.action_install),
                    modifier = Modifier.padding(start = if (state == CompanionState.Installing) 8.dp else 0.dp),
                )
            }
        }
    }
}

/** A switch, and an optional sub-switch that follows it. */
@Composable
private fun FeatureCard(
    @StringRes card: Int,
    store: Store,
    actions: GoogleActions,
    main: Toggle,
    sub: Toggle? = null,
    extra: @Composable () -> Unit = {},
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
        extra()
    }
}

/** Opens the system page where Google is picked as the default digital assistant. */
@Composable
private fun AssistantShortcut() {
    val context = LocalContext.current
    TextButton(
        onClick = {
            val open = runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
            if (open.isFailure) runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        },
        modifier = Modifier.padding(start = 12.dp),
    ) {
        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(stringResource(R.string.g_set_assistant), modifier = Modifier.padding(start = 8.dp))
    }
}
