package io.github.tnyfishy.all4hyper.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp

private val ExpandedHeight = 136.dp
private val CollapsedHeight = 64.dp

/** The collapsed title is this much of the expanded one (headlineMedium 30sp → about 22sp). */
private const val CollapsedScale = 0.72f

/**
 * A large top bar whose one title shrinks smoothly with the scroll and settles at the collapsed
 * size, instead of the standard bar's fade from a large title to a small one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShrinkingTopBar(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val limit = with(LocalDensity.current) { (CollapsedHeight - ExpandedHeight).toPx() }
    SideEffect {
        if (scrollBehavior.state.heightOffsetLimit != limit) scrollBehavior.state.heightOffsetLimit = limit
    }
    val fraction = scrollBehavior.state.collapsedFraction
    val colors = MaterialTheme.colorScheme
    Surface(color = lerp(colors.background, colors.surfaceContainer, fraction)) {
        Box(
            Modifier
                .windowInsetsPadding(TopAppBarDefaults.windowInsets)
                .fillMaxWidth()
                .height(lerp(ExpandedHeight, CollapsedHeight, fraction)),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, end = 64.dp, bottom = lerp(20.dp, 18.dp, fraction))
                    .graphicsLayer {
                        val scale = 1f - (1f - CollapsedScale) * fraction
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0f, 1f)
                    },
            )
            Row(
                Modifier.align(Alignment.TopEnd).height(CollapsedHeight).padding(end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
    }
}
