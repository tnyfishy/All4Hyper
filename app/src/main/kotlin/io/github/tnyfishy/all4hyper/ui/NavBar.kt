package io.github.tnyfishy.all4hyper.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

class NavItem(val label: String, val icon: ImageVector)

/** Space the pages leave under their content so the last row can scroll clear of the floating bar. */
val FloatingBarHeight = 96.dp

/**
 * A floating pill that follows the pager: its highlight is placed from the pager's live position
 * (page plus drag offset), so it glides along with a swipe instead of jumping when the page
 * settles, and each tab blends between its resting and active colors as the highlight passes.
 */
@Composable
fun FloatingNavBar(
    items: List<NavItem>,
    pager: PagerState,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var widthPx by remember { mutableIntStateOf(0) }
    val inset = 6.dp
    val barHeight = 68.dp
    val tabWidthPx = if (widthPx > 0) (widthPx - with(density) { inset.toPx() * 2 }) / items.size else 0f
    val active = MaterialTheme.colorScheme.onSecondaryContainer
    val resting = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 3.dp,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .onSizeChanged { widthPx = it.width },
        ) {
            Box(Modifier.padding(horizontal = inset, vertical = inset)) {
                if (tabWidthPx > 0f) {
                    Box(
                        Modifier
                            .offset {
                                val position = pager.currentPage + pager.currentPageOffsetFraction
                                IntOffset((position * tabWidthPx).roundToInt(), 0)
                            }
                            .width(with(density) { tabWidthPx.toDp() })
                            .height(barHeight - inset * 2)
                            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                    )
                }
                Row(Modifier.fillMaxWidth().height(barHeight - inset * 2)) {
                    items.forEachIndexed { index, item ->
                        val position = pager.currentPage + pager.currentPageOffsetFraction
                        val tint = lerp(resting, active, (1f - abs(position - index)).coerceIn(0f, 1f))
                        Tab(item, tint, selected = pager.currentPage == index, onClick = { onSelect(index) })
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.Tab(item: NavItem, tint: Color, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(item.icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Text(item.label, style = MaterialTheme.typography.labelMedium, color = tint, maxLines = 1)
    }
}
