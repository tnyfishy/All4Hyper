package io.github.tnyfishy.all4hyper.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.tnyfishy.all4hyper.Store

/** A scrolling page whose last item can scroll clear of the floating navigation bar. */
@Composable
fun Page(content: LazyListScope.() -> Unit) {
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + FloatingBarHeight
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottom),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

/** The toggles of a [list] as rows, with a divider between them. */
@Composable
fun Toggles(store: Store, list: List<Toggle>) {
    list.forEachIndexed { index, toggle ->
        if (index > 0) CardDivider()
        ToggleRow(
            title = stringResource(toggle.title),
            summary = stringResource(toggle.summary),
            checked = store.bool(toggle.key),
            enabled = store.writable,
            onChange = { store.put(toggle.key, it) },
        )
    }
}
