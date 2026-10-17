package io.github.tnyfishy.all4hyper.ui

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import io.github.tnyfishy.all4hyper.Appearance

/** Blue-seeded fallback palette for devices or choices without dynamic color. */
private val LightScheme = lightColorScheme(
    primary = Color(0xFF3F5BA9), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD9E2FF), onPrimaryContainer = Color(0xFF001945),
    secondary = Color(0xFF575E71), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDBE2F9), onSecondaryContainer = Color(0xFF141B2C),
    tertiary = Color(0xFF725572), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFBD7FC), onTertiaryContainer = Color(0xFF2A122C),
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFAF8FF), onBackground = Color(0xFF1A1B20),
    surface = Color(0xFFFAF8FF), onSurface = Color(0xFF1A1B20),
    surfaceVariant = Color(0xFFE1E2EC), onSurfaceVariant = Color(0xFF44464F),
    outline = Color(0xFF757780), outlineVariant = Color(0xFFC5C6D0),
    inverseSurface = Color(0xFF2F3036), inverseOnSurface = Color(0xFFF1F0F7), inversePrimary = Color(0xFFB0C6FF),
    surfaceDim = Color(0xFFDAD9E0), surfaceBright = Color(0xFFFAF8FF),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF4F3FA),
    surfaceContainer = Color(0xFFEEEDF4), surfaceContainerHigh = Color(0xFFE8E7EF),
    surfaceContainerHighest = Color(0xFFE3E2E9),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFB0C6FF), onPrimary = Color(0xFF002D6F),
    primaryContainer = Color(0xFF254287), onPrimaryContainer = Color(0xFFD9E2FF),
    secondary = Color(0xFFBFC6DC), onSecondary = Color(0xFF293041),
    secondaryContainer = Color(0xFF3F4759), onSecondaryContainer = Color(0xFFDBE2F9),
    tertiary = Color(0xFFDEBCDF), onTertiary = Color(0xFF402843),
    tertiaryContainer = Color(0xFF583E5B), onTertiaryContainer = Color(0xFFFBD7FC),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF121318), onBackground = Color(0xFFE3E2E9),
    surface = Color(0xFF121318), onSurface = Color(0xFFE3E2E9),
    surfaceVariant = Color(0xFF44464F), onSurfaceVariant = Color(0xFFC5C6D0),
    outline = Color(0xFF8F909A), outlineVariant = Color(0xFF44464F),
    inverseSurface = Color(0xFFE3E2E9), inverseOnSurface = Color(0xFF2F3036), inversePrimary = Color(0xFF3F5BA9),
    surfaceDim = Color(0xFF121318), surfaceBright = Color(0xFF38393F),
    surfaceContainerLowest = Color(0xFF0D0E13), surfaceContainerLow = Color(0xFF1A1B20),
    surfaceContainer = Color(0xFF1E1F25), surfaceContainerHigh = Color(0xFF292A2F),
    surfaceContainerHighest = Color(0xFF34343A),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun All4HyperTheme(appearance: Appearance, content: @Composable () -> Unit) {
    val dark = when (appearance.mode) {
        Appearance.Mode.System -> isSystemInDarkTheme()
        Appearance.Mode.Light -> false
        Appearance.Mode.Dark -> true
    }
    val context = LocalContext.current
    val colors = when {
        appearance.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkScheme
        else -> LightScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialExpressiveTheme(
        colorScheme = colors,
        motionScheme = MotionScheme.expressive(),
        shapes = AppShapes,
        content = content,
    )
}
