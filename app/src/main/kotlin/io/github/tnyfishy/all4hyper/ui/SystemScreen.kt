package io.github.tnyfishy.all4hyper.ui

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.res.stringResource
import io.github.tnyfishy.all4hyper.R
import io.github.tnyfishy.all4hyper.Store

fun LazyListScope.systemPage(store: Store) {
    item { SectionCard(stringResource(R.string.card_power)) { Toggles(store, Features.power) } }
    item { SectionCard(stringResource(R.string.card_perm)) { Toggles(store, Features.permissions) } }
    item { SectionCard(stringResource(R.string.card_net)) { Toggles(store, Features.network) } }
}
