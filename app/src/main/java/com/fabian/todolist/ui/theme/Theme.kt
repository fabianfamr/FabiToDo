package com.fabian.todolist.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Midnight Slate Scheme (Premium Turquoise Accents)
private val BlueLightColors = lightColorScheme(
    primary = Color(0xFF0061A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF535F70),
    onSecondary = Color.White,
    background = Color(0xFFF8F9FF), // Slightly blueish background
    surface = Color.White,
    onBackground = Color(0xFF111C2B),
    onSurface = Color(0xFF111C2B)
)
private val BlueDarkColors = darkColorScheme(
    primary = Color(0xFFD1E4FF), 
    onPrimary = Color(0xFF00315E),
    primaryContainer = Color(0xFF004494),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFFBFC6DC),
    onSecondary = Color(0xFF293041),
    background = Color(0xFF000000), // Pure black background
    surface = Color(0xFF0A121D),
    onBackground = Color(0xFFE3E2E6),
    onSurface = Color(0xFFE3E2E6)
)

// Emerald Mint Scheme
private val GreenLightColors = lightColorScheme(
    primary = Color(0xFF0F9D58),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6F4EA),
    onPrimaryContainer = Color(0xFF137333),
    secondary = Color(0xFF137333),
    onSecondary = Color.White,
    background = Color(0xFFF5FAF6),
    surface = Color.White,
    onBackground = Color(0xFF1A1C1E),
    onSurface = Color(0xFF1A1C1E)
)
private val GreenDarkColors = darkColorScheme(
    primary = Color(0xFF81C995),
    onPrimary = Color(0xFF137333),
    primaryContainer = Color(0xFF137333),
    onPrimaryContainer = Color(0xFFE6F4EA),
    secondary = Color(0xFF81C995),
    onSecondary = Color(0xFF137333),
    background = Color(0xFF0D2016),
    surface = Color(0xFF1B3527),
    onBackground = Color(0xFFF1F8F4),
    onSurface = Color(0xFFF1F8F4)
)

// Sunset Coral Scheme
private val CoralLightColors = lightColorScheme(
    primary = Color(0xFFEA4335),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFCE8E6),
    onPrimaryContainer = Color(0xFFC5221F),
    secondary = Color(0xFFC5221F),
    onSecondary = Color.White,
    background = Color(0xFFFDF6F5),
    surface = Color.White,
    onBackground = Color(0xFF1A1C1E),
    onSurface = Color(0xFF1A1C1E)
)
private val CoralDarkColors = darkColorScheme(
    primary = Color(0xFFF28B82),
    onPrimary = Color(0xFFC5221F),
    primaryContainer = Color(0xFFC5221F),
    onPrimaryContainer = Color(0xFFFCE8E6),
    secondary = Color(0xFFF28B82),
    onSecondary = Color(0xFFC5221F),
    background = Color(0xFF28110E),
    surface = Color(0xFF3B1E1B),
    onBackground = Color(0xFFFFECEB),
    onSurface = Color(0xFFFFECEB)
)

// Orchid Purple Scheme
private val PurpleLightColors = lightColorScheme(
    primary = Color(0xFF8B5CF6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF6D28D9),
    secondary = Color(0xFF6D28D9),
    onSecondary = Color.White,
    background = Color(0xFFFAF5FF),
    surface = Color.White,
    onBackground = Color(0xFF1A1C1E),
    onSurface = Color(0xFF1A1C1E)
)
private val PurpleDarkColors = darkColorScheme(
    primary = Color(0xFFC4B5FD),
    onPrimary = Color(0xFF6D28D9),
    primaryContainer = Color(0xFF6D28D9),
    onPrimaryContainer = Color(0xFFEDE9FE),
    secondary = Color(0xFFC4B5FD),
    onSecondary = Color.White,
    background = Color(0xFF1A112B),
    surface = Color(0xFF2B1D3F),
    onBackground = Color(0xFFF5F1FA),
    onSurface = Color(0xFFF5F1FA)
)

// Pixel Gold Scheme
private val GoldLightColors = lightColorScheme(
    primary = Color(0xFFE29500),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFEF7E0),
    onPrimaryContainer = Color(0xFFB06000),
    secondary = Color(0xFFB06000),
    onSecondary = Color.White,
    background = Color(0xFFFFFDF5),
    surface = Color.White,
    onBackground = Color(0xFF1A1C1E),
    onSurface = Color(0xFF1A1C1E)
)
private val GoldDarkColors = darkColorScheme(
    primary = Color(0xFFFDD663),
    onPrimary = Color(0xFFB06000),
    primaryContainer = Color(0xFFB06000),
    onPrimaryContainer = Color(0xFFFEF7E0),
    secondary = Color(0xFFFDD663),
    onSecondary = Color(0xFFB06000),
    background = Color(0xFF231A02),
    surface = Color(0xFF382C05),
    onBackground = Color(0xFFFFF3DE),
    onSurface = Color(0xFFFFF3DE)
)

// Lavender Pink Scheme
private val PinkLightColors = lightColorScheme(
    primary = Color(0xFFE91E63),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFCE4EC),
    onPrimaryContainer = Color(0xFFC2185B),
    secondary = Color(0xFFC2185B),
    onSecondary = Color.White,
    background = Color(0xFFFFF2F6),
    surface = Color.White,
    onBackground = Color(0xFF1A1C1E),
    onSurface = Color(0xFF1A1C1E)
)
private val PinkDarkColors = darkColorScheme(
    primary = Color(0xFFF48FB1),
    onPrimary = Color(0xFFC2185B),
    primaryContainer = Color(0xFFC2185B),
    onPrimaryContainer = Color(0xFFFCE4EC),
    secondary = Color(0xFFF48FB1),
    onSecondary = Color(0xFFC2185B),
    background = Color(0xFF2B0E1E),
    surface = Color(0xFF3F192C),
    onBackground = Color(0xFFFFF0F5),
    onSurface = Color(0xFFFFF0F5)
)

@Composable
fun FabiToDoTheme(
    selectedColor: String = "system", // system, blue, green, coral, purple, gold, pink
    selectedDarkTheme: String = "system", // system, light, dark
    content: @Composable () -> Unit,
) {
    val isSystemDark = isSystemInDarkTheme()
    val useDark = when (selectedDarkTheme) {
        "light" -> false
        "dark" -> true
        else -> isSystemDark
    }

    val context = LocalContext.current
    val colorScheme = when {
        // Material You dynamic system colors (on Android 12+)
        selectedColor == "system" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // Fallbacks and palette choices
        selectedColor == "blue" -> if (useDark) BlueDarkColors else BlueLightColors
        selectedColor == "green" -> if (useDark) GreenDarkColors else GreenLightColors
        selectedColor == "coral" -> if (useDark) CoralDarkColors else CoralLightColors
        selectedColor == "purple" -> if (useDark) PurpleDarkColors else PurpleLightColors
        selectedColor == "gold" -> if (useDark) GoldDarkColors else GoldLightColors
        selectedColor == "pink" -> if (useDark) PinkDarkColors else PinkLightColors
        // Default theme is Pixel Blue (if system dynamic color is unsupported or default is preferred)
        else -> if (useDark) BlueDarkColors else BlueLightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
