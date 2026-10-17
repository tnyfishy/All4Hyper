package io.github.tnyfishy.all4hyper.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow

/** A destination: [icon] while it is not selected, [selectedIcon] (usually the filled one) while it is. */
class NavItem(val label: String, val icon: ImageVector, val selectedIcon: ImageVector = icon)

/** The standard Material 3 navigation bar. */
@Composable
fun AppNavigationBar(items: List<NavItem>, selected: Int, onSelect: (Int) -> Unit) {
    NavigationBar {
        items.forEachIndexed { index, item ->
            val on = index == selected
            NavigationBarItem(
                selected = on,
                onClick = { onSelect(index) },
                icon = { Icon(if (on) item.selectedIcon else item.icon, contentDescription = null) },
                label = { Text(item.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            )
        }
    }
}
